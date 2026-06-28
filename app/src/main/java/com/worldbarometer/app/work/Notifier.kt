package com.worldbarometer.app.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.worldbarometer.app.MainActivity
import com.worldbarometer.app.R

/**
 * Lokalne powiadomienia (na telefonie). Aplikacja nie wysyła pushy z serwera —
 * decyzja zapada lokalnie w RefreshWorker. Kanał o wysokim priorytecie dla alertów.
 */
class Notifier(context: Context) {

    private val appContext = context.applicationContext

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
            val alerts = NotificationChannel(
                CHANNEL_ID,
                "Barometer alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications when the threat level rises above your threshold."
            }
            // Kanał MIN dla krótkiej pracy w tle (expedited WorkManager na API < 31
            // wymaga notyfikacji foreground; ma być cicha i nienachalna).
            val updates = NotificationChannel(
                UPDATES_CHANNEL_ID,
                "Background updates",
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Brief, silent status while the widget refreshes in the background."
            }
            manager.createNotificationChannel(alerts)
            manager.createNotificationChannel(updates)
        }
    }

    /**
     * Cicha notyfikacja foreground dla expedited WorkManagera (wymagana na API < 31).
     * Na Androidzie 12+ expedited działa bez usługi foreground i ta notyfikacja się nie pokazuje.
     */
    fun buildUpdatingNotification(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(appContext, UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_barometer)
            .setContentTitle("Updating…")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    /** Zwraca true, jeśli powiadomienie zostało wysłane. */
    fun notifyAlert(score: Double, levelLabel: String, summary: String): Boolean {
        if (!hasPermission()) return false

        val openIntent = android.app.PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val title = appContext.getString(R.string.notification_alert_title_format, levelLabel, score)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_barometer)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "barometer_alerts"
        const val UPDATES_CHANNEL_ID = "barometer_updates"

        /** Id notyfikacji foreground dla expedited WorkManagera (osobne od alertu). */
        const val FOREGROUND_NOTIFICATION_ID = 1002
        private const val NOTIFICATION_ID = 1001
    }
}
