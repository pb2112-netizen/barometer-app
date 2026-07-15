package com.worldbarometer.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.appwidget.SizeMode
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
import com.worldbarometer.app.core.ANCHOR_MARKER_RADIUS_DP
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.core.Level
import com.worldbarometer.app.core.LevelPalette
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.core.SignificantMarkerBitmap
import com.worldbarometer.app.core.SignificantMarkerColor
import com.worldbarometer.app.core.SparklineBitmap
import com.worldbarometer.app.core.Tone
import com.worldbarometer.app.data.model.MostSignificantEvent
import com.worldbarometer.app.data.model.ScoreHistoryPoint
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import kotlinx.coroutines.flow.scan
import java.time.Duration
import java.time.Instant
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

// WB-055: marker size driven by shared ANCHOR_MARKER_RADIUS_DP (parity with dashboard)
private val WidgetMarkerDotSize = (ANCHOR_MARKER_RADIUS_DP * 2 * 2.2f).dp   // dot + halo bounding box

// WB-055: SizeMode.Responsive breakpoints
private val SIZE_COMPACT = DpSize(130.dp, 100.dp)
private val SIZE_STANDARD = DpSize(200.dp, 100.dp)
private val SIZE_WIDE = DpSize(281.dp, 100.dp)

/**
 * Widget pulpitu (Glance). Tło = gradient poziomu, tekst biały.
 * WB-029/WB-030: etykieta TL, sparkline TR, marker kotwicy wydarzeń.
 * WB-055: SizeMode.Responsive (Compact/Standard/Wide), stale indicator, preview w pickerze.
 */
class BarometerWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_COMPACT, SIZE_STANDARD, SIZE_WIDE)
    )

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
    val context = LocalContext.current
    val levelLabel = context.getString(LevelPalette.labelRes(level, tone))
    val updatedText = snapshot?.let { "Updated ${RelativeTime.formatAbsolute(it.data.updatedAt)}" } ?: ""
    val white = ColorProvider(Color.White)

    val isStale = snapshot?.isStale(System.currentTimeMillis()) ?: false

    val history = snapshot?.data?.scoreHistory.orEmpty()
    // WB-060: "Most significant event" zastępuje eventsAnchor/shortSummary jako sticky nagłówek.
    val mse = snapshot?.data?.mostSignificantEvent
    val mseLabel = mse?.label ?: context.getString(R.string.mse_gathering_data)

    val topEventTitle = snapshot?.data?.topEvents?.firstOrNull()?.title.orEmpty()

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
        mse = mse,
        isStale = isStale,
    )

    // WB-055: dim overlay when stale (simulates alpha 0.75 on gradient)
    val staleOverlayColor = ColorProvider(Color(0x40000000))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(backgroundFor(level, tone)))
            .semantics { contentDescription = widgetDescription }
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(WidgetPadding),
    ) {
        // Stale dim overlay
        if (isStale) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(staleOverlayColor),
            ) {}
        }

        when {
            widgetSize.width < 200.dp -> CompactWidgetContent(
                scoreText = scoreText,
                levelLabel = levelLabel,
                updatedText = updatedText,
                countryName = countryName,
                isStale = isStale,
                white = white,
                context = context,
            )
            widgetSize.width > 280.dp -> WideWidgetContent(
                scoreText = scoreText,
                levelLabel = levelLabel,
                updatedText = updatedText,
                mse = mse,
                mseLabel = mseLabel,
                countryName = countryName,
                isStale = isStale,
                topEventTitle = topEventTitle,
                snapshot = snapshot,
                history = history,
                sparklineWidth = sparklineWidth,
                sparklineHeight = sparklineHeight,
                sparklineWidthPx = sparklineWidthPx,
                sparklineHeightPx = sparklineHeightPx,
                white = white,
                context = context,
            )
            else -> StandardWidgetContent(
                scoreText = scoreText,
                levelLabel = levelLabel,
                updatedText = updatedText,
                mse = mse,
                mseLabel = mseLabel,
                countryName = countryName,
                isStale = isStale,
                snapshot = snapshot,
                history = history,
                sparklineWidth = sparklineWidth,
                sparklineHeight = sparklineHeight,
                sparklineWidthPx = sparklineWidthPx,
                sparklineHeightPx = sparklineHeightPx,
                white = white,
                context = context,
            )
        }
    }
}

/** Wariant Compact: width < 200dp — score + level label + updated. Bez sparkline i summary. */
@Composable
private fun CompactWidgetContent(
    scoreText: String,
    levelLabel: String,
    updatedText: String,
    countryName: String,
    isStale: Boolean,
    white: ColorProvider,
    context: Context,
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = levelLabel,
            style = TextStyle(color = white, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )

        Spacer(GlanceModifier.defaultWeight())

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
            Column(horizontalAlignment = Alignment.End) {
                if (updatedText.isNotBlank()) {
                    Text(
                        text = updatedText,
                        style = TextStyle(color = white, fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
                if (isStale) {
                    Text(
                        text = context.getString(R.string.widget_stale_suffix),
                        style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)), fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Wariant Standard: 200–280dp — dotychczasowy pełny layout. */
@Composable
private fun StandardWidgetContent(
    scoreText: String,
    levelLabel: String,
    updatedText: String,
    mse: MostSignificantEvent?,
    mseLabel: String,
    countryName: String,
    isStale: Boolean,
    snapshot: BarometerRepository.Snapshot?,
    history: List<ScoreHistoryPoint>,
    sparklineWidth: Dp,
    sparklineHeight: Dp,
    sparklineWidthPx: Int,
    sparklineHeightPx: Int,
    white: ColorProvider,
    context: Context,
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

            // WB-060: blok "Most significant event" — ZAWSZE widoczny (marker kolorowany lub fallback).
            Spacer(GlanceModifier.height(4.dp))
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val markerColor = mse?.let {
                        LevelPalette.eventBadgeColor(it.score, Tone.fromString(it.sentiment))
                    } ?: SignificantMarkerColor
                    val markerBitmap = remember(markerColor) {
                        SignificantMarkerBitmap.render(context, color = markerColor)
                    }
                    Image(
                        provider = ImageProvider(markerBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.size(WidgetMarkerDotSize),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.most_significant_event),
                        style = TextStyle(color = white, fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
            }

            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = mseLabel,
                style = TextStyle(color = white, fontSize = 11.sp, textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 2,
            )
        }

        Spacer(GlanceModifier.defaultWeight())

        BottomRow(
            countryName = countryName,
            updatedText = updatedText,
            isStale = isStale,
            white = white,
            context = context,
        )
    }
}

/** Wariant Wide: > 280dp — Standard + tytuł pierwszego top eventu (1 linia). */
@Composable
private fun WideWidgetContent(
    scoreText: String,
    levelLabel: String,
    updatedText: String,
    mse: MostSignificantEvent?,
    mseLabel: String,
    countryName: String,
    isStale: Boolean,
    topEventTitle: String,
    snapshot: BarometerRepository.Snapshot?,
    history: List<ScoreHistoryPoint>,
    sparklineWidth: Dp,
    sparklineHeight: Dp,
    sparklineWidthPx: Int,
    sparklineHeightPx: Int,
    white: ColorProvider,
    context: Context,
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

            // WB-060: blok "Most significant event" — ZAWSZE widoczny (marker kolorowany lub fallback).
            Spacer(GlanceModifier.height(4.dp))
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val markerColor = mse?.let {
                        LevelPalette.eventBadgeColor(it.score, Tone.fromString(it.sentiment))
                    } ?: SignificantMarkerColor
                    val markerBitmap = remember(markerColor) {
                        SignificantMarkerBitmap.render(context, color = markerColor)
                    }
                    Image(
                        provider = ImageProvider(markerBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.size(WidgetMarkerDotSize),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.most_significant_event),
                        style = TextStyle(color = white, fontSize = 10.sp),
                        maxLines = 1,
                    )
                }
            }

            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = mseLabel,
                style = TextStyle(color = white, fontSize = 11.sp, textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 2,
            )

            // Wide-only: top event title
            if (topEventTitle.isNotBlank()) {
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = topEventTitle,
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.85f)),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                    maxLines = 1,
                )
            }
        }

        Spacer(GlanceModifier.defaultWeight())

        BottomRow(
            countryName = countryName,
            updatedText = updatedText,
            isStale = isStale,
            white = white,
            context = context,
        )
    }
}

@Composable
private fun BottomRow(
    countryName: String,
    updatedText: String,
    isStale: Boolean,
    white: ColorProvider,
    context: Context,
) {
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
        Column(horizontalAlignment = Alignment.End) {
            if (updatedText.isNotBlank()) {
                Text(
                    text = updatedText,
                    style = TextStyle(color = white, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
            // WB-055: stale indicator
            if (isStale) {
                Text(
                    text = context.getString(R.string.widget_stale_suffix),
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                        fontSize = 10.sp,
                    ),
                    maxLines = 1,
                )
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
                // WB-060: okno renderowania 24h — SparklineBitmap.render filtruje/wygładza wewnętrznie.
                val sparklineBitmap = remember(
                    history,
                    updatedAt,
                    sparklineWidthPx,
                    sparklineHeightPx,
                ) {
                    SparklineBitmap.render(
                        context = context,
                        history = history,
                        updatedAt = updatedAt,
                        widthPx = sparklineWidthPx,
                        heightPx = sparklineHeightPx,
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

/** WB-060: fraza "N hours"/"less than 1 hour" bez końcowego "ago" — używana w `mse_description` (ma "ago" w szablonie). */
private fun mseDetectedAgoPhrase(isoUtc: String?, nowMillis: Long = System.currentTimeMillis()): String {
    val instant = runCatching { Instant.parse(isoUtc.orEmpty()) }.getOrNull() ?: return "unknown"
    val now = Instant.ofEpochMilli(nowMillis)
    if (instant.isAfter(now)) return "less than 1 hour"
    val hours = Duration.between(instant, now).toHours()
    return when {
        hours < 1 -> "less than 1 hour"
        hours < 24 -> if (hours == 1L) "1 hour" else "$hours hours"
        else -> "more than a day"
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
    mse: MostSignificantEvent?,
    isStale: Boolean,
): String {
    if (snapshot == null) return context.getString(R.string.app_name)
    val updatedPart = updatedText.removePrefix("Updated ").lowercase(Locale.US)
    val tonePart = if (level.isCalmBand) {
        ""
    } else {
        " — ${context.getString(LevelPalette.tonePhraseRes(tone))}"
    }
    // WB-060: "Most significant event" zastępuje dawny anchor/peak a11y opis.
    val msePart = mse?.let {
        val agoPhrase = mseDetectedAgoPhrase(it.detectedAt)
        val mseScore = String.format(Locale.US, "%.1f", it.score)
        context.getString(R.string.mse_description, it.label, mseScore, agoPhrase)
    }
    return buildString {
        append(
            context.getString(
                R.string.widget_content_description_format,
                context.getString(R.string.app_name),
                scoreText,
                levelLabel.lowercase(Locale.US),
                tonePart,
                snapshot.trend.name.lowercase(Locale.US),
                countryName,
                updatedPart,
            ),
        )
        if (msePart != null) append(" $msePart")
        // WB-055: stale a11y
        if (isStale) append(" ${context.getString(R.string.widget_content_description_stale_suffix)}")
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
