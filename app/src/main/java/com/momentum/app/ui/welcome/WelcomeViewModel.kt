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
    /** True once the user has signed in, or chosen to skip — caller should navigate onward. */
    val resolved: Boolean = false,
)

/**
 * Drives the one-time "sign in / create an account / skip and stay local" gate shown on first
 * launch. Resolves immediately without showing anything if cloud sync isn't configured for this
 * build, if the user already went through the gate before, or if they're already signed in.
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
                _uiState.value.copy(isLoading = false, showGate = true)
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

    fun resendVerification() {
        viewModelScope.launch {
            auth.sendEmailVerification()
            _uiState.value = _uiState.value.copy(resendStatus = "Verification email sent")
        }
    }

    /** After sign-up, the user can start using the app right away without waiting to verify. */
    fun continueToApp() = completeGate()

    fun skip() = completeGate()

    private fun completeGate() {
        viewModelScope.launch {
            prefs.setAuthGateCompleted(true)
            _uiState.value = _uiState.value.copy(isBusy = false, resolved = true)
        }
    }
}
