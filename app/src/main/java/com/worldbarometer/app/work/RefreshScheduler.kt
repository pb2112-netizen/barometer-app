package com.worldbarometer.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Planowanie odświeżania (WB-045): cyklicznie ~co godzinę (flex 15 min), tylko sieć.
 * Plus expedited one-off: zmiana kraju, odblokowanie telefonu.
 *
 * Interwał ~60 min — dopasowany do backendu (silnik liczy ~raz na godzinę).
 */
object RefreshScheduler {

    private const val PERIODIC_WORK = "barometer_periodic_refresh"
    private const val LENS_CHANGE_WORK = "barometer_lens_change_refresh"
    private const val UNLOCK_WORK = "barometer_unlock_refresh"

    private const val REFRESH_INTERVAL_MINUTES = 60L
    private const val FLEX_INTERVAL_MINUTES = 15L

    private val periodicConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // Expedited dopuszcza tylko ograniczone constraints (sieć/storage).
    private val expeditedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Wywoływane przy starcie aplikacji i po BOOT_COMPLETED. UPDATE = podmień parametry
     * istniejącego planu bez gubienia harmonogramu.
     */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(
            REFRESH_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
            FLEX_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(periodicConstraints)
            .setInputData(
                workDataOf(RefreshWorker.KEY_TRIGGER_SOURCE to RefreshCoordinator.TriggerSource.PERIODIC.key),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Zmiana kraju (lens). Expedited one-off uruchamia render + refresh widgetu w procesie
     * WorkManagera, który PRZEŻYWA wyjście z aplikacji.
     * REPLACE: szybkie kolejne przełączenie kraju anuluje poprzednie zlecenie.
     */
    fun requestLensChangeRefresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setConstraints(expeditedConstraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    RefreshWorker.KEY_LENS_CHANGE to true,
                    RefreshWorker.KEY_TRIGGER_SOURCE to RefreshCoordinator.TriggerSource.LENS.key,
                ),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            LENS_CHANGE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Odblokowanie telefonu — backstop gdy periodic opóźniony przez Doze.
     * KEEP: nie kasuj periodycznego harmonogramu.
     */
    fun requestUnlockRefresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setConstraints(expeditedConstraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(RefreshWorker.KEY_TRIGGER_SOURCE to RefreshCoordinator.TriggerSource.UNLOCK.key),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNLOCK_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
