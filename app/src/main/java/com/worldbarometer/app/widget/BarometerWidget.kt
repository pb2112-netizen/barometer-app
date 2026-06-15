package com.worldbarometer.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.worldbarometer.app.MainActivity
import com.worldbarometer.app.R
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.core.Level
import com.worldbarometer.app.core.LevelPalette
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.core.SignificantMarkerBitmap
import com.worldbarometer.app.core.EventsAnchor
import com.worldbarometer.app.core.resolveVisibleEventsAnchor
import com.worldbarometer.app.core.Sparkline
import com.worldbarometer.app.core.SparklineBitmap
import com.worldbarometer.app.core.Tone
import com.worldbarometer.app.core.hoursAgo
import com.worldbarometer.app.data.model.ScoreHistoryPoint
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import kotlinx.coroutines.flow.scan
import java.util.Locale

private val ScoreFontSize = 34.sp
private val WidgetPadding = 14.dp
private val LabelReservedWidth = 88.dp
private val TopRowGap = 8.dp
private val SparklineMinWidth = 56.dp
private val SparklineHeight = 44.dp
private const val WIDGET_SPARKLINE_WIDTH_SCALE = 1.8f
private const val WIDGET_SPARKLINE_SIZE_SCALE = 1.1f
private val WidgetSparklineEndPadding = 6.dp
private val WidgetMarkerDotSize = 10.dp

/**
 * Widget pulpitu (Glance). Tło = gradient poziomu, tekst biały.
 * WB-029/WB-030: etykieta TL, sparkline TR (dynamiczna szerokość), marker kotwicy wydarzeń na wykresie.
 */
class BarometerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ServiceLocator.ensureInitialized(context)
        val settings = ServiceLocator.settingsStore
        val initialLensId = settings.currentLensId()
        val initialSnapshot = repository.currentSnapshot()
        provideContent {
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
    val level = snapshot?.level ?: Level.STABLE
    val tone = snapshot?.tone ?: Tone.NEUTRAL
    val scoreText = snapshot?.let { String.format(Locale.US, "%.1f", it.data.globalScore) } ?: "—"
    val summary = snapshot?.data?.shortSummary.orEmpty()
    val context = LocalContext.current
    val levelLabel = context.getString(LevelPalette.labelRes(level, tone))
    val updatedText = snapshot?.let { "Updated ${RelativeTime.formatAbsolute(it.data.updatedAt)}" } ?: ""
    val white = ColorProvider(Color.White)

    val history = snapshot?.data?.scoreHistory.orEmpty()
    val eventsAnchor = snapshot?.let {
        resolveVisibleEventsAnchor(
            history = history,
            eventsAnchorAt = it.data.eventsAnchorAt,
            topEvents = it.data.topEvents,
            shortSummary = summary,
        )
    }
    val showEventHeader = eventsAnchor != null

    val widgetSize = LocalSize.current
    val contentWidth = widgetSize.width - WidgetPadding * 2
    val baseSparklineWidth = (contentWidth - LabelReservedWidth - TopRowGap).coerceAtLeast(SparklineMinWidth)
    val sparklineWidth = (baseSparklineWidth * WIDGET_SPARKLINE_WIDTH_SCALE * WIDGET_SPARKLINE_SIZE_SCALE)
        .coerceAtMost(contentWidth * 0.92f)
        .coerceAtLeast(SparklineMinWidth)
    val sparklineHeight = SparklineHeight * WIDGET_SPARKLINE_SIZE_SCALE
    val sparklineWidthPx = SparklineBitmap.dpToPx(context, sparklineWidth)
    val sparklineHeightPx = SparklineBitmap.dpToPx(context, sparklineHeight)

    val widgetDescription = buildWidgetContentDescription(
        context = context,
        snapshot = snapshot,
        scoreText = scoreText,
        levelLabel = levelLabel,
        level = level,
        tone = tone,
        countryName = countryName,
        updatedText = updatedText,
        eventsAnchor = eventsAnchor,
        updatedAt = snapshot?.data?.updatedAt,
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(backgroundFor(level, tone)))
            .semantics { contentDescription = widgetDescription }
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(WidgetPadding),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            TopLabelSparklineRow(
                levelLabel = levelLabel,
                snapshot = snapshot,
                history = history,
                updatedAt = snapshot?.data?.updatedAt,
                sparklineWidth = sparklineWidth,
                sparklineHeight = sparklineHeight,
                sparklineWidthPx = sparklineWidthPx,
                sparklineHeightPx = sparklineHeightPx,
                peakIndex = eventsAnchor?.historyIndex,
            )

            Spacer(GlanceModifier.defaultWeight())

            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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

                if (showEventHeader) {
                    Spacer(GlanceModifier.height(4.dp))
                    Box(
                        modifier = GlanceModifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val markerBitmap = remember {
                                SignificantMarkerBitmap.render(context)
                            }
                            Image(
                                provider = ImageProvider(markerBitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.size(WidgetMarkerDotSize),
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            Text(
                                text = context.getString(R.string.last_significant_event),
                                style = TextStyle(color = white, fontSize = 10.sp),
                                maxLines = 1,
                            )
                        }
                    }
                }

                if (summary.isNotBlank()) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = summary,
                        style = TextStyle(color = white, fontSize = 11.sp, textAlign = TextAlign.Center),
                        modifier = GlanceModifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }
            }

            Spacer(GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (countryName.isNotBlank()) {
                    CountryBadgeGlance(countryName = countryName)
                } else {
                    Spacer(GlanceModifier.width(1.dp))
                }
                Spacer(GlanceModifier.defaultWeight())
                if (updatedText.isNotBlank()) {
                    Text(
                        text = updatedText,
                        style = TextStyle(color = white, fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopLabelSparklineRow(
    levelLabel: String,
    snapshot: BarometerRepository.Snapshot?,
    history: List<ScoreHistoryPoint>,
    updatedAt: String?,
    sparklineWidth: Dp,
    sparklineHeight: Dp,
    sparklineWidthPx: Int,
    sparklineHeightPx: Int,
    peakIndex: Int?,
) {
    val context = LocalContext.current
    val white = ColorProvider(Color.White)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = levelLabel,
            style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.width(LabelReservedWidth),
            maxLines = 1,
        )
        Spacer(GlanceModifier.width(TopRowGap))
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (snapshot != null && history.isNotEmpty()) {
                val sparklineBitmap = remember(
                    history,
                    updatedAt,
                    sparklineWidthPx,
                    sparklineHeightPx,
                    peakIndex,
                ) {
                    SparklineBitmap.render(
                        context = context,
                        history = history,
                        updatedAt = updatedAt,
                        widthPx = sparklineWidthPx,
                        heightPx = sparklineHeightPx,
                        peakIndex = peakIndex,
                    )
                }
                Image(
                    provider = ImageProvider(sparklineBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(sparklineWidth, sparklineHeight)
                        .padding(end = WidgetSparklineEndPadding),
                )
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

private fun buildWidgetContentDescription(
    context: Context,
    snapshot: BarometerRepository.Snapshot?,
    scoreText: String,
    levelLabel: String,
    level: Level,
    tone: Tone,
    countryName: String,
    updatedText: String,
    eventsAnchor: EventsAnchor?,
    updatedAt: String?,
): String {
    if (snapshot == null) return "World Barometer"
    val updatedPart = updatedText.removePrefix("Updated ").lowercase(Locale.US)
    val tonePart = if (level.isCalmBand) {
        ""
    } else {
        " — ${context.getString(LevelPalette.tonePhraseRes(tone))}"
    }
    val anchorPart = eventsAnchor?.let { anchor ->
        val windowEnd = Sparkline.windowEnd(snapshot.data.scoreHistory, updatedAt)
        val hours = hoursAgo(anchor.timestamp, windowEnd)
        val anchorScore = String.format(Locale.US, "%.1f", anchor.score)
        context.getString(R.string.significant_peak_description, hours, anchorScore)
    }
    return buildString {
        append(
            "World Barometer $scoreText ${levelLabel.lowercase(Locale.US)}$tonePart, " +
                "trend ${snapshot.trend.name.lowercase(Locale.US)} over the last 48 hours, " +
                "for $countryName, updated $updatedPart",
        )
        if (anchorPart != null) append(" $anchorPart")
    }
}

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
