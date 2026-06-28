package com.worldbarometer.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Backstop po odblokowaniu: expedited fetch gdy periodic opóźniony przez Doze (WB-045).
 * Debounce 45 min; wyjątek gdy dane stale (>90 min).
 */
class UserPresentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_USER_PRESENT) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val appContext = context.applicationContext
                ServiceLocator.ensureInitialized(appContext)
                val settings = ServiceLocator.settingsStore
                val lastFetch = settings.lastSuccessfulFetchMillis()
                val now = System.currentTimeMillis()
                val elapsed = now - lastFetch
                val isStale = lastFetch <= 0L || elapsed > BarometerRepository.STALE_AFTER_MILLIS

                if (!isStale && elapsed < UNLOCK_DEBOUNCE_MS) {
                    Log.d(TAG, "unlock: skipped debounce (${elapsed / 60_000} min since last fetch)")
                    return@launch
                }

                Log.d(TAG, "unlock: requesting refresh (stale=$isStale)")
                RefreshScheduler.requestUnlockRefresh(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WB-Widget"
        private const val UNLOCK_DEBOUNCE_MS = 45L * 60L * 1000L

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
