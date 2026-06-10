package com.worldbarometer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "barometer_cache")

/**
 * Lokalny cache ostatniego znanego wyniku (DataStore). Zasila tryb offline:
 * gdy brak sieci pokazujemy ostatni snapshot + znacznik „nieaktualne".
 * Jedno źródło prawdy dla aplikacji i widgetu.
 */
class BarometerStore(context: Context) {

    private val dataStore = context.applicationContext.cacheDataStore

    data class CachedSnapshot(val rawJson: String, val fetchedAtMillis: Long)

    val snapshot: Flow<CachedSnapshot?> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_RAW_JSON] ?: return@map null
        CachedSnapshot(raw, prefs[KEY_FETCHED_AT] ?: 0L)
    }

    suspend fun save(rawJson: String, fetchedAtMillis: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_RAW_JSON] = rawJson
            prefs[KEY_FETCHED_AT] = fetchedAtMillis
        }
    }

    private companion object {
        val KEY_RAW_JSON = stringPreferencesKey("raw_json")
        val KEY_FETCHED_AT = longPreferencesKey("fetched_at_millis")
    }
}
