package com.momentum.app.ui.components

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * "Continue with Google" via Credential Manager (the current, non-deprecated Google sign-in
 * API) — resolves a Google ID token and hands it to [onIdToken] for the caller to complete the
 * Firebase sign-in. [webClientId] is Firebase's auto-generated OAuth web client id; callers
 * should only show this button when [com.momentum.app.sync.AuthManager.googleSignInAvailable]
 * is true.
 */
@Composable
fun GoogleSignInButton(
    webClientId: String,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        onClick = {
            scope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val option = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .build()
                    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                    val response = credentialManager.getCredential(context, request)
                    val credential = response.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        onIdToken(idToken)
                    } else {
                        onError("Unexpected credential type from Google")
                    }
                } catch (e: GetCredentialException) {
                    onError(e.message ?: "Google sign-in was cancelled or failed")
                }
            }
        },
    ) {
        Text("Continue with Google")
    }
}
