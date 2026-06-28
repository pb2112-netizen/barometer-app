package com.worldbarometer.app.work

import android.content.Context
import android.util.Log
import com.worldbarometer.app.di.ServiceLocator
import com.worldbarometer.app.widget.BarometerWidgetUpdater

/**
 * Wspólna ścieżka po udanym fetchu: zapis czasu + przerysowanie widgetu + log źródła (WB-045).
 */
object RefreshCoordinator {

    private const val TAG = "WB-Widget"

    enum class TriggerSource(val key: String) {
        PERIODIC("periodic"),
        UNLOCK("unlock"),
        LENS("lens"),
        APP("app"),
        MANUAL("manual"),
        ;

        companion object {
            fun fromKey(key: String?): TriggerSource =
                entries.firstOrNull { it.key == key } ?: PERIODIC
        }
    }

    suspend fun onFetchSuccess(context: Context, source: TriggerSource) {
        val appContext = context.applicationContext
        ServiceLocator.ensureInitialized(appContext)
        ServiceLocator.settingsStore.recordLastSuccessfulFetchMillis(System.currentTimeMillis())
        Log.d(TAG, "onFetchSuccess source=${source.key}")
        BarometerWidgetUpdater.requestUpdate(appContext)
    }
}
