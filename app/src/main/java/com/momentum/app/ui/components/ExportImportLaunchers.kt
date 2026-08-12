package com.momentum.app.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.momentum.app.AppContainer
import com.momentum.app.data.export.ImportResult
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun rememberExportLauncher(container: AppContainer): () -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { container.dataExportManager.exportTo(uri) }
                .onSuccess { Toast.makeText(context, "Exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    return {
        scope.launch {
            if (container.habitRepository.getAllHabitsOnce().isEmpty()) {
                Toast.makeText(context, "No habits yet — nothing to export", Toast.LENGTH_SHORT).show()
            } else {
                launcher.launch("momentum-export-${LocalDate.now()}.json")
            }
        }
    }
}

/**
 * Importing wipes every habit and completion currently on the device before restoring from the
 * file, so this confirms with the user before touching anything — picking a file no longer
 * silently discards their existing data.
 */
@Composable
fun rememberImportLauncher(container: AppContainer): () -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingUri = uri
    }

    val uriToConfirm = pendingUri
    if (uriToConfirm != null) {
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "Importing this file will permanently delete every habit and completion " +
                        "currently on this device, then restore from the file. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingUri = null
                    scope.launch {
                        when (val result = container.dataExportManager.importFrom(uriToConfirm)) {
                            is ImportResult.Success -> Toast.makeText(
                                context,
                                "Imported ${result.habitCount} habits, ${result.completionCount} completions",
                                Toast.LENGTH_LONG,
                            ).show()
                            is ImportResult.Failure -> Toast.makeText(context, "Import failed: ${result.message}", Toast.LENGTH_LONG).show()
                        }
                        container.refreshWidgets()
                    }
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUri = null }) { Text("Cancel") }
            },
        )
    }

    return { launcher.launch(arrayOf("application/json")) }
}
