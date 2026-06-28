package com.worldbarometer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.worldbarometer.app.core.LensCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Ustawienia użytkownika + stan potrzebny do decyzji o powiadomieniu.
 */
class SettingsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    data class Settings(
        val notificationsEnabled: Boolean = DEFAULT_NOTIFICATIONS_ENABLED,
        val threshold: Double = DEFAULT_THRESHOLD,
        val lensId: String = LensCatalog.DEFAULT_LENS_ID,
    )

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            notificationsEnabled = prefs[KEY_ENABLED] ?: DEFAULT_NOTIFICATIONS_ENABLED,
            threshold = prefs[KEY_THRESHOLD] ?: DEFAULT_THRESHOLD,
            lensId = LensCatalog.sanitize(prefs[KEY_LENS_ID] ?: LensCatalog.DEFAULT_LENS_ID),
        )
    }

    val lensId: Flow<String> = settings.map { it.lensId }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setThreshold(value: Double) {
        dataStore.edit { it[KEY_THRESHOLD] = value.coerceIn(1.0, 10.0) }
    }

    suspend fun setLensId(value: String) {
        dataStore.edit { it[KEY_LENS_ID] = LensCatalog.sanitize(value) }
    }

    suspend fun currentSettings(): Settings = settings.first()

    suspend fun currentLensId(): String = currentSettings().lensId

    suspend fun lastNotificationMillis(): Long =
        dataStore.data.first()[KEY_LAST_NOTIFICATION] ?: 0L

    suspend fun lastSeenScore(): Double =
        dataStore.data.first()[KEY_LAST_SEEN_SCORE] ?: NO_PREVIOUS_SCORE

    suspend fun recordNotification(millis: Long) {
        dataStore.edit { it[KEY_LAST_NOTIFICATION] = millis }
    }

    suspend fun recordSeenScore(score: Double) {
        dataStore.edit { it[KEY_LAST_SEEN_SCORE] = score }
    }

    suspend fun lastSuccessfulFetchMillis(): Long =
        dataStore.data.first()[KEY_LAST_SUCCESSFUL_FETCH] ?: 0L

    suspend fun recordLastSuccessfulFetchMillis(millis: Long) {
        dataStore.edit { it[KEY_LAST_SUCCESSFUL_FETCH] = millis }
    }

    companion object {
        const val DEFAULT_THRESHOLD = 5.0
        const val DEFAULT_NOTIFICATIONS_ENABLED = true
        const val NO_PREVIOUS_SCORE = -1.0

        private val KEY_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_THRESHOLD = doublePreferencesKey("notification_threshold")
        private val KEY_LENS_ID = stringPreferencesKey("lens_id")
        private val KEY_LAST_NOTIFICATION = longPreferencesKey("last_notification_millis")
        private val KEY_LAST_SEEN_SCORE = doublePreferencesKey("last_seen_score")
        private val KEY_LAST_SUCCESSFUL_FETCH = longPreferencesKey("last_successful_fetch_millis")
    }
}
