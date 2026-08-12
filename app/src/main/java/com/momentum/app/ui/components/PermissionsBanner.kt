package com.momentum.app.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.momentum.app.ui.theme.LocalMomentumColors

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun exactAlarmGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return true
    return alarmManager.canScheduleExactAlarms()
}

/**
 * Requests POST_NOTIFICATIONS once on first appearance (Android 13+), then shows a dismissible
 * banner whenever reminders can't fully work — either that permission or exact-alarm scheduling
 * (Android 12+) is denied. Without this, reminders silently degrade to "late or never" with no
 * explanation anywhere in the app.
 */
@Composable
fun PermissionsBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = LocalMomentumColors.current

    var notifGranted by remember { mutableStateOf(notificationsGranted(context)) }
    var alarmGranted by remember { mutableStateOf(exactAlarmGranted(context)) }
    var dismissed by remember { mutableStateOf(false) }
    var requestedOnce by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = notificationsGranted(context)
                alarmGranted = exactAlarmGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!requestedOnce && !notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestedOnce = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (dismissed || (notifGranted && alarmGranted)) return

    val message = when {
        !notifGranted && !alarmGranted -> "Notifications and exact-time alarms are both off, so reminders won't show reliably."
        !notifGranted -> "Notifications are off, so habit reminders won't show."
        else -> "Exact-alarm scheduling is off, so reminders may arrive late."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.NotificationsOff, contentDescription = null, tint = colors.textSecondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
            Text(
                text = "Fix in settings",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                modifier = Modifier.clickable {
                    openRelevantSettings(context, notifGranted)
                },
            )
        }
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Dismiss",
            tint = colors.textSecondary,
            modifier = Modifier.clickable { dismissed = true },
        )
    }
}

private fun openRelevantSettings(context: Context, notifGranted: Boolean) {
    val intent = if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
