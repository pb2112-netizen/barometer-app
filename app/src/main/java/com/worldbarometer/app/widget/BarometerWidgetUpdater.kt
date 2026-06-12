package com.worldbarometer.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.runComposition
import com.worldbarometer.app.di.ServiceLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Odświeżanie widgetu Z POMINIĘCIEM menedżera sesji Glance.
 *
 * Dlaczego: `GlanceAppWidget.update()` przy ŻYWEJ sesji tylko zgłasza event do SessionWorkera,
 * a te eventy bywają gubione (znany problem biblioteki — aktualizacje w oknie życia sesji
 * ~45 s są ignorowane). Objaw: widget "losowo" nie reaguje na zmianę kraju, niezależnie
 * od urządzenia (S24 i emulator Pixel 7).
 *
 * Zamiast tego: `runComposition()` renderuje kompozycję (pełny `provideGlance`, świeże dane)
 * do RemoteViews, które wypychamy BEZPOŚREDNIO przez `AppWidgetManager.updateAppWidget()` —
 * deterministyczna ścieżka klasycznych widgetów, bez WorkManagera i sesji po drodze.
 * `update()` zostaje tylko jako fallback, gdy bezpośredni render zawiedzie.
 */
object BarometerWidgetUpdater {

    private const val TAG = "WB-Widget"

    /** Limit renderu jednej instancji; po nim fallback na klasyczne update(). */
    private const val RENDER_TIMEOUT_MS = 10_000L

    private val widget = BarometerWidget()

    /** runComposition nie wspiera równoległych wywołań dla tego samego ID (proces = ten sam). */
    private val renderMutex = Mutex()

    /** Renderuje i wypycha każdą instancję widgetu. Bezpieczne z UI i z workerów. */
    @OptIn(ExperimentalGlanceApi::class)
    suspend fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        ServiceLocator.ensureInitialized(appContext)

        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, BarometerWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) {
            Log.d(TAG, "requestUpdate: no widget instances")
            return
        }

        val glanceManager = GlanceAppWidgetManager(appContext)
        renderMutex.withLock {
            widgetIds.forEach { appWidgetId ->
                val pushed = runCatching {
                    val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                    val views = withTimeoutOrNull(RENDER_TIMEOUT_MS) {
                        widget.runComposition(appContext, glanceId).first()
                    }
                    if (views != null) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        Log.d(TAG, "requestUpdate: direct render OK (id=$appWidgetId)")
                        true
                    } else {
                        Log.w(TAG, "requestUpdate: render timeout (id=$appWidgetId)")
                        false
                    }
                }.getOrElse { e ->
                    Log.w(TAG, "requestUpdate: direct render failed (id=$appWidgetId)", e)
                    false
                }

                if (!pushed) {
                    runCatching {
                        widget.update(appContext, glanceManager.getGlanceIdBy(appWidgetId))
                        Log.d(TAG, "requestUpdate: session fallback used (id=$appWidgetId)")
                    }.onFailure { e ->
                        Log.e(TAG, "requestUpdate: fallback failed (id=$appWidgetId)", e)
                    }
                }
            }
        }
    }
}
