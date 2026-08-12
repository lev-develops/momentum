package com.momentum.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.momentum.app.ui.theme.LocalMomentumColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: AccountViewModel = viewModel(
        factory = viewModelFactory { initializer { AccountViewModel(container) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMomentumColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Cloud sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!uiState.isConfigured) {
                Text(
                    text = "Cloud sync isn't set up yet. This build has no Firebase project " +
                        "configured, so habits stay local-only on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                return@Scaffold
            }

            Text(
                text = "Momentum still works fully offline. Signing in backs your habits up to " +
                    "the cloud and keeps them in sync across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )

            val email = uiState.signedInEmail
            if (email != null) {
                SignedInSection(email = email, isBusy = uiState.isBusy, viewModel = viewModel)
            } else {
                SignedOutSection(uiState = uiState, viewModel = viewModel)
            }

            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.isError) MaterialTheme.colorScheme.error else colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SignedInSection(email: String, isBusy: Boolean, viewModel: AccountViewModel) {
    val colors = LocalMomentumColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Signed in as", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Text(text = email, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::syncNow, enabled = !isBusy) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Sync now")
            }
            OutlinedButton(onClick = viewModel::signOut, enabled = !isBusy) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun SignedOutSection(uiState: AccountUiState, viewModel: AccountViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.emailField,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.passwordField,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        val canSubmit = uiState.emailField.isNotBlank() && uiState.passwordField.length >= 6 && !uiState.isBusy
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::signIn, enabled = canSubmit) { Text("Sign in") }
            TextButton(onClick = viewModel::signUp, enabled = canSubmit) { Text("Create account") }
        }
    }
}
