package com.worldbarometer.app.work

import android.Manifest
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
import java.util.Locale

/**
 * Lokalne powiadomienia (na telefonie). Aplikacja nie wysyła pushy z serwera —
 * decyzja zapada lokalnie w RefreshWorker. Kanał o wysokim priorytecie dla alertów.
 */
class Notifier(context: Context) {

    private val appContext = context.applicationContext

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Barometer alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications when the threat level rises above your threshold."
            }
            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
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

        val title = String.format(Locale.US, "World Barometer: %s %.1f", levelLabel, score)
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
        private const val NOTIFICATION_ID = 1001
    }
}
