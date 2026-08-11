package com.momentum.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.momentum.app.MomentumApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager alarms don't survive reboot; re-arm every habit reminder plus the midnight rollover. */
class BootRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as MomentumApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = container.habitRepository.getAllHabitsOnce()
                container.reminderScheduler.rescheduleAll(habits)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
