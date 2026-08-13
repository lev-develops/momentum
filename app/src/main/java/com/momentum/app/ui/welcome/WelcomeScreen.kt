package com.momentum.app.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.momentum.app.AppContainer
import com.momentum.app.ui.components.GoogleSignInButton
import com.momentum.app.ui.theme.LocalMomentumColors

/** One-time gate shown on first launch: sign in/create an account with cloud sync, or skip and
 * use the app fully offline. Resolves instantly (no UI shown) once the choice is made. */
@Composable
fun WelcomeScreen(container: AppContainer, onDone: () -> Unit) {
    val viewModel: WelcomeViewModel = viewModel(
        factory = viewModelFactory { initializer { WelcomeViewModel(container) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMomentumColors.current

    LaunchedEffect(uiState.resolved) {
        if (uiState.resolved) onDone()
    }

    if (uiState.isLoading || uiState.resolved) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background))
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Momentum", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Text(
                    text = "Back up your habits and keep them in sync across devices — or skip " +
                        "and keep everything on this device only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            val verificationEmail = uiState.verificationSentTo
            if (verificationEmail != null) {
                VerifyEmailCard(
                    email = verificationEmail,
                    resendStatus = uiState.resendStatus,
                    onResend = viewModel::resendVerification,
                    onContinue = viewModel::continueToApp,
                )
            } else {
                AuthForm(uiState = uiState, viewModel = viewModel)
                TextButton(onClick = viewModel::skip) {
                    Text("Skip — use Momentum locally", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun AuthForm(uiState: WelcomeUiState, viewModel: WelcomeViewModel) {
    val colors = LocalMomentumColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { viewModel.setSignUpMode(true) }) {
                Text(
                    text = "Create account",
                    color = if (uiState.isSignUpMode) colors.textPrimary else colors.textSecondary,
                )
            }
            TextButton(onClick = { viewModel.setSignUpMode(false) }) {
                Text(
                    text = "Sign in",
                    color = if (!uiState.isSignUpMode) colors.textPrimary else colors.textSecondary,
                )
            }
        }
        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.errorMessage?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        val canSubmit = uiState.email.isNotBlank() && uiState.password.length >= 6 && !uiState.isBusy
        Button(onClick = viewModel::submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.isSignUpMode) "Create account" else "Sign in")
        }
        uiState.googleWebClientId?.let { webClientId ->
            GoogleSignInButton(
                webClientId = webClientId,
                onIdToken = viewModel::onGoogleIdToken,
                onError = viewModel::onGoogleSignInError,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun VerifyEmailCard(
    email: String,
    resendStatus: String?,
    onResend: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = LocalMomentumColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Confirm your email", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(
            text = "We sent a verification link to $email. Open it to confirm your address — " +
                "you can keep using the app in the meantime.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        resendStatus?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onContinue) { Text("Continue") }
            TextButton(onClick = onResend) { Text("Resend email") }
        }
    }
}
