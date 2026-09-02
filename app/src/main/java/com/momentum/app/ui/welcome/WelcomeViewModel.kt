package com.momentum.app.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.app.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WelcomeUiState(
    /** True while deciding whether to show the gate at all — kept brief to avoid a flash. */
    val isLoading: Boolean = true,
    val showGate: Boolean = false,
    val isSignUpMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val verificationSentTo: String? = null,
    val resendStatus: String? = null,
    val passwordResetStatus: String? = null,
    val googleWebClientId: String? = null,
    /** True once the user has signed in — caller should navigate onward. */
    val resolved: Boolean = false,
)

/**
 * Drives the one-time "sign in / create an account" gate shown on first launch. Resolves
 * immediately without showing anything if cloud sync isn't configured for this build (there's
 * nothing to sign in to) or if the user is already signed in. Otherwise signing in is required —
 * there's deliberately no "skip and stay local" escape hatch here.
 */
class WelcomeViewModel(container: AppContainer) : ViewModel() {

    private val auth = container.authManager
    private val prefs = container.appPrefsDataStore

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val alreadyDecided = prefs.getAuthGateCompleted()
            val alreadySignedIn = auth.currentUser != null
            _uiState.value = if (!auth.isConfigured || alreadyDecided || alreadySignedIn) {
                _uiState.value.copy(isLoading = false, resolved = true)
            } else {
                _uiState.value.copy(isLoading = false, showGate = true, googleWebClientId = auth.googleWebClientId())
            }
        }
    }

    fun setSignUpMode(signUp: Boolean) {
        _uiState.value = _uiState.value.copy(isSignUpMode = signUp, errorMessage = null)
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun submit() {
        val state = _uiState.value
        if (state.isBusy) return
        _uiState.value = state.copy(isBusy = true, errorMessage = null)
        viewModelScope.launch {
            val result = if (state.isSignUpMode) {
                auth.signUp(state.email, state.password)
            } else {
                auth.signIn(state.email, state.password)
            }
            result.fold(
                onSuccess = {
                    if (state.isSignUpMode) {
                        auth.sendEmailVerification()
                        _uiState.value = _uiState.value.copy(isBusy = false, verificationSentTo = state.email)
                    } else {
                        completeGate()
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        errorMessage = error.message ?: "Something went wrong",
                    )
                },
            )
        }
    }

    fun onGoogleIdToken(idToken: String) {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
        viewModelScope.launch {
            val result = auth.signInWithGoogleIdToken(idToken)
            result.fold(
                onSuccess = { completeGate() },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isBusy = false, errorMessage = error.message ?: "Google sign-in failed")
                },
            )
        }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.value = _uiState.value.copy(isBusy = false, errorMessage = message)
    }

    fun resendVerification() {
        viewModelScope.launch {
            auth.sendEmailVerification()
            _uiState.value = _uiState.value.copy(resendStatus = "Verification email sent")
        }
    }

    /** After sign-up, the user can start using the app right away without waiting to verify. */
    fun continueToApp() = completeGate()

    fun forgotPassword() {
        val email = _uiState.value.email
        if (email.isBlank() || _uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null, passwordResetStatus = null)
        viewModelScope.launch {
            auth.sendPasswordResetEmail(email)
            // Always show the same message regardless of whether the email is registered, so
            // this can't be used to check which addresses have accounts.
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                passwordResetStatus = "If an account exists for that email, a reset link is on its way.",
            )
        }
    }

    private fun completeGate() {
        viewModelScope.launch {
            prefs.setAuthGateCompleted(true)
            _uiState.value = _uiState.value.copy(isBusy = false, resolved = true)
        }
    }
}
