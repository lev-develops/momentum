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
)

class HabitDetailViewModel(private val container: AppContainer, private val habitId: Long) : ViewModel() {

    private val repository = container.habitRepository

    val uiState: StateFlow<HabitDetailUiState> = combine(
        repository.observeHabit(habitId),
        repository.observeCompletedDates(habitId),
        currentDateFlow(container.clock),
    ) { habit, dates, today ->
        if (habit == null) {
            HabitDetailUiState(isLoading = false, habit = null)
        } else {
            HabitDetailUiState(
                isLoading = false,
                habit = habit,
                grid = ContributionGridBuilder.build(dates, habit.targetDaysPerWeek, today),
                completedToday = today in dates,
                currentStreak = StreakCalculator.currentStreak(dates, habit.frequency, habit.targetDaysPerWeek, today),
                bestStreak = StreakCalculator.bestStreak(dates, habit.frequency, habit.targetDaysPerWeek, today),
                completionRate = StreakCalculator.completionRate(
                    dates,
                    habit.createdAt.atZone(container.clock.zone).toLocalDate(),
                    habit.frequency,
                    habit.targetDaysPerWeek,
                    today,
                ),
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

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val habit = uiState.value.habit ?: return@launch
            repository.deleteHabit(habit)
            container.reminderScheduler.cancelReminder(habitId)
            container.refreshWidgets()
            onDone()
        }
    }
}
