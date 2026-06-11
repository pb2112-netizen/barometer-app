package com.worldbarometer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.worldbarometer.app.core.LensCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "barometer_cache")

/**
 * Lokalny cache wyniku per lens (DataStore). Zasila tryb offline i widget.
 */
class BarometerStore(context: Context) {

    private val dataStore = context.applicationContext.cacheDataStore

    data class CachedSnapshot(val rawJson: String, val fetchedAtMillis: Long)

    fun snapshot(lensId: String): Flow<CachedSnapshot?> = dataStore.data.map { prefs ->
        val safeLens = LensCatalog.sanitize(lensId)
        val raw = prefs[rawKey(safeLens)]
            ?: prefs[LEGACY_RAW_JSON].takeIf { safeLens == LensCatalog.DEFAULT_LENS_ID }
            ?: return@map null
        CachedSnapshot(raw, prefs[fetchedKey(safeLens)] ?: prefs[LEGACY_FETCHED_AT] ?: 0L)
    }

    suspend fun save(lensId: String, rawJson: String, fetchedAtMillis: Long) {
        val safeLens = LensCatalog.sanitize(lensId)
        dataStore.edit { prefs ->
            prefs[rawKey(safeLens)] = rawJson
            prefs[fetchedKey(safeLens)] = fetchedAtMillis
        }
    }

    private companion object {
        val LEGACY_RAW_JSON = stringPreferencesKey("raw_json")
        val LEGACY_FETCHED_AT = longPreferencesKey("fetched_at_millis")

        fun rawKey(lensId: String) = stringPreferencesKey("raw_json_$lensId")
        fun fetchedKey(lensId: String) = longPreferencesKey("fetched_at_$lensId")
    }
}
