package com.momentum.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.app.AppContainer
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.streak.StreakCalculator
import com.momentum.app.util.currentDateFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayHabitItem(
    val habit: Habit,
    val completedToday: Boolean,
    val currentStreak: Int,
)

data class TodayUiState(
    val isLoading: Boolean = true,
    val habits: List<TodayHabitItem> = emptyList(),
) {
    val doneCount: Int get() = habits.count { it.completedToday }
    val totalCount: Int get() = habits.size
}

class TodayViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.habitRepository

    val uiState: StateFlow<TodayUiState> = combine(
        repository.observeActiveHabits(),
        repository.observeCompletionsByHabit(),
        currentDateFlow(container.clock),
    ) { habits, completionsByHabit, today ->
        val items = habits.map { habit ->
            val dates = completionsByHabit[habit.id].orEmpty().map { it.date }.toSet()
            TodayHabitItem(
                habit = habit,
                completedToday = today in dates,
                currentStreak = StreakCalculator.currentStreak(dates, habit.frequency, habit.targetDaysPerWeek, today),
            )
        }
        TodayUiState(isLoading = false, habits = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun toggle(habitId: Long) {
        viewModelScope.launch {
            val today = LocalDate.now(container.clock)
            repository.toggleCompletion(habitId, today)
            container.refreshWidgets()
        }
    }

    fun reorder(orderedHabitIds: List<Long>) {
        viewModelScope.launch {
            repository.reorder(orderedHabitIds)
        }
    }
}
