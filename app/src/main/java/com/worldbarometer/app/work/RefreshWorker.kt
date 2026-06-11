package com.worldbarometer.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.worldbarometer.app.data.local.SettingsStore
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import com.worldbarometer.app.widget.BarometerWidgetUpdater

/**
 * Jedyne miejsce, które pobiera dane w tle (SPEC_MVP §2: brak ciągłej pracy/wakelocków).
 * Jedno pobranie zasila aplikację, widget i decyzję o powiadomieniu.
 */
class RefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = ServiceLocator.ensureInitialized(applicationContext)
        val settings = ServiceLocator.settingsStore

        return when (val result = repository.refresh()) {
            is BarometerRepository.RefreshResult.Success -> {
                BarometerWidgetUpdater.requestUpdate(applicationContext)
                evaluateNotification(settings, result.snapshot.data.globalScore, result.snapshot)
                Result.success()
            }
            // Błąd sieci → ponów z wykładniczym backoffem (SPEC_MVP §2).
            is BarometerRepository.RefreshResult.Failure -> Result.retry()
        }
    }

    /**
     * Reguła (SPEC_MVP §4): score >= próg ORAZ wzrost względem poprzedniego odczytu
     * ORAZ minęło >= 3 h od ostatniego powiadomienia. Pierwszy odczyt = brak alertu.
     */
    private suspend fun evaluateNotification(
        settings: SettingsStore,
        score: Double,
        snapshot: BarometerRepository.Snapshot,
    ) {
        val config = settings.currentSettings()
        val previousScore = settings.lastSeenScore()
        val lastNotification = settings.lastNotificationMillis()
        val now = System.currentTimeMillis()

        val hasPrevious = previousScore >= 0.0
        val isRising = hasPrevious && score > previousScore
        val cooldownPassed = now - lastNotification >= NOTIFICATION_COOLDOWN_MS

        if (config.notificationsEnabled && score >= config.threshold && isRising && cooldownPassed) {
            val sent = Notifier(applicationContext).notifyAlert(
                score = score,
                levelLabel = snapshot.level.label,
                summary = snapshot.data.shortSummary,
            )
            if (sent) settings.recordNotification(now)
        }

        settings.recordSeenScore(score)
    }

    companion object {
        /** Limit częstotliwości powiadomień: 3 h. */
        const val NOTIFICATION_COOLDOWN_MS = 3L * 60L * 60L * 1000L
    }
}
