package com.worldbarometer.app.widget

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Klucze stanu Glance (PreferencesGlanceStateDefinition — domyślny w Glance 1.1). */
object BarometerWidgetStateKeys {
    val LENS_ID = stringPreferencesKey("lens_id")
    val COUNTRY_NAME = stringPreferencesKey("country_name")
    /** Monotoniczny token wymusza przebudowę po zmianie kraju / refresh. */
    val UPDATE_TOKEN = longPreferencesKey("update_token")
}
