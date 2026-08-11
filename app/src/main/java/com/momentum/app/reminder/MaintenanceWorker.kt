package com.momentum.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.momentum.app.MomentumApplication
import com.momentum.app.widget.MomentumWidget
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * WorkManager backstop that runs roughly every 15 minutes (its minimum period). AlarmManager
 * drives the actual midnight rollover and reminders, but exact alarms can occasionally be
 * dropped by OEM battery managers; this worker notices a missed date change and catches the
 * widget up, and re-arms any alarms that went missing.
 */
class MaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MomentumApplication).container
        val today = LocalDate.now(container.clock)
        val lastKnown = container.appPrefsDataStore.getLastKnownDateEpochDay()

        if (lastKnown == null || lastKnown != today.toEpochDay()) {
            MomentumWidget.refreshAll(applicationContext)
            container.appPrefsDataStore.setLastKnownDateEpochDay(today.toEpochDay())
        }

        val habits = container.habitRepository.getAllHabitsOnce()
        container.reminderScheduler.rescheduleAll(habits)

        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "momentum_maintenance"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
