package com.momentum.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.appPrefsDataStore by preferencesDataStore(name = "app_prefs")

/** Small general app-level prefs; a self-healing check for local-midnight rollover, plus whether
 * the user has already been through the sign-in-or-skip welcome gate. */
class AppPrefsDataStore(private val context: Context) {

    private val lastKnownDateEpochDayKey = longPreferencesKey("last_known_date_epoch_day")
    private val authGateCompletedKey = booleanPreferencesKey("auth_gate_completed")

    suspend fun getLastKnownDateEpochDay(): Long? =
        context.appPrefsDataStore.data.first()[lastKnownDateEpochDayKey]

    suspend fun setLastKnownDateEpochDay(epochDay: Long) {
        context.appPrefsDataStore.edit { it[lastKnownDateEpochDayKey] = epochDay }
    }

    suspend fun getAuthGateCompleted(): Boolean =
        context.appPrefsDataStore.data.first()[authGateCompletedKey] ?: false

    suspend fun setAuthGateCompleted(completed: Boolean) {
        context.appPrefsDataStore.edit { it[authGateCompletedKey] = completed }
    }
}
