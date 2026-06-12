package com.worldbarometer.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.worldbarometer.app.MainActivity
import com.worldbarometer.app.R
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.core.Level
import com.worldbarometer.app.core.LevelPalette
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.core.Tone
import com.worldbarometer.app.core.Trend
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import kotlinx.coroutines.flow.scan
import java.util.Locale

/** Wysokość cyfry score — ikona trendu ma ten sam rozmiar wizualny. */
private val ScoreFontSize = 34.sp
private val TrendIconSize = 34.dp

/**
 * Widget pulpitu (Glance). Tło = gradient poziomu, tekst biały.
 * Ikona trendu (↑/↓/→) w prawym górnym rogu. Tap = otwarcie aplikacji.
 */
class BarometerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ServiceLocator.ensureInitialized(context)
        val settings = ServiceLocator.settingsStore
        // Wartości startowe — pierwsza klatka bez „pustego" widgetu.
        val initialLensId = settings.currentLensId()
        val initialSnapshot = repository.currentSnapshot()
        provideContent {
            // KLUCZOWE: dane czytane WEWNĄTRZ kompozycji (flow → collectAsState).
            // Żywa sesja Glance przy update() tylko REKOMPONUJE — wartości złapane przed
            // provideContent byłyby zamrożone do wygaśnięcia sesji (historyczna przyczyna
            // „martwego" widgetu po zmianie kraju). Dzięki flow każda zmiana cache/lensu
            // w DataStore sama przerysowuje żywą sesję; update() z workerów obsługuje
            // wyłącznie restart sesji martwej.
            // scan: zatrzymuje ostatni niepusty snapshot — przy zmianie kraju widget pokazuje
            // stary kraj+dane aż nowy cache się pojawi, potem przełącza ATOMOWO (bez pustej
            // klatki i bez etykiety nowego kraju nad danymi starego).
            val snapshot by repository.observe()
                .scan(initialSnapshot) { previous, next -> next ?: previous }
                .collectAsState(initial = initialSnapshot)
            val countryName = LensCatalog.nameFor(snapshot?.lensId ?: initialLensId)
            WidgetContent(snapshot = snapshot, countryName = countryName)
        }
    }
}

@Composable
private fun WidgetContent(
    snapshot: BarometerRepository.Snapshot?,
    countryName: String,
) {
    // Brak danych (stary cache / pierwsza klatka) -> Calm + NEUTRAL (WB-015 AC-1, T4).
    val level = snapshot?.level ?: Level.STABLE
    val tone = snapshot?.tone ?: Tone.NEUTRAL
    val scoreText = snapshot?.let { String.format(Locale.US, "%.1f", it.data.globalScore) } ?: "—"
    val summary = snapshot?.data?.shortSummary.orEmpty()
    val context = LocalContext.current
    // Etykieta ze wspólnego słownika WB-014 (jedno źródło prawdy z dashboardem — AC-3).
    val levelLabel = context.getString(LevelPalette.labelRes(level, tone))
    val updatedText = snapshot?.let { "Updated ${RelativeTime.formatAbsolute(it.data.updatedAt)}" } ?: ""
    val white = ColorProvider(Color.White)
    val widgetDescription = buildWidgetContentDescription(
        context = context,
        snapshot = snapshot,
        scoreText = scoreText,
        levelLabel = levelLabel,
        level = level,
        tone = tone,
        countryName = countryName,
        updatedText = updatedText,
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(backgroundFor(level, tone)))
            .semantics { contentDescription = widgetDescription }
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(14.dp),
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = scoreText,
                        style = TextStyle(color = white, fontSize = ScoreFontSize, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "/10",
                        style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    )
                }

                Text(
                    text = levelLabel,
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
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = updatedText,
                        style = TextStyle(color = white, fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
            }
        }

        if (countryName.isNotBlank()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart,
            ) {
                CountryBadgeGlance(countryName = countryName)
            }
        }

        if (snapshot != null) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd,
            ) {
                TrendIconGlance(trend = snapshot.trend)
            }
        }
    }
}

@Composable
private fun CountryBadgeGlance(countryName: String) {
    val white = ColorProvider(Color.White)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ic_location_pin),
            contentDescription = null,
            modifier = GlanceModifier.size(12.dp),
            colorFilter = ColorFilter.tint(white),
        )
        Spacer(GlanceModifier.size(4.dp))
        Text(
            text = countryName,
            style = TextStyle(color = white, fontSize = 10.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
private fun TrendIconGlance(trend: Trend) {
    val iconRes = when (trend) {
        Trend.RISING -> R.drawable.ic_trend_rising
        Trend.FALLING -> R.drawable.ic_trend_falling
        Trend.STABLE -> R.drawable.ic_trend_stable
    }
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = "Trend: ${trend.name.lowercase()}",
        modifier = GlanceModifier.size(TrendIconSize),
        colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
    )
}

private fun buildWidgetContentDescription(
    context: Context,
    snapshot: BarometerRepository.Snapshot?,
    scoreText: String,
    levelLabel: String,
    level: Level,
    tone: Tone,
    countryName: String,
    updatedText: String,
): String {
    if (snapshot == null) return "World Barometer"
    val updatedPart = updatedText.removePrefix("Updated ").lowercase(Locale.US)
    // Ton dokładany tylko w pasmach sygnału (score >= 5) — WB-015 §4.4, spójnie z WB-014 §4.6.
    val tonePart = if (level.isCalmBand) {
        ""
    } else {
        " — ${context.getString(LevelPalette.tonePhraseRes(tone))}"
    }
    return "World Barometer $scoreText ${levelLabel.lowercase(Locale.US)}$tonePart, " +
        "for $countryName, updated $updatedPart"
}

/**
 * Tło z pary (pasmo score x ton) — macierz WB-015 §4.1: 2 tła spokoju (bez tonowania)
 * + 3 pasma sygnału x 3 tony. Negative = dotychczasowe gradienty sygnałowe.
 */
private fun backgroundFor(level: Level, tone: Tone): Int = when (level) {
    Level.STABLE -> R.drawable.widget_bg_calm
    Level.LOW -> R.drawable.widget_bg_quiet
    Level.ELEVATED -> when (tone) {
        Tone.NEGATIVE -> R.drawable.widget_bg_mid_negative
        Tone.NEUTRAL -> R.drawable.widget_bg_mid_neutral
        Tone.POSITIVE -> R.drawable.widget_bg_mid_positive
    }
    Level.HIGH -> when (tone) {
        Tone.NEGATIVE -> R.drawable.widget_bg_high_negative
        Tone.NEUTRAL -> R.drawable.widget_bg_high_neutral
        Tone.POSITIVE -> R.drawable.widget_bg_high_positive
    }
    Level.CRITICAL -> when (tone) {
        Tone.NEGATIVE -> R.drawable.widget_bg_top_negative
        Tone.NEUTRAL -> R.drawable.widget_bg_top_neutral
        Tone.POSITIVE -> R.drawable.widget_bg_top_positive
    }
}
