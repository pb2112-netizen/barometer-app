package com.worldbarometer.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Planowanie odświeżania (SPEC_MVP §2): cyklicznie co 15 min, tylko gdy jest sieć
 * i bateria nie jest niska. Plus jednorazowe (one-off) dla tapu w widget / pull-to-refresh.
 */
object RefreshScheduler {

    private const val PERIODIC_WORK = "barometer_periodic_refresh"
    private const val ONE_OFF_WORK = "barometer_one_off_refresh"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /** Wywoływane raz przy starcie aplikacji. KEEP = nie nadpisuj istniejącego planu. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Jednorazowe odświeżenie na żądanie (widget/tap). KEEP chroni przed spamem. */
    fun requestOneOff(context: Context) {
        val request = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_OFF_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
