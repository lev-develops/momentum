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
    val isEmailVerified: Boolean = true,
    val emailField: String = "",
    val passwordField: String = "",
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val googleWebClientId: String? = null,
)

class AccountViewModel(private val container: AppContainer) : ViewModel() {

    private val auth = container.authManager
    private val sync = container.cloudSyncRepository

    private val _formState = MutableStateFlow(
        AccountUiState(isConfigured = auth.isConfigured, googleWebClientId = auth.googleWebClientId()),
    )
    val uiState: StateFlow<AccountUiState> = combine(_formState, auth.authStateFlow()) { form, user ->
        form.copy(signedInEmail = user?.email, isEmailVerified = user?.isEmailVerified ?: true)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        _formState.value.copy(signedInEmail = auth.currentUser?.email, isEmailVerified = auth.isEmailVerified),
    )

    fun updateEmail(value: String) {
        _formState.value = _formState.value.copy(emailField = value)
    }

    fun updatePassword(value: String) {
        _formState.value = _formState.value.copy(passwordField = value)
    }

    fun signIn() = runAuthAction { auth.signIn(_formState.value.emailField, _formState.value.passwordField) }

    fun signUp() = runAuthAction { auth.signUp(_formState.value.emailField, _formState.value.passwordField) }

    fun onGoogleIdToken(idToken: String) = runAuthAction { auth.signInWithGoogleIdToken(idToken) }

    fun onGoogleSignInError(message: String) {
        _formState.value = _formState.value.copy(isError = true, statusMessage = message)
    }

    fun forgotPassword() {
        val email = _formState.value.emailField
        if (email.isBlank() || _formState.value.isBusy) return
        _formState.value = _formState.value.copy(isBusy = true, statusMessage = null)
        viewModelScope.launch {
            auth.sendPasswordResetEmail(email)
            _formState.value = _formState.value.copy(
                isBusy = false,
                isError = false,
                statusMessage = "If an account exists for that email, a reset link is on its way.",
            )
        }
    }

    fun signOut() {
        auth.signOut()
        _formState.value = _formState.value.copy(statusMessage = "Signed out", isError = false)
    }

    /** Deletes the cloud account and everything stored under it in Firestore. Local Room data on
     * this device is untouched — the two are independent by design. */
    fun deleteAccount() {
        if (_formState.value.isBusy) return
        _formState.value = _formState.value.copy(isBusy = true, statusMessage = null)
        viewModelScope.launch {
            sync.deleteAllRemoteData()
            val result = auth.deleteAccount()
            if (result.isSuccess) {
                // A login-required build should re-show the welcome gate on next launch now that
                // this account no longer exists, not silently let the old flag through.
                container.appPrefsDataStore.setAuthGateCompleted(false)
                _formState.value = _formState.value.copy(isBusy = false, isError = false, statusMessage = "Account deleted")
            } else {
                _formState.value = _formState.value.copy(
                    isBusy = false,
                    isError = true,
                    statusMessage = result.exceptionOrNull()?.message ?: "Couldn't delete account",
                )
            }
        }
    }

    fun resendVerification() {
        if (_formState.value.isBusy) return
        _formState.value = _formState.value.copy(isBusy = true, statusMessage = null)
        viewModelScope.launch {
            val result = auth.sendEmailVerification()
            _formState.value = result.fold(
                onSuccess = { _formState.value.copy(isBusy = false, isError = false, statusMessage = "Verification email sent") },
                onFailure = { _formState.value.copy(isBusy = false, isError = true, statusMessage = it.message ?: "Couldn't send verification email") },
            )
        }
    }

    fun syncNow() {
        if (_formState.value.isBusy) return
        _formState.value = _formState.value.copy(isBusy = true, statusMessage = null)
        viewModelScope.launch {
            when (val result = sync.sync()) {
                is SyncResult.Success -> {
                    // Widgets read the DB once when they redraw — nothing else invalidates them
                    // when data changes underneath via sync, so without this a habit renamed or
                    // completed on another device stays stale on this device's home screen.
                    container.refreshWidgets()
                    _formState.value = _formState.value.copy(
                        isBusy = false,
                        isError = false,
                        statusMessage = "Synced ${result.habitCount} habits, ${result.completionCount} completions",
                    )
                }
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
