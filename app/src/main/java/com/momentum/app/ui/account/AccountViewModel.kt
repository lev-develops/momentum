package com.momentum.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.app.AppContainer
import com.momentum.app.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val isConfigured: Boolean = false,
    val signedInEmail: String? = null,
    val emailField: String = "",
    val passwordField: String = "",
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
)

class AccountViewModel(private val container: AppContainer) : ViewModel() {

    private val auth = container.authManager
    private val sync = container.cloudSyncRepository

    private val _formState = MutableStateFlow(AccountUiState(isConfigured = auth.isConfigured))
    val uiState: StateFlow<AccountUiState> = combine(_formState, auth.authStateFlow()) { form, user ->
        form.copy(signedInEmail = user?.email)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        _formState.value.copy(signedInEmail = auth.currentUser?.email),
    )

    fun updateEmail(value: String) {
        _formState.value = _formState.value.copy(emailField = value)
    }

    fun updatePassword(value: String) {
        _formState.value = _formState.value.copy(passwordField = value)
    }

    fun signIn() = runAuthAction { auth.signIn(_formState.value.emailField, _formState.value.passwordField) }

    fun signUp() = runAuthAction { auth.signUp(_formState.value.emailField, _formState.value.passwordField) }

    fun signOut() {
        auth.signOut()
        _formState.value = _formState.value.copy(statusMessage = "Signed out", isError = false)
    }

    fun syncNow() {
        if (_formState.value.isBusy) return
        _formState.value = _formState.value.copy(isBusy = true, statusMessage = null)
        viewModelScope.launch {
            when (val result = sync.sync()) {
                is SyncResult.Success -> _formState.value = _formState.value.copy(
                    isBusy = false,
                    isError = false,
                    statusMessage = "Synced ${result.habitCount} habits, ${result.completionCount} completions",
                )
                is SyncResult.Failure -> _formState.value = _formState.value.copy(
                    isBusy = false,
                    isError = true,
                    statusMessage = result.message,
                )
            }
        }
    }

    private fun runAuthAction(action: suspend () -> Result<Unit>) {
        if (_formState.value.isBusy) return
        _formState.value = _formState.value.copy(isBusy = true, statusMessage = null)
        viewModelScope.launch {
            val result = action()
            _formState.value = result.fold(
                onSuccess = { _formState.value.copy(isBusy = false, isError = false, statusMessage = null, passwordField = "") },
                onFailure = { _formState.value.copy(isBusy = false, isError = true, statusMessage = it.message ?: "Something went wrong") },
            )
            if (result.isSuccess) syncNow()
        }
    }
}
