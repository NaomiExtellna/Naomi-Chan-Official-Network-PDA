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
    val lastLoginAt: Long?
) {
    val isAdmin: Boolean get() = role == StaffRole.ADMIN
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
    data class Success(val account: StaffAccount) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class StaffRepository(private val staffDao: StaffDao) {

    companion object {
        const val RESERVED_ADMIN_USERNAME = "naomi"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
    }

    val allStaff: Flow<List<StaffAccountEntity>> = staffDao.observeAllStaff()
    val allShifts: Flow<List<StaffShiftEntity>> = staffDao.observeAllShifts()

    suspend fun hasAnyStaff(): Boolean = withContext(Dispatchers.IO) {
        staffDao.countStaff() > 0
    }

    suspend fun createNaomiAdmin(credential: String): AuthResult = withContext(Dispatchers.IO) {
        if (staffDao.countStaff() > 0) {
            return@withContext AuthResult.Error("Administrator setup has already been completed.")
        }
        if (credential.length < 4) {
            return@withContext AuthResult.Error("Use at least 4 characters for the admin PIN/password.")
        }

        val salt = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val account = StaffAccountEntity(
            id = UUID.randomUUID().toString(),
            username = RESERVED_ADMIN_USERNAME,
            displayName = "Naomi",
            credentialHash = hashCredential(credential, salt),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            role = StaffRole.ADMIN.name,
            isActive = true
        )
        staffDao.insertStaff(account)
        AuthResult.Success(account.toModel())
    }

    suspend fun registerStaff(username: String, displayName: String, credential: String): AuthResult = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim().lowercase(Locale.ROOT)
        val cleanDisplayName = displayName.trim()

        when {
            normalizedUsername.length < 3 -> return@withContext AuthResult.Error("Username must be at least 3 characters.")
            normalizedUsername == RESERVED_ADMIN_USERNAME || normalizedUsername == "admin" -> {
                return@withContext AuthResult.Error("That username is reserved for the administrator.")
            }
            cleanDisplayName.length < 2 -> return@withContext AuthResult.Error("Enter the staff member's display name.")
            credential.length < 4 -> return@withContext AuthResult.Error("Use at least 4 characters for the PIN/password.")
            staffDao.findByUsername(normalizedUsername) != null -> return@withContext AuthResult.Error("That username is already registered.")
        }

        val salt = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val account = StaffAccountEntity(
            id = UUID.randomUUID().toString(),
            username = normalizedUsername,
            displayName = cleanDisplayName,
            credentialHash = hashCredential(credential, salt),
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            role = StaffRole.STAFF.name,
            isActive = false
        )
        staffDao.insertStaff(account)
        AuthResult.Success(account.toModel())
    }

    suspend fun authenticate(username: String, credential: String): AuthResult = withContext(Dispatchers.IO) {
        val account = staffDao.findByUsername(username.trim())
            ?: return@withContext AuthResult.Error("Incorrect username or PIN/password.")

        if (!account.isActive) {
            return@withContext AuthResult.Error("This staff account is pending or disabled. Ask Naomi (Admin) to enable it.")
        }

        val salt = try {
            Base64.decode(account.salt, Base64.NO_WRAP)
        } catch (_: Exception) {
            return@withContext AuthResult.Error("Account credentials are invalid. Contact the administrator.")
        }
        val candidate = hashCredential(credential, salt)
        val matches = MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            account.credentialHash.toByteArray(Charsets.UTF_8)
        )
        if (!matches) {
            return@withContext AuthResult.Error("Incorrect username or PIN/password.")
        }

        val loginAt = System.currentTimeMillis()
        staffDao.updateLastLogin(account.id, loginAt)
        AuthResult.Success(account.copy(lastLoginAt = loginAt).toModel())
    }

    suspend fun setStaffActive(requestingUser: StaffAccount, staffId: String, isActive: Boolean): String? = withContext(Dispatchers.IO) {
        if (!requestingUser.isAdmin) return@withContext "Only Naomi (Admin) can manage staff accounts."
        if (requestingUser.id == staffId && !isActive) return@withContext "The signed-in administrator cannot disable their own account."
        staffDao.setStaffActive(staffId, isActive)
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

    private fun hashCredential(credential: String, salt: ByteArray): String {
        val algorithm = runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }
            .getOrElse { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") }
        val spec = PBEKeySpec(credential.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            Base64.encodeToString(algorithm.generateSecret(spec).encoded, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }
}

fun StaffAccountEntity.toModel(): StaffAccount = StaffAccount(
    id = id,
    username = username,
    displayName = displayName,
    role = runCatching { StaffRole.valueOf(role) }.getOrDefault(StaffRole.STAFF),
    isActive = isActive,
    createdAt = createdAt,
    lastLoginAt = lastLoginAt
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
