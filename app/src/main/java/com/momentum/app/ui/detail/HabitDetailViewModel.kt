package com.momentum.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.app.AppContainer
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.streak.ContributionGrid
import com.momentum.app.domain.streak.ContributionGridBuilder
import com.momentum.app.domain.streak.StreakCalculator
import com.momentum.app.util.currentDateFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitDetailUiState(
    val isLoading: Boolean = true,
    val habit: Habit? = null,
    val grid: ContributionGrid? = null,
    val completedToday: Boolean = false,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completionRate: Float = 0f,
    val deleted: Boolean = false,
    val freezesAvailable: Int = 0,
    val canUseFreeze: Boolean = false,
)

class HabitDetailViewModel(private val container: AppContainer, private val habitId: Long) : ViewModel() {

    private val repository = container.habitRepository

    val uiState: StateFlow<HabitDetailUiState> = combine(
        repository.observeHabit(habitId),
        repository.observeCompletedDates(habitId),
        repository.observeFrozenDates(habitId),
        currentDateFlow(container.clock),
    ) { habit, dates, frozen, today ->
        if (habit == null) {
            HabitDetailUiState(isLoading = false, habit = null)
        } else {
            val countedDates = dates + frozen
            val yesterday = today.minusDays(1)
            HabitDetailUiState(
                isLoading = false,
                habit = habit,
                grid = ContributionGridBuilder.build(countedDates, habit.targetDaysPerWeek, today),
                completedToday = today in dates,
                currentStreak = StreakCalculator.currentStreak(countedDates, habit.frequency, habit.targetDaysPerWeek, today),
                bestStreak = StreakCalculator.bestStreak(countedDates, habit.frequency, habit.targetDaysPerWeek, today),
                completionRate = StreakCalculator.completionRate(
                    dates,
                    habit.createdAt.atZone(container.clock.zone).toLocalDate(),
                    habit.frequency,
                    habit.targetDaysPerWeek,
                    today,
                ),
                freezesAvailable = habit.freezesAvailable,
                canUseFreeze = habit.freezesAvailable > 0 && yesterday !in countedDates && !yesterday.isBefore(habit.createdAt.atZone(container.clock.zone).toLocalDate()),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())

    fun toggleToday() {
        viewModelScope.launch {
            val today = LocalDate.now(container.clock)
            repository.toggleCompletion(habitId, today)
            container.refreshWidgets()
        }
    }

    fun useFreeze() {
        viewModelScope.launch {
            val today = LocalDate.now(container.clock)
            repository.useFreeze(habitId, today.minusDays(1))
            container.refreshWidgets()
        }
    }

    /**
     * Doesn't delete anything yet — hands the habit to [PendingDeleteHolder] so the Today screen
     * (where the user lands right after) can offer "Undo" before the delete actually commits.
     */
    fun delete(onDone: () -> Unit) {
        val habit = uiState.value.habit ?: return
        container.pendingDeleteHolder.begin(habit) {
            repository.deleteHabit(habit)
            container.reminderScheduler.cancelReminder(habit.id)
            container.widgetPrefsDataStore.clearForHabit(habit.id)
            container.refreshWidgets()
        }
        onDone()
    }
}
