package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuditRepository
import com.example.data.AuthResult
import com.example.data.StaffAccount
import com.example.data.StaffRepository
import com.example.data.StaffShift
import com.example.data.toModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = true,
    val isAuthenticating: Boolean = false,
    val needsAdminSetup: Boolean = false,
    val currentUser: StaffAccount? = null,
    val activeShift: StaffShift? = null,
    val pendingRecoveryCode: String? = null,
    val adminGeneratedCode: String? = null,
    val adminGeneratedCodeLabel: String? = null,
    val startupError: String? = null,
    val message: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = StaffRepository(database.staffDao())
    private val auditRepository = AuditRepository(database.auditDao())

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val staffAccounts = repository.allStaff
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shiftHistory = repository.allShifts
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditEvents = auditRepository.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                val hasStaff = repository.hasAnyStaff()
                _state.value = AuthUiState(isLoading = false, needsAdminSetup = !hasStaff)
            } catch (error: Throwable) {
                _state.value = AuthUiState(
                    isLoading = false,
                    startupError = "${error.javaClass.simpleName}: ${error.message ?: "Unable to open the local PDA database"}",
                    message = "Local authentication storage could not be opened."
                )
            }
        }
    }

    fun createNaomiAdmin(credential: String, confirmation: String) {
        if (credential != confirmation) {
            _state.value = _state.value.copy(message = "PIN/password confirmation does not match.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.createNaomiAdmin(credential)) {
                is AuthResult.Success -> {
                    auditRepository.log(result.account.id, result.account.displayName, "ADMIN_CREATED", result.account.username, "First-run Naomi administrator setup completed.")
                    _state.value = AuthUiState(
                        isLoading = false,
                        needsAdminSetup = false,
                        currentUser = result.account,
                        pendingRecoveryCode = result.generatedCode,
                        message = "Naomi administrator account created. Save the recovery code now."
                    )
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
            }
        }
    }

    fun login(username: String, credential: String) {
        if (_state.value.isAuthenticating) return

        val normalizedUsername = username.trim()
        _state.value = _state.value.copy(isAuthenticating = true, message = null)

        viewModelScope.launch {
            when (val result = repository.authenticate(normalizedUsername, credential)) {
                is AuthResult.Success -> {
                    val account = result.account

                    // Enter the application as soon as credential verification succeeds.
                    // Shift lookup and audit persistence are important, but they do not need
                    // to block the user's transition into the terminal UI.
                    _state.value = _state.value.copy(
                        isAuthenticating = false,
                        currentUser = account,
                        activeShift = null,
                        message = if (account.mustChangeCredential) {
                            "Temporary credential accepted. Create a new PIN/password now."
                        } else {
                            "Signed in as ${account.displayName}."
                        }
                    )

                    viewModelScope.launch {
                        val shift = repository.getOpenShiftForStaff(account.id)
                        if (_state.value.currentUser?.id == account.id) {
                            _state.value = _state.value.copy(activeShift = shift)
                        }
                        auditRepository.log(account.id, account.displayName, "LOGIN_SUCCESS", account.username)
                    }
                }

                is AuthResult.Error -> {
                    _state.value = _state.value.copy(
                        isAuthenticating = false,
                        message = result.message
                    )
                    viewModelScope.launch {
                        auditRepository.log(
                            null,
                            normalizedUsername.ifBlank { "Unknown" },
                            "LOGIN_FAILED",
                            normalizedUsername,
                            result.message,
                            "WARN"
                        )
                    }
                }
            }
        }
    }

    fun registerStaff(username: String, displayName: String, credential: String, confirmation: String) {
        if (credential != confirmation) {
            _state.value = _state.value.copy(message = "PIN/password confirmation does not match.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.registerStaff(username, displayName, credential)) {
                is AuthResult.Success -> {
                    auditRepository.log(result.account.id, result.account.displayName, "STAFF_REGISTERED", result.account.username, "Account pending Naomi approval.")
                    _state.value = _state.value.copy(message = "Staff account '${result.account.username}' registered and is waiting for Naomi (Admin) approval.")
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
            }
        }
    }

    fun recoverNaomiAdmin(recoveryCode: String, newCredential: String, confirmation: String) {
        if (newCredential != confirmation) {
            _state.value = _state.value.copy(message = "PIN/password confirmation does not match.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.recoverNaomiAdmin(recoveryCode, newCredential)) {
                is AuthResult.Success -> {
                    auditRepository.log(result.account.id, result.account.displayName, "ADMIN_RECOVERED", result.account.username, "Administrator credential reset using recovery code.", "WARN")
                    _state.value = _state.value.copy(
                        currentUser = result.account,
                        activeShift = repository.getOpenShiftForStaff(result.account.id),
                        pendingRecoveryCode = result.generatedCode,
                        message = "Admin access recovered. Your old recovery code is now invalid."
                    )
                }
                is AuthResult.Error -> {
                    auditRepository.log(null, "Recovery", "ADMIN_RECOVERY_FAILED", "naomi", result.message, "WARN")
                    _state.value = _state.value.copy(message = result.message)
                }
            }
        }
    }

    fun acknowledgeRecoveryCode() {
        _state.value = _state.value.copy(pendingRecoveryCode = null, message = "Recovery code acknowledged.")
    }

    fun rotateAdminRecovery() {
        val requester = _state.value.currentUser ?: return
        viewModelScope.launch {
            when (val result = repository.rotateAdminRecovery(requester)) {
                is AuthResult.Success -> {
                    auditRepository.log(requester.id, requester.displayName, "RECOVERY_CODE_ROTATED", requester.username, "A new one-time administrator recovery code was generated.", "WARN")
                    _state.value = _state.value.copy(pendingRecoveryCode = result.generatedCode, message = "New recovery code generated. Save it securely.")
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
            }
        }
    }

    fun changeOwnCredential(newCredential: String, confirmation: String) {
        val user = _state.value.currentUser ?: return
        if (newCredential != confirmation) {
            _state.value = _state.value.copy(message = "PIN/password confirmation does not match.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.changeOwnCredential(user, newCredential)) {
                is AuthResult.Success -> {
                    auditRepository.log(user.id, user.displayName, "CREDENTIAL_CHANGED", user.username)
                    _state.value = _state.value.copy(currentUser = result.account, message = "PIN/password changed successfully.")
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
            }
        }
    }

    fun resetStaffCredential(staffId: String) {
        val requester = _state.value.currentUser ?: return
        viewModelScope.launch {
            when (val result = repository.resetStaffCredential(requester, staffId)) {
                is AuthResult.Success -> {
                    auditRepository.log(requester.id, requester.displayName, "STAFF_CREDENTIAL_RESET", result.account.username, "Temporary PIN issued; change required on next sign-in.", "WARN")
                    _state.value = _state.value.copy(
                        adminGeneratedCode = result.generatedCode,
                        adminGeneratedCodeLabel = "Temporary PIN for ${result.account.displayName}",
                        message = "Temporary PIN generated."
                    )
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
            }
        }
    }

    fun clearAdminGeneratedCode() {
        _state.value = _state.value.copy(adminGeneratedCode = null, adminGeneratedCodeLabel = null)
    }

    fun logout() {
        val user = _state.value.currentUser
        if (user != null) {
            viewModelScope.launch { auditRepository.log(user.id, user.displayName, "LOGOUT", user.username) }
        }
        _state.value = _state.value.copy(
            isAuthenticating = false,
            currentUser = null,
            activeShift = null,
            pendingRecoveryCode = null,
            message = null
        )
    }

    fun openShift(note: String = "") {
        val user = _state.value.currentUser ?: return
        viewModelScope.launch {
            val shift = repository.openShift(user, note)
            auditRepository.log(user.id, user.displayName, "SHIFT_OPENED", shift.id, note.trim())
            _state.value = _state.value.copy(activeShift = shift, message = "Shift opened for ${user.displayName}.")
        }
    }

    fun closeShift(note: String = "") {
        val user = _state.value.currentUser ?: return
        val shift = _state.value.activeShift ?: return
        viewModelScope.launch {
            val closed = repository.closeShift(shift, note)
            auditRepository.log(user.id, user.displayName, "SHIFT_CLOSED", shift.id, note.trim())
            _state.value = _state.value.copy(
                activeShift = null,
                message = "Shift closed at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.UK).format(java.util.Date(closed.closedAt ?: 0L))}."
            )
        }
    }

    fun setStaffActive(staffId: String, active: Boolean) {
        val requester = _state.value.currentUser ?: return
        viewModelScope.launch {
            val target = staffAccounts.value.firstOrNull { it.id == staffId }
            val error = repository.setStaffActive(requester, staffId, active)
            if (error == null) {
                auditRepository.log(requester.id, requester.displayName, if (active) "STAFF_ENABLED" else "STAFF_DISABLED", target?.username ?: staffId)
            }
            _state.value = _state.value.copy(message = error ?: if (active) "Staff account approved and enabled." else "Staff account disabled.")
        }
    }

    fun unlockStaff(staffId: String) {
        val requester = _state.value.currentUser ?: return
        viewModelScope.launch {
            val target = staffAccounts.value.firstOrNull { it.id == staffId }
            val error = repository.unlockStaff(requester, staffId)
            if (error == null) auditRepository.log(requester.id, requester.displayName, "STAFF_UNLOCKED", target?.username ?: staffId)
            _state.value = _state.value.copy(message = error ?: "Staff login lock cleared.")
        }
    }

    fun setStaffPermissions(
        staffId: String,
        canVoid: Boolean,
        canExport: Boolean,
        canEditVenues: Boolean,
        canChangeGateway: Boolean,
        canViewTotals: Boolean
    ) {
        val requester = _state.value.currentUser ?: return
        viewModelScope.launch {
            val target = staffAccounts.value.firstOrNull { it.id == staffId }
            val error = repository.setPermissions(requester, staffId, canVoid, canExport, canEditVenues, canChangeGateway, canViewTotals)
            if (error == null) {
                auditRepository.log(
                    requester.id,
                    requester.displayName,
                    "STAFF_PERMISSIONS_CHANGED",
                    target?.username ?: staffId,
                    "void=$canVoid export=$canExport venues=$canEditVenues gateway=$canChangeGateway totals=$canViewTotals"
                )
            }
            _state.value = _state.value.copy(message = error ?: "Staff permissions updated.")
        }
    }

    fun logAudit(action: String, target: String = "", details: String = "", severity: String = "INFO") {
        val user = _state.value.currentUser
        viewModelScope.launch {
            auditRepository.log(user?.id, user?.displayName ?: "System", action, target, details, severity)
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
