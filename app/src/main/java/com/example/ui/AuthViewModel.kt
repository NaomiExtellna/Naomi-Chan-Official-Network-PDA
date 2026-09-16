package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
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
    val needsAdminSetup: Boolean = false,
    val currentUser: StaffAccount? = null,
    val activeShift: StaffShift? = null,
    val message: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StaffRepository(AppDatabase.getDatabase(application).staffDao())

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val staffAccounts = repository.allStaff
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shiftHistory = repository.allShifts
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val hasStaff = repository.hasAnyStaff()
            _state.value = AuthUiState(
                isLoading = false,
                needsAdminSetup = !hasStaff
            )
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
                    _state.value = AuthUiState(
                        isLoading = false,
                        needsAdminSetup = false,
                        currentUser = result.account,
                        message = "Naomi administrator account created."
                    )
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
            }
        }
    }

    fun login(username: String, credential: String) {
        viewModelScope.launch {
            when (val result = repository.authenticate(username, credential)) {
                is AuthResult.Success -> {
                    val shift = repository.getOpenShiftForStaff(result.account.id)
                    _state.value = _state.value.copy(
                        currentUser = result.account,
                        activeShift = shift,
                        message = "Signed in as ${result.account.displayName}."
                    )
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
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
                    _state.value = _state.value.copy(
                        message = "Staff account '${result.account.username}' registered and is waiting for Naomi (Admin) approval."
                    )
                }
                is AuthResult.Error -> _state.value = _state.value.copy(message = result.message)
            }
        }
    }

    fun logout() {
        _state.value = _state.value.copy(
            currentUser = null,
            activeShift = null,
            message = null
        )
    }

    fun openShift(note: String = "") {
        val user = _state.value.currentUser ?: return
        viewModelScope.launch {
            val shift = repository.openShift(user, note)
            _state.value = _state.value.copy(
                activeShift = shift,
                message = "Shift opened for ${user.displayName}."
            )
        }
    }

    fun closeShift(note: String = "") {
        val shift = _state.value.activeShift ?: return
        viewModelScope.launch {
            val closed = repository.closeShift(shift, note)
            _state.value = _state.value.copy(
                activeShift = null,
                message = "Shift closed at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.UK).format(java.util.Date(closed.closedAt ?: 0L))}."
            )
        }
    }

    fun setStaffActive(staffId: String, active: Boolean) {
        val requester = _state.value.currentUser ?: return
        viewModelScope.launch {
            val error = repository.setStaffActive(requester, staffId, active)
            _state.value = _state.value.copy(
                message = error ?: if (active) "Staff account approved and enabled." else "Staff account disabled."
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
