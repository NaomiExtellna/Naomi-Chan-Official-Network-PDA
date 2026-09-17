package com.example.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class StaffRole {
    ADMIN,
    STAFF
}

data class StaffAccount(
    val id: String,
    val username: String,
    val displayName: String,
    val role: StaffRole,
    val isActive: Boolean,
    val createdAt: Long,
    val lastLoginAt: Long?,
    val failedAttempts: Int = 0,
    val lockedUntil: Long? = null,
    val mustChangeCredential: Boolean = false,
    val canVoid: Boolean = false,
    val canExport: Boolean = false,
    val canEditVenues: Boolean = false,
    val canChangeGateway: Boolean = false,
    val canViewTotals: Boolean = false
) {
    val isAdmin: Boolean get() = role == StaffRole.ADMIN
    val canVoidReceipts: Boolean get() = isAdmin || canVoid
    val canExportData: Boolean get() = isAdmin || canExport
    val canManageVenues: Boolean get() = isAdmin || canEditVenues
    val canConfigureGateway: Boolean get() = isAdmin || canChangeGateway
    val canViewFinancialTotals: Boolean get() = isAdmin || canViewTotals
    val isLocked: Boolean get() = (lockedUntil ?: 0L) > System.currentTimeMillis()
}

data class StaffShift(
    val id: String,
    val staffId: String,
    val staffDisplayName: String,
    val openedAt: Long,
    val closedAt: Long?,
    val openingNote: String,
    val closingNote: String
) {
    val isOpen: Boolean get() = closedAt == null
}

sealed class AuthResult {
    data class Success(
        val account: StaffAccount,
        val generatedCode: String? = null
    ) : AuthResult()

    data class Error(val message: String) : AuthResult()
}

class StaffRepository(private val staffDao: StaffDao) {

    companion object {
        const val RESERVED_ADMIN_USERNAME = "naomi"
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_MS = 5 * 60 * 1000L
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }

    val allStaff: Flow<List<StaffAccountEntity>> = staffDao.observeAllStaff()
    val allShifts: Flow<List<StaffShiftEntity>> = staffDao.observeAllShifts()

    suspend fun hasAnyStaff(): Boolean = withContext(Dispatchers.IO) {
        staffDao.countStaff() > 0
    }

    fun credentialValidationError(credential: String): String? {
        return when {
            credential.all { it.isDigit() } && credential.length < 6 -> "PIN must be at least 6 digits."
            credential.all { it.isDigit() } && credential.length > 12 -> "PIN must be 6 to 12 digits."
            !credential.all { it.isDigit() } && credential.length < 8 -> "Password must be at least 8 characters."
            credential.length > 128 -> "Credential is too long."
            else -> null
        }
    }

    suspend fun createNaomiAdmin(credential: String): AuthResult = withContext(Dispatchers.IO) {
        if (staffDao.countStaff() > 0) {
            return@withContext AuthResult.Error("Administrator setup has already been completed.")
        }
        credentialValidationError(credential)?.let { return@withContext AuthResult.Error(it) }

        val credentialSalt = newSalt()
        val recoveryCode = generateRecoveryCode()
        val recoverySalt = newSalt()
        val account = StaffAccountEntity(
            id = UUID.randomUUID().toString(),
            username = RESERVED_ADMIN_USERNAME,
            displayName = "Naomi",
            credentialHash = hashSecret(credential, credentialSalt),
            salt = encode(credentialSalt),
            role = StaffRole.ADMIN.name,
            isActive = true,
            recoveryHash = hashSecret(normalizeRecoveryCode(recoveryCode), recoverySalt),
            recoverySalt = encode(recoverySalt),
            canVoid = true,
            canExport = true,
            canEditVenues = true,
            canChangeGateway = true,
            canViewTotals = true
        )
        staffDao.insertStaff(account)
        AuthResult.Success(account.toModel(), generatedCode = recoveryCode)
    }

    suspend fun registerStaff(username: String, displayName: String, credential: String): AuthResult = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim().lowercase(Locale.ROOT)
        val cleanDisplayName = displayName.trim()

        when {
            normalizedUsername.length < 3 -> return@withContext AuthResult.Error("Username must be at least 3 characters.")
            !normalizedUsername.matches(Regex("[a-z0-9._-]+")) -> return@withContext AuthResult.Error("Username may only use letters, numbers, dot, dash and underscore.")
            normalizedUsername == RESERVED_ADMIN_USERNAME || normalizedUsername == "admin" -> return@withContext AuthResult.Error("That username is reserved for the administrator.")
            cleanDisplayName.length < 2 -> return@withContext AuthResult.Error("Enter the staff member's display name.")
            staffDao.findByUsername(normalizedUsername) != null -> return@withContext AuthResult.Error("That username is already registered.")
        }
        credentialValidationError(credential)?.let { return@withContext AuthResult.Error(it) }

        val salt = newSalt()
        val account = StaffAccountEntity(
            id = UUID.randomUUID().toString(),
            username = normalizedUsername,
            displayName = cleanDisplayName,
            credentialHash = hashSecret(credential, salt),
            salt = encode(salt),
            role = StaffRole.STAFF.name,
            isActive = false
        )
        staffDao.insertStaff(account)
        AuthResult.Success(account.toModel())
    }

    suspend fun authenticate(username: String, credential: String): AuthResult = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim().lowercase(Locale.ROOT)
        val account = staffDao.findByUsername(normalizedUsername)
            ?: return@withContext AuthResult.Error("Incorrect username or PIN/password.")
        val now = System.currentTimeMillis()

        if (!account.isActive) {
            return@withContext AuthResult.Error("This staff account is pending or disabled. Ask Naomi (Admin) to enable it.")
        }
        val lockedUntil = account.lockedUntil
        if (lockedUntil != null && lockedUntil > now) {
            val seconds = ((lockedUntil - now) / 1000L).coerceAtLeast(1L)
            return@withContext AuthResult.Error("Account temporarily locked. Try again in ${seconds / 60 + 1} minute(s), or ask Naomi to unlock it.")
        }
        if (lockedUntil != null) staffDao.clearLoginLock(account.id)

        val salt = decode(account.salt) ?: return@withContext AuthResult.Error("Account credentials are invalid. Contact the administrator.")
        val candidate = hashSecret(credential, salt)
        val matches = MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            account.credentialHash.toByteArray(Charsets.UTF_8)
        )
        if (!matches) {
            val attempts = (account.failedAttempts + 1).coerceAtMost(MAX_FAILED_ATTEMPTS)
            val newLock = if (attempts >= MAX_FAILED_ATTEMPTS) now + LOCKOUT_MS else null
            staffDao.updateLoginFailure(account.id, attempts, newLock)
            return@withContext if (newLock != null) {
                AuthResult.Error("Too many incorrect attempts. Account locked for 5 minutes.")
            } else {
                AuthResult.Error("Incorrect username or PIN/password. ${MAX_FAILED_ATTEMPTS - attempts} attempt(s) remaining before lockout.")
            }
        }

        staffDao.markLoginSuccess(account.id, now)

        // Avoid a second SELECT after a successful credential check. We know exactly
        // which fields markLoginSuccess changed, so return the refreshed model directly.
        val authenticated = account.copy(
            lastLoginAt = now,
            failedAttempts = 0,
            lockedUntil = null
        )
        AuthResult.Success(authenticated.toModel())
    }

    suspend fun recoverNaomiAdmin(recoveryCode: String, newCredential: String): AuthResult = withContext(Dispatchers.IO) {
        credentialValidationError(newCredential)?.let { return@withContext AuthResult.Error(it) }
        val account = staffDao.findByUsername(RESERVED_ADMIN_USERNAME)
            ?: return@withContext AuthResult.Error("Naomi administrator account was not found.")
        val storedHash = account.recoveryHash
            ?: return@withContext AuthResult.Error("No recovery code exists yet. Sign in as Naomi and generate one from Ops → Staff.")
        val recoverySalt = account.recoverySalt?.let(::decode)
            ?: return@withContext AuthResult.Error("Recovery configuration is invalid.")
        val candidate = hashSecret(normalizeRecoveryCode(recoveryCode), recoverySalt)
        if (!MessageDigest.isEqual(candidate.toByteArray(Charsets.UTF_8), storedHash.toByteArray(Charsets.UTF_8))) {
            return@withContext AuthResult.Error("Recovery code is incorrect.")
        }

        val newCredentialSalt = newSalt()
        staffDao.updateCredential(account.id, hashSecret(newCredential, newCredentialSalt), encode(newCredentialSalt), mustChange = false)
        val replacementRecovery = generateRecoveryCode()
        val replacementSalt = newSalt()
        staffDao.updateRecovery(account.id, hashSecret(normalizeRecoveryCode(replacementRecovery), replacementSalt), encode(replacementSalt))
        staffDao.clearLoginLock(account.id)
        val refreshed = staffDao.findById(account.id) ?: account
        AuthResult.Success(refreshed.toModel(), generatedCode = replacementRecovery)
    }

    suspend fun rotateAdminRecovery(requester: StaffAccount): AuthResult = withContext(Dispatchers.IO) {
        if (!requester.isAdmin) return@withContext AuthResult.Error("Only Naomi (Admin) can rotate the recovery code.")
        val account = staffDao.findById(requester.id) ?: return@withContext AuthResult.Error("Administrator account not found.")
        val code = generateRecoveryCode()
        val salt = newSalt()
        staffDao.updateRecovery(account.id, hashSecret(normalizeRecoveryCode(code), salt), encode(salt))
        AuthResult.Success(account.toModel(), generatedCode = code)
    }

    suspend fun resetStaffCredential(requester: StaffAccount, staffId: String): AuthResult = withContext(Dispatchers.IO) {
        if (!requester.isAdmin) return@withContext AuthResult.Error("Only Naomi (Admin) can reset staff credentials.")
        if (requester.id == staffId) return@withContext AuthResult.Error("Use Change My PIN/Password for the signed-in administrator.")
        val target = staffDao.findById(staffId) ?: return@withContext AuthResult.Error("Staff account not found.")
        if (target.role == StaffRole.ADMIN.name) return@withContext AuthResult.Error("Administrator recovery must use the recovery-code flow.")
        val temporaryPin = (100000 + SecureRandom().nextInt(900000)).toString()
        val salt = newSalt()
        staffDao.updateCredential(target.id, hashSecret(temporaryPin, salt), encode(salt), mustChange = true)
        staffDao.clearLoginLock(target.id)
        val refreshed = staffDao.findById(target.id) ?: target.copy(mustChangeCredential = true)
        AuthResult.Success(refreshed.toModel(), generatedCode = temporaryPin)
    }

    suspend fun changeOwnCredential(account: StaffAccount, newCredential: String): AuthResult = withContext(Dispatchers.IO) {
        credentialValidationError(newCredential)?.let { return@withContext AuthResult.Error(it) }
        val salt = newSalt()
        staffDao.updateCredential(account.id, hashSecret(newCredential, salt), encode(salt), mustChange = false)
        val refreshed = staffDao.findById(account.id) ?: return@withContext AuthResult.Error("Account not found.")
        AuthResult.Success(refreshed.toModel())
    }

    suspend fun setStaffActive(requestingUser: StaffAccount, staffId: String, isActive: Boolean): String? = withContext(Dispatchers.IO) {
        if (!requestingUser.isAdmin) return@withContext "Only Naomi (Admin) can manage staff accounts."
        if (requestingUser.id == staffId && !isActive) return@withContext "The signed-in administrator cannot disable their own account."
        staffDao.setStaffActive(staffId, isActive)
        null
    }

    suspend fun unlockStaff(requestingUser: StaffAccount, staffId: String): String? = withContext(Dispatchers.IO) {
        if (!requestingUser.isAdmin) return@withContext "Only Naomi (Admin) can unlock staff accounts."
        staffDao.clearLoginLock(staffId)
        null
    }

    suspend fun setPermissions(
        requestingUser: StaffAccount,
        staffId: String,
        canVoid: Boolean,
        canExport: Boolean,
        canEditVenues: Boolean,
        canChangeGateway: Boolean,
        canViewTotals: Boolean
    ): String? = withContext(Dispatchers.IO) {
        if (!requestingUser.isAdmin) return@withContext "Only Naomi (Admin) can change permissions."
        val target = staffDao.findById(staffId) ?: return@withContext "Staff account not found."
        if (target.role == StaffRole.ADMIN.name) return@withContext "Administrator permissions are always enabled."
        staffDao.updatePermissions(staffId, canVoid, canExport, canEditVenues, canChangeGateway, canViewTotals)
        null
    }

    suspend fun getOpenShiftForStaff(staffId: String): StaffShift? = withContext(Dispatchers.IO) {
        staffDao.getOpenShiftForStaff(staffId)?.toModel()
    }

    suspend fun openShift(account: StaffAccount, note: String): StaffShift = withContext(Dispatchers.IO) {
        staffDao.getOpenShiftForStaff(account.id)?.toModel()?.let { return@withContext it }
        val entity = StaffShiftEntity(
            id = UUID.randomUUID().toString(),
            staffId = account.id,
            staffDisplayName = account.displayName,
            openedAt = System.currentTimeMillis(),
            openingNote = note.trim()
        )
        staffDao.insertShift(entity)
        entity.toModel()
    }

    suspend fun closeShift(shift: StaffShift, note: String): StaffShift = withContext(Dispatchers.IO) {
        val closedAt = System.currentTimeMillis()
        staffDao.closeShift(shift.id, closedAt, note.trim())
        shift.copy(closedAt = closedAt, closingNote = note.trim())
    }

    private fun newSalt(): ByteArray = ByteArray(24).also { SecureRandom().nextBytes(it) }
    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray? = runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull()

    private fun hashSecret(secret: String, salt: ByteArray): String {
        val algorithm = runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }
            .getOrElse { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") }
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            Base64.encodeToString(algorithm.generateSecret(spec).encoded, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    private fun normalizeRecoveryCode(code: String): String = code.uppercase(Locale.ROOT).replace("-", "").replace(" ", "")

    private fun generateRecoveryCode(): String {
        val random = SecureRandom()
        val raw = buildString {
            repeat(12) { append(RECOVERY_ALPHABET[random.nextInt(RECOVERY_ALPHABET.length)]) }
        }
        return "NCR-${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}"
    }
}

fun StaffAccountEntity.toModel(): StaffAccount = StaffAccount(
    id = id,
    username = username,
    displayName = displayName,
    role = runCatching { StaffRole.valueOf(role) }.getOrDefault(StaffRole.STAFF),
    isActive = isActive,
    createdAt = createdAt,
    lastLoginAt = lastLoginAt,
    failedAttempts = failedAttempts,
    lockedUntil = lockedUntil,
    mustChangeCredential = mustChangeCredential,
    canVoid = canVoid,
    canExport = canExport,
    canEditVenues = canEditVenues,
    canChangeGateway = canChangeGateway,
    canViewTotals = canViewTotals
)

fun StaffShiftEntity.toModel(): StaffShift = StaffShift(
    id = id,
    staffId = staffId,
    staffDisplayName = staffDisplayName,
    openedAt = openedAt,
    closedAt = closedAt,
    openingNote = openingNote,
    closingNote = closingNote
)
