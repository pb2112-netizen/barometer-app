package com.worldbarometer.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
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
        val lensChange = inputData.getBoolean(KEY_LENS_CHANGE, false)

        val result = repository.refresh()

        // update() PO pobraniu (cache jest już zapisany) — restartuje martwą sesję Glance,
        // która od razu czyta świeży cache. Żywa sesja i tak przerysowała się sama po zapisie
        // (treść reaktywna w provideGlance).
        when (result) {
            is BarometerRepository.RefreshResult.Success -> {
                BarometerWidgetUpdater.requestUpdate(applicationContext)
                evaluateNotification(settings, result.snapshot.data.globalScore, result.snapshot)
                return Result.success()
            }
            is BarometerRepository.RefreshResult.Failure -> {
                // Zmiana kraju offline: i tak odśwież widget (kraj + dane z cache, jeśli są) i nie ponawiaj.
                // Cykl periodyczny w zwykłym biegu: retry z backoffem (SPEC_MVP §2).
                return if (lensChange) {
                    BarometerWidgetUpdater.requestUpdate(applicationContext)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        }
    }

    /**
     * Wymagane przez expedited WorkManager na API < 31 (foreground service). Na Androidzie 12+
     * expedited nie używa usługi foreground, więc notyfikacja się nie pokazuje.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(
            Notifier.FOREGROUND_NOTIFICATION_ID,
            Notifier(applicationContext).buildUpdatingNotification(),
        )

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

        /** true → ścieżka zmiany kraju: po pobraniu zawsze odśwież widget i nie ponawiaj przy błędzie. */
        const val KEY_LENS_CHANGE = "lens_change"
    }
}
