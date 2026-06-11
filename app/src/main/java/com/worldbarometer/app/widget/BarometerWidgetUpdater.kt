package com.worldbarometer.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.di.ServiceLocator
/**
 * Odświeżanie widgetu. Glance uruchamia `provideGlance` przez WorkManager i może
 * zignorować kolejne `update()` wywołane w ciągu ~1–2 s — stąd opóźnione drugie odświeżenie.
 */
object BarometerWidgetUpdater {

    private val widget = BarometerWidget()

    /** Zapisuje aktywny lens w stanie Glance i odświeża każdą instancję widgetu. */
    suspend fun requestUpdate(context: Context, lensId: String? = null) {
        val appContext = context.applicationContext
        ServiceLocator.ensureInitialized(appContext)
        val activeLensId = lensId ?: ServiceLocator.settingsStore.currentLensId()
        val countryName = LensCatalog.nameFor(activeLensId)

        val manager = GlanceAppWidgetManager(appContext)
        val glanceIds = manager.getGlanceIds(BarometerWidget::class.java)
        if (glanceIds.isEmpty()) return

        val token = System.currentTimeMillis()
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(appContext, glanceId) { prefs ->
                prefs[BarometerWidgetStateKeys.LENS_ID] = activeLensId
                prefs[BarometerWidgetStateKeys.COUNTRY_NAME] = countryName
                prefs[BarometerWidgetStateKeys.UPDATE_TOKEN] = token
            }
            widget.update(appContext, glanceId)
        }
    }

    /** Opóźnienie przed ponownym `update()` — Glance często ignoruje wywołania w ciągu ~1–2 s. */
    const val GLANCE_SECOND_UPDATE_DELAY_MS = 2_500L
}
