package com.momentum.app

import android.app.Application
import com.momentum.app.reminder.MaintenanceWorker
import com.momentum.app.reminder.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MomentumApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)

        applicationScope.launch { container.reminderScheduler.scheduleMidnightRollover() }
        MaintenanceWorker.enqueuePeriodic(this)
    }
}
