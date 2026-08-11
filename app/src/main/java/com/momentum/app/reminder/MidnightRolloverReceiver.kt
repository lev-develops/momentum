package com.momentum.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.momentum.app.MomentumApplication
import com.momentum.app.widget.MomentumWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires once at local midnight so any placed widgets advance to the new day's grid column. */
class MidnightRolloverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as MomentumApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MomentumWidget.refreshAll(context)
                container.reminderScheduler.scheduleMidnightRollover()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
