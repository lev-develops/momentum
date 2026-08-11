package com.momentum.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.momentum.app.MainActivity
import com.momentum.app.MomentumApplication
import com.momentum.app.data.datastore.WidgetPrefsDataStore
import com.momentum.app.data.repository.HabitRepository
import com.momentum.app.domain.model.Habit
import com.momentum.app.ui.components.imageVector
import com.momentum.app.ui.theme.LocalMomentumColors
import com.momentum.app.ui.theme.MomentumTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private val viewModel: WidgetConfigViewModel by viewModels {
        val container = (application as MomentumApplication).container
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WidgetConfigViewModel(container.habitRepository, container.widgetPrefsDataStore) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MomentumTheme {
                WidgetConfigScreen(
                    viewModel = viewModel,
                    onHabitChosen = { habitId -> confirmSelection(habitId) },
                    onCreateHabit = { startActivity(Intent(this, MainActivity::class.java)) },
                )
            }
        }
    }

    private fun confirmSelection(habitId: Long) {
        lifecycleScope.launch {
            viewModel.bindHabit(appWidgetId, habitId)
            MomentumWidget.refreshAll(this@WidgetConfigActivity)
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

private class WidgetConfigViewModel(
    private val repository: HabitRepository,
    private val widgetPrefsDataStore: WidgetPrefsDataStore,
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = repository.observeActiveHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun bindHabit(appWidgetId: Int, habitId: Long) {
        widgetPrefsDataStore.setHabitId(appWidgetId, habitId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    viewModel: WidgetConfigViewModel,
    onHabitChosen: (Long) -> Unit,
    onCreateHabit: () -> Unit,
) {
    val habits by viewModel.habits.collectAsState()
    val colors = LocalMomentumColors.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose a habit") }) },
    ) { padding ->
        if (habits.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text("No habits yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Create a habit in Momentum first, then add this widget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                Button(onClick = onCreateHabit) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Open Momentum", modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(habits, key = { it.id }) { habit ->
                    HabitPickRow(habit = habit, onClick = { onHabitChosen(habit.id) })
                    HorizontalDivider(color = colors.hairline)
                }
            }
        }
    }
}

@Composable
private fun HabitPickRow(habit: Habit, onClick: () -> Unit) {
    val colors = LocalMomentumColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = habit.iconKey.imageVector(),
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = habit.name,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
