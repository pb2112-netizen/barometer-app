package com.worldbarometer.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.di.ServiceLocator
/**
 * Odświeżanie widgetu: zapis aktywnego lensu/kraju do stanu Glance + `update()` każdej instancji.
 * Wołane z procesu, który ma przeżyć render — w tle z `RefreshWorker` (też przy zmianie kraju,
 * patrz `RefreshScheduler.requestLensChangeRefresh`), nie z krótkiej korutyny UI.
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
}
