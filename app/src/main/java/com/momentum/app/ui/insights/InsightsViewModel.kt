package com.momentum.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momentum.app.AppContainer
import com.momentum.app.domain.insights.InsightsCalculator
import com.momentum.app.domain.insights.InsightsSummary
import com.momentum.app.util.currentDateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InsightsViewModel(container: AppContainer) : ViewModel() {

    val summary: StateFlow<InsightsSummary?> = combine(
        container.habitRepository.observeAllHabits(),
        container.habitRepository.observeCompletionsByHabit(),
        currentDateFlow(container.clock),
    ) { habits, completionsByHabit, today ->
        InsightsCalculator.summarize(
            habits = habits.filter { !it.archived },
            completionsByHabit = completionsByHabit,
            today = today,
            zone = container.clock.zone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
