package com.momentum.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.momentum.app.MomentumApplication
import com.momentum.app.domain.model.HabitFrequency
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires roughly once a week (Sunday evening) with a short week-in-review notification, then
 * reschedules itself for the following week. */
class WeeklySummaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as MomentumApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now(container.clock)
                val weekStart = today.minusDays(6)
                val habits = container.habitRepository.getAllHabitsOnce().filter { !it.archived }

                if (habits.isNotEmpty()) {
                    val completionsByHabit = container.habitRepository.getAllCompletionsOnce()
                        .filter { it.date in weekStart..today }
                        .groupBy { it.habitId }

                    var expected = 0
                    var done = 0
                    var bestHabitName: String? = null
                    var bestHabitCount = -1
                    habits.forEach { habit ->
                        val habitExpected = when (habit.frequency) {
                            HabitFrequency.DAILY -> 7
                            HabitFrequency.WEEKLY_TARGET -> habit.targetDaysPerWeek
                        }
                        val habitDone = completionsByHabit[habit.id]?.size ?: 0
                        expected += habitExpected
                        done += habitDone
                        if (habitDone > bestHabitCount) {
                            bestHabitCount = habitDone
                            bestHabitName = habit.name
                        }
                    }

                    NotificationHelper.notifyWeeklySummary(
                        context,
                        completed = done,
                        expected = expected,
                        bestHabitName = bestHabitName,
                    )
                }

                container.reminderScheduler.scheduleWeeklySummary()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
