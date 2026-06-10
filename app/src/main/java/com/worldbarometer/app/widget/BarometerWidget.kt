package com.worldbarometer.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.worldbarometer.app.MainActivity
import com.worldbarometer.app.R
import com.worldbarometer.app.core.Level
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Widget pulpitu (Glance). Tło = gradient poziomu (2 stopnie), tekst biały, bez ikon.
 * Pokazuje: ocenę „X/10" (10 jak potęga), etykietę poziomu, krótki komentarz oraz
 * czas ostatniej aktualizacji. Czyta ten sam cache co aplikacja (jedno źródło prawdy).
 * Tap = otwarcie aplikacji (głównego ekranu).
 */
class BarometerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ServiceLocator.ensureInitialized(context)
        val snapshot = repository.observe().first()
        provideContent {
            WidgetContent(snapshot)
        }
    }
}

@Composable
private fun WidgetContent(snapshot: BarometerRepository.Snapshot?) {
    val level = snapshot?.level ?: Level.STABLE
    val scoreText = snapshot?.let { String.format(Locale.US, "%.1f", it.data.globalScore) } ?: "—"
    val summary = snapshot?.data?.shortSummary.orEmpty()
    val updatedText = snapshot?.let { "Updated ${RelativeTime.format(it.data.updatedAt)}" } ?: ""
    val white = ColorProvider(Color.White)
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(backgroundFor(level)))
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Ocena: duża liczba + małe, podniesione "/10" (efekt potęgi).
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = scoreText,
                style = TextStyle(color = white, fontSize = 34.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = "/10",
                style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
        }

        Text(
            text = level.label,
            style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        )

        if (summary.isNotBlank()) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = summary,
                style = TextStyle(color = white, fontSize = 11.sp),
                maxLines = 2,
            )
        }

        if (updatedText.isNotBlank()) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = updatedText,
                style = TextStyle(color = white, fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

private fun backgroundFor(level: Level): Int = when (level) {
    Level.STABLE -> R.drawable.widget_bg_stable
    Level.LOW -> R.drawable.widget_bg_low
    Level.ELEVATED -> R.drawable.widget_bg_elevated
    Level.HIGH -> R.drawable.widget_bg_high
    Level.CRITICAL -> R.drawable.widget_bg_critical
}
