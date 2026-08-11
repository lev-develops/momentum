package com.momentum.app.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.app.AppContainer
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.model.HabitFrequency
import com.momentum.app.domain.model.HabitIcon
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
    val name: String = "",
    val iconKey: HabitIcon = HabitIcon.Default,
    val colorKey: HabitColor = HabitColor.Default,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDaysPerWeek: Int = 7,
    val reminderTime: LocalTime? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

class AddEditHabitViewModel(private val container: AppContainer, private val habitId: Long?) : ViewModel() {

    private val repository = container.habitRepository

    private val _uiState = MutableStateFlow(AddEditUiState(isNew = habitId == null))
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private var originalHabit: Habit? = null

    init {
        if (habitId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                val habit = repository.getHabit(habitId)
                originalHabit = habit
                if (habit != null) {
                    _uiState.value = AddEditUiState(
                        isNew = false,
                        isLoading = false,
                        name = habit.name,
                        iconKey = habit.iconKey,
                        colorKey = habit.colorKey,
                        frequency = habit.frequency,
                        targetDaysPerWeek = habit.targetDaysPerWeek,
                        reminderTime = habit.reminderTime,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateIcon(icon: HabitIcon) {
        _uiState.value = _uiState.value.copy(iconKey = icon)
    }

    fun updateColor(color: HabitColor) {
        _uiState.value = _uiState.value.copy(colorKey = color)
    }

    fun updateFrequency(frequency: HabitFrequency) {
        _uiState.value = _uiState.value.copy(
            frequency = frequency,
            targetDaysPerWeek = if (frequency == HabitFrequency.DAILY) 7 else _uiState.value.targetDaysPerWeek.coerceIn(1, 6),
        )
    }

    fun updateTargetDaysPerWeek(days: Int) {
        _uiState.value = _uiState.value.copy(targetDaysPerWeek = days.coerceIn(1, 7))
    }

    fun updateReminderTime(time: LocalTime?) {
        _uiState.value = _uiState.value.copy(reminderTime = time)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val existing = originalHabit
            val habit = Habit(
                id = existing?.id ?: 0L,
                name = state.name.trim(),
                iconKey = state.iconKey,
                colorKey = state.colorKey,
                frequency = state.frequency,
                targetDaysPerWeek = state.targetDaysPerWeek,
                reminderTime = state.reminderTime,
                sortOrder = existing?.sortOrder ?: repository.getAllHabitsOnce().size,
                createdAt = existing?.createdAt ?: Instant.now(container.clock),
                archived = existing?.archived ?: false,
            )

            val savedId = if (existing == null) repository.addHabit(habit) else {
                repository.updateHabit(habit)
                habit.id
            }

            if (habit.reminderTime != null) {
                container.reminderScheduler.scheduleReminder(habit.copy(id = savedId))
            } else {
                container.reminderScheduler.cancelReminder(savedId)
            }
            container.refreshWidgets()

            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }
}
