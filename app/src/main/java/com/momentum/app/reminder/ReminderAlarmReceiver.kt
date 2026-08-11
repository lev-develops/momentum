package com.momentum.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.momentum.app.MomentumApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_NAME).orEmpty()
        if (habitId < 0) return

        NotificationHelper.notifyReminder(context, habitId, habitName)

        val pendingResult = goAsync()
        val container = (context.applicationContext as MomentumApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = container.habitRepository.getHabit(habitId)
                if (habit != null && !habit.archived && habit.reminderTime != null) {
                    container.reminderScheduler.scheduleReminder(habit)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
