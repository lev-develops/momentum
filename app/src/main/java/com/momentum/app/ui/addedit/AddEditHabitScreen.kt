package com.momentum.app.ui.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.momentum.app.AppContainer
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.model.HabitFrequency
import com.momentum.app.domain.model.HabitIcon
import com.momentum.app.ui.components.imageVector
import com.momentum.app.ui.theme.HabitPalette
import com.momentum.app.ui.theme.LocalMomentumColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    container: AppContainer,
    habitId: Long?,
    onDone: () -> Unit,
) {
    val viewModel: AddEditHabitViewModel = viewModel(
        factory = viewModelFactory { initializer { AddEditHabitViewModel(container, habitId) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMomentumColors.current
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "New habit" else "Edit habit") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = uiState.canSave && !uiState.isSaving) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                ),
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Icon")
            IconPicker(selected = uiState.iconKey, onSelect = viewModel::updateIcon)

            SectionLabel("Color")
            ColorPicker(selected = uiState.colorKey, onSelect = viewModel::updateColor)

            SectionLabel("Frequency")
            FrequencyPicker(
                frequency = uiState.frequency,
                targetDaysPerWeek = uiState.targetDaysPerWeek,
                onFrequencyChange = viewModel::updateFrequency,
                onTargetChange = viewModel::updateTargetDaysPerWeek,
            )

            SectionLabel("Reminder")
            ReminderRow(
                time = uiState.reminderTime,
                onToggle = { enabled ->
                    viewModel.updateReminderTime(if (enabled) LocalTime.of(9, 0) else null)
                },
                onTap = { showTimePicker = true },
            )
        }
    }

    if (showTimePicker) {
        val initial = uiState.reminderTime ?: LocalTime.of(9, 0)
        val timeState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .background(colors.background, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timeState)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        viewModel.updateReminderTime(LocalTime.of(timeState.hour, timeState.minute))
                        showTimePicker = false
                    }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalMomentumColors.current
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: HabitIcon, onSelect: (HabitIcon) -> Unit) {
    val colors = LocalMomentumColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HabitIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.surface else colors.background)
                    .border(1.dp, if (isSelected) colors.textPrimary else colors.hairline, CircleShape)
                    .clickable { onSelect(icon) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon.imageVector(),
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun ColorPicker(selected: HabitColor, onSelect: (HabitColor) -> Unit) {
    val colors = LocalMomentumColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        HabitColor.entries.forEach { colorKey ->
            val isSelected = colorKey == selected
            val swatch = HabitPalette.accent(colorKey, colors.isDark)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, colors.textPrimary, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(colorKey) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.background, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun FrequencyPicker(
    frequency: HabitFrequency,
    targetDaysPerWeek: Int,
    onFrequencyChange: (HabitFrequency) -> Unit,
    onTargetChange: (Int) -> Unit,
) {
    val colors = LocalMomentumColors.current
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FrequencyChip(
                label = "Every day",
                selected = frequency == HabitFrequency.DAILY,
                onClick = { onFrequencyChange(HabitFrequency.DAILY) },
            )
            FrequencyChip(
                label = "N times a week",
                selected = frequency == HabitFrequency.WEEKLY_TARGET,
                onClick = { onFrequencyChange(HabitFrequency.WEEKLY_TARGET) },
            )
        }
        if (frequency == HabitFrequency.WEEKLY_TARGET) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                IconButton(onClick = { onTargetChange(targetDaysPerWeek - 1) }) {
                    Text("–", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                }
                Text(
                    text = "$targetDaysPerWeek days / week",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                )
                IconButton(onClick = { onTargetChange(targetDaysPerWeek + 1) }) {
                    Text("+", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun FrequencyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMomentumColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.surface else colors.background)
            .border(1.dp, if (selected) colors.textPrimary else colors.hairline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
private fun ReminderRow(time: LocalTime?, onToggle: (Boolean) -> Unit, onTap: () -> Unit) {
    val colors = LocalMomentumColors.current
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = time != null, onClick = onTap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = time?.format(formatter) ?: "Off",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
        )
        Switch(checked = time != null, onCheckedChange = onToggle)
    }
}
