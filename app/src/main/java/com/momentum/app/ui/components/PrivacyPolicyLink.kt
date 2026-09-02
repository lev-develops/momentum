package com.momentum.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.momentum.app.ui.theme.LocalMomentumColors

/**
 * TODO: replace with your published policy's real URL before shipping — Play Console requires
 * the privacy policy link to be publicly reachable (no sign-in) at the URL you submit there.
 */
const val PRIVACY_POLICY_URL = "https://claude.ai/code/artifact/f1909bf7-eafd-48a7-9bbf-2346486d18de"

@Composable
fun PrivacyPolicyLink(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalMomentumColors.current
    TextButton(
        modifier = modifier,
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        },
    ) {
        Text("Privacy policy", color = colors.textSecondary)
    }
}
