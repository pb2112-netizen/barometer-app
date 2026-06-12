package com.worldbarometer.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.worldbarometer.app.di.ServiceLocator

/**
 * Odświeżanie widgetu: `update()` każdej instancji. Treść widget czyta SAM, reaktywnie,
 * wewnątrz kompozycji (patrz `BarometerWidget.provideGlance`) — update() służy tylko do
 * restartu martwej sesji Glance (żywa sesja przerysowuje się sama przy zmianie DataStore).
 * Wołane z procesu, który ma przeżyć render — w tle z `RefreshWorker`, nie z krótkiej korutyny UI.
 */
object BarometerWidgetUpdater {

    private val widget = BarometerWidget()

    /** Odświeża każdą instancję widgetu. */
    suspend fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        ServiceLocator.ensureInitialized(appContext)

        val manager = GlanceAppWidgetManager(appContext)
        manager.getGlanceIds(BarometerWidget::class.java).forEach { glanceId ->
            widget.update(appContext, glanceId)
        }
    }
}
