package com.momentum.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appPrefsDataStore by preferencesDataStore(name = "app_prefs")

/** Small general app-level prefs; currently just a self-healing check for local-midnight rollover. */
class AppPrefsDataStore(private val context: Context) {

    private val lastKnownDateEpochDayKey = longPreferencesKey("last_known_date_epoch_day")

    suspend fun getLastKnownDateEpochDay(): Long? =
        context.appPrefsDataStore.data.first()[lastKnownDateEpochDayKey]

    suspend fun setLastKnownDateEpochDay(epochDay: Long) {
        context.appPrefsDataStore.edit { it[lastKnownDateEpochDayKey] = epochDay }
    }
}
