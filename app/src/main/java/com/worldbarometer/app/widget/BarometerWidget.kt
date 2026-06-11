package com.worldbarometer.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
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
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.core.Trend
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import java.util.Locale

/** Wysokość cyfry score — ikona trendu ma ten sam rozmiar wizualny. */
private val ScoreFontSize = 34.sp
private val TrendIconSize = 34.dp

/**
 * Widget pulpitu (Glance). Tło = gradient poziomu, tekst biały.
 * Ikona trendu (↑/↓/→) w prawym górnym rogu. Tap = otwarcie aplikacji.
 */
class BarometerWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ServiceLocator.ensureInitialized(context)
        val glanceState = getAppWidgetState<Preferences>(context, id)
        val lensId = glanceState[BarometerWidgetStateKeys.LENS_ID]
            ?: ServiceLocator.settingsStore.currentLensId()
        val countryName = glanceState[BarometerWidgetStateKeys.COUNTRY_NAME]
            ?: LensCatalog.nameFor(lensId)
        val snapshot = repository.currentSnapshot()
        provideContent {
            WidgetContent(snapshot = snapshot, countryName = countryName)
        }
    }
}

@Composable
private fun WidgetContent(
    snapshot: BarometerRepository.Snapshot?,
    countryName: String,
) {
    val level = snapshot?.level ?: Level.STABLE
    val scoreText = snapshot?.let { String.format(Locale.US, "%.1f", it.data.globalScore) } ?: "—"
    val summary = snapshot?.data?.shortSummary.orEmpty()
    val context = LocalContext.current
    val updatedText = snapshot?.let { "Updated ${RelativeTime.format(it.data.updatedAt)}" } ?: ""
    val white = ColorProvider(Color.White)
    val widgetDescription = buildWidgetContentDescription(
        snapshot = snapshot,
        scoreText = scoreText,
        levelLabel = level.label,
        countryName = countryName,
        updatedText = updatedText,
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(backgroundFor(level)))
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
    snapshot: BarometerRepository.Snapshot?,
    scoreText: String,
    levelLabel: String,
    countryName: String,
    updatedText: String,
): String {
    if (snapshot == null) return "World Barometer"
    val updatedPart = updatedText.removePrefix("Updated ").lowercase(Locale.US)
    return "World Barometer $scoreText ${levelLabel.lowercase(Locale.US)}, for $countryName, updated $updatedPart"
}

private fun backgroundFor(level: Level): Int = when (level) {
    Level.STABLE -> R.drawable.widget_bg_stable
    Level.LOW -> R.drawable.widget_bg_low
    Level.ELEVATED -> R.drawable.widget_bg_elevated
    Level.HIGH -> R.drawable.widget_bg_high
    Level.CRITICAL -> R.drawable.widget_bg_critical
}
