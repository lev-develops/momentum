package com.momentum.app

import android.content.Context
import com.momentum.app.data.db.AppDatabase
import com.momentum.app.data.datastore.AppPrefsDataStore
import com.momentum.app.data.datastore.WidgetPrefsDataStore
import com.momentum.app.data.export.DataExportManager
import com.momentum.app.data.repository.HabitRepository
import com.momentum.app.data.repository.HabitRepositoryImpl
import com.momentum.app.reminder.ReminderScheduler
import java.time.Clock

/** Hand-rolled DI container — the app is a single module with no Hilt/Dagger dependency. */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()

    private val database: AppDatabase = AppDatabase.getInstance(context)

    val habitRepository: HabitRepository = HabitRepositoryImpl(
        habitDao = database.habitDao(),
        completionDao = database.completionDao(),
        clock = clock,
    )

    val widgetPrefsDataStore = WidgetPrefsDataStore(context)

    val appPrefsDataStore = AppPrefsDataStore(context)

    val dataExportManager = DataExportManager(
        repository = habitRepository,
        contentResolver = context.contentResolver,
    )

    val reminderScheduler = ReminderScheduler(context)

    suspend fun refreshWidgets() {
        com.momentum.app.widget.MomentumWidget.refreshAll(appContext)
    }
}
