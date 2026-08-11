package com.momentum.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "widget_prefs")

/**
 * Persists which habit each home-screen widget instance is bound to, keyed by appWidgetId, so
 * multiple widget instances can each hold an independent selection.
 */
class WidgetPrefsDataStore(private val context: Context) {

    private fun keyFor(appWidgetId: Int) = longPreferencesKey("widget_habit_$appWidgetId")

    fun observeHabitId(appWidgetId: Int): Flow<Long?> =
        context.widgetDataStore.data.map { prefs -> prefs[keyFor(appWidgetId)] }

    suspend fun getHabitId(appWidgetId: Int): Long? = observeHabitId(appWidgetId).first()

    suspend fun setHabitId(appWidgetId: Int, habitId: Long) {
        context.widgetDataStore.edit { prefs -> prefs[keyFor(appWidgetId)] = habitId }
    }

    suspend fun clear(appWidgetId: Int) {
        context.widgetDataStore.edit { prefs -> prefs.remove(keyFor(appWidgetId)) }
    }
}
