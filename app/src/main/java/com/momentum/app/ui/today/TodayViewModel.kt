package com.momentum.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.app.AppContainer
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.streak.StreakCalculator
import com.momentum.app.util.currentDateFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
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
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
) {
    val doneCount: Int get() = habits.count { it.completedToday }
    val totalCount: Int get() = habits.size
}

class TodayViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.habitRepository
    private val selectedCategory = MutableStateFlow<String?>(null)

    private val baseUiState = combine(
        repository.observeActiveHabits(),
        repository.observeCompletionsByHabit(),
        repository.observeFreezesByHabit(),
        currentDateFlow(container.clock),
        selectedCategory,
    ) { habits, completionsByHabit, freezesByHabit, today, category ->
        val categories = habits.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        val visibleHabits = if (category == null) habits else habits.filter { it.category == category }
        val items = visibleHabits.map { habit ->
            val dates = completionsByHabit[habit.id].orEmpty().map { it.date }.toSet()
            val frozen = freezesByHabit[habit.id].orEmpty()
            TodayHabitItem(
                habit = habit,
                completedToday = today in dates,
                currentStreak = StreakCalculator.currentStreak(dates + frozen, habit.frequency, habit.targetDaysPerWeek, today),
            )
        }
        TodayUiState(isLoading = false, habits = items, categories = categories, selectedCategory = category)
    }

    // A habit mid-"Undo" window hasn't actually been deleted from Room yet (see
    // PendingDeleteHolder), so it's filtered out here rather than left visible until it commits.
    val uiState: StateFlow<TodayUiState> = combine(
        baseUiState,
        container.pendingDeleteHolder.pending,
    ) { state, pending ->
        if (pending == null) {
            state
        } else {
            state.copy(habits = state.habits.filterNot { it.habit.id == pending.habit.id })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun setCategoryFilter(category: String?) {
        selectedCategory.value = category
    }

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
