package com.momentum.app.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
    return { launcher.launch("momentum-export-${LocalDate.now()}.json") }
}

@Composable
fun rememberImportLauncher(container: AppContainer): () -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val result = container.dataExportManager.importFrom(uri)) {
                is ImportResult.Success -> Toast.makeText(
                    context,
                    "Imported ${result.habitCount} habits, ${result.completionCount} completions",
                    Toast.LENGTH_LONG,
                ).show()
                is ImportResult.Failure -> Toast.makeText(context, "Import failed: ${result.message}", Toast.LENGTH_LONG).show()
            }
            container.refreshWidgets()
        }
    }
    return { launcher.launch(arrayOf("application/json")) }
}
