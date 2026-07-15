package com.worldbarometer.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldbarometer.app.data.model.ScoreHistoryPoint
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/** Default/fallback color for the static "Most significant event" marker (WB-060). */
val SignificantMarkerColor = Color(0xFFEAB308)

/** Static halo matching pulsing “now” marker at peak alpha (0.85 × 0.45). */
private const val MARKER_HALO_ALPHA = 0.3825f
private const val MARKER_HALO_RADIUS_MULTIPLIER = 2.2f

/** WB-055: single source of truth for anchor marker radius — dashboard and widget use the same value. */
const val ANCHOR_MARKER_RADIUS_DP = 3.5f

/** Dashboard sparkline uses ~70% width, centered in parent. */
const val DASHBOARD_CHART_WIDTH_FRACTION = 0.7f

@Composable
fun SignificantMarkerDot(
    modifier: Modifier = Modifier,
    dotRadius: Dp = ANCHOR_MARKER_RADIUS_DP.dp,
    color: Color = SignificantMarkerColor,
) {
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { dotRadius.toPx() }
    val haloRadiusPx = dotRadiusPx * MARKER_HALO_RADIUS_MULTIPLIER
    val boxSize = with(density) { (haloRadiusPx * 2f).toDp() }

    Box(modifier = modifier.size(boxSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color = color.copy(alpha = MARKER_HALO_ALPHA), radius = haloRadiusPx)
            drawCircle(color = color, radius = dotRadiusPx)
        }
    }
}

/** Wspólna logika sparkline (WB-003/WB-029): stała skala Y 1–10; dane silnika 48h (WB-060: renderowanie 24h). */
object Sparkline {
    const val Y_MIN = 1.0
    const val Y_MAX = 10.0
    /** Retencja danych w silniku (HISTORY_HOURS) — dokumentacyjne, nieużywane już w rysowaniu (WB-060). */
    const val WINDOW_HOURS = 48L
    /** WB-060: okno RENDEROWANIA wykresu — 24h (dane w JSON nadal 48h, kompatybilność). */
    const val DISPLAY_WINDOW_HOURS = 24L

    /** Keeps score=1 one dot above the x-axis. */
    const val Y_BOTTOM_INSET_RATIO = 0.085f
    const val Y_TOP_INSET_RATIO = 0.02f
    /** Horizontal inset so first/last point halos are not clipped. */
    const val X_EDGE_INSET_RATIO = 0.065f

    data class PlotPoint(val x: Float, val y: Float)

    data class Style(
        val lineColor: Color,
        val axisColor: Color,
        val lastPointColor: Color,
        val enablePulse: Boolean = false,
    )

    data class RenderConfig(
        val showYAxis: Boolean = true,
        val lineStrokeScale: Float = 1f,
        val pointScale: Float = 1f,
    )

    /** WB-060: filtruje historie do okna wyswietlania (usuwa punkty starsze, nie clampuje na krawedzi). */
    fun pointsInWindow(
        history: List<ScoreHistoryPoint>,
        windowEnd: Instant,
        windowHours: Long = DISPLAY_WINDOW_HOURS,
    ): List<ScoreHistoryPoint> {
        val cutoff = windowEnd.minus(Duration.ofHours(windowHours))
        return history.filter { point ->
            val instant = parseInstant(point.timestamp) ?: return@filter false
            !instant.isBefore(cutoff)
        }
    }

    /** WB-060: lekkie wygladzenie — tlumi tylko male wahania, zachowuje realne skoki (delta >= threshold). */
    fun smoothed(history: List<ScoreHistoryPoint>, deltaThreshold: Double = 0.6): List<ScoreHistoryPoint> {
        if (history.size < 3) return history
        return history.mapIndexed { index, point ->
            if (index == 0 || index == history.lastIndex) return@mapIndexed point
            val prev = history[index - 1].score
            val next = history[index + 1].score
            val isBigJump = kotlin.math.abs(point.score - prev) >= deltaThreshold ||
                kotlin.math.abs(next - point.score) >= deltaThreshold
            if (isBigJump) point else point.copy(score = 0.25 * prev + 0.5 * point.score + 0.25 * next)
        }
    }

    fun plotPoints(
        history: List<ScoreHistoryPoint>,
        windowEnd: Instant,
        plotWidth: Float,
        plotHeight: Float,
        windowHours: Long = DISPLAY_WINDOW_HOURS,
    ): List<PlotPoint> {
        if (plotWidth <= 0f || plotHeight <= 0f) return emptyList()

        return history.mapNotNull { point ->
            plotPointAt(point, windowEnd, plotWidth, plotHeight, windowHours)
        }
    }

    fun plotPointAtIndex(
        history: List<ScoreHistoryPoint>,
        historyIndex: Int,
        windowEnd: Instant,
        plotWidth: Float,
        plotHeight: Float,
        windowHours: Long = DISPLAY_WINDOW_HOURS,
    ): PlotPoint? {
        if (historyIndex !in history.indices) return null
        return plotPointAt(history[historyIndex], windowEnd, plotWidth, plotHeight, windowHours)
    }

    fun scoreToPlotY(score: Double, plotHeight: Float): Float {
        val bottomInset = plotHeight * Y_BOTTOM_INSET_RATIO
        val topInset = plotHeight * Y_TOP_INSET_RATIO
        val usableHeight = (plotHeight - bottomInset - topInset).coerceAtLeast(1f)
        val ratio = ((score - Y_MIN) / (Y_MAX - Y_MIN)).toFloat().coerceIn(0f, 1f)
        return topInset + usableHeight * (1f - ratio)
    }

    fun scoreToPlotX(xRatio: Float, plotWidth: Float): Float {
        val inset = plotWidth * X_EDGE_INSET_RATIO
        val usableWidth = (plotWidth - 2f * inset).coerceAtLeast(1f)
        return inset + xRatio * usableWidth
    }

    private fun plotPointAt(
        point: ScoreHistoryPoint,
        windowEnd: Instant,
        plotWidth: Float,
        plotHeight: Float,
        windowHours: Long,
    ): PlotPoint? {
        val instant = parseInstant(point.timestamp) ?: return null
        val windowStart = windowEnd.minus(Duration.ofHours(windowHours))
        val windowMillis = Duration.ofHours(windowHours).toMillis().toFloat().coerceAtLeast(1f)
        val xRatio = ((instant.toEpochMilli() - windowStart.toEpochMilli()).toFloat() / windowMillis)
            .coerceIn(0f, 1f)
        return PlotPoint(
            x = scoreToPlotX(xRatio, plotWidth),
            y = scoreToPlotY(point.score, plotHeight),
        )
    }

    fun windowEnd(history: List<ScoreHistoryPoint>, fallbackIso: String?): Instant {
        val fromHistory = history.mapNotNull { parseInstant(it.timestamp) }.maxOrNull()
        if (fromHistory != null) return fromHistory
        return parseInstant(fallbackIso.orEmpty()) ?: Instant.now()
    }

    fun parseInstant(iso: String): Instant? =
        runCatching { Instant.parse(iso) }.getOrNull()
}

private fun buildComposeSmoothPath(points: List<Offset>): ComposePath {
    val path = ComposePath()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }

    path.moveTo(points[0].x, points[0].y)
    for (index in 0 until points.size - 1) {
        val previous = points[maxOf(0, index - 1)]
        val current = points[index]
        val next = points[index + 1]
        val afterNext = points[minOf(points.lastIndex, index + 2)]

        val control1X = current.x + (next.x - previous.x) / 6f
        val control1Y = current.y + (next.y - previous.y) / 6f
        val control2X = next.x - (afterNext.x - current.x) / 6f
        val control2Y = next.y - (afterNext.y - current.y) / 6f
        path.cubicTo(control1X, control1Y, control2X, control2Y, next.x, next.y)
    }
    return path
}

private fun buildAndroidSmoothPath(points: List<Sparkline.PlotPoint>, plotLeft: Float): AndroidPath {
    val path = AndroidPath()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(plotLeft + points[0].x, points[0].y)
        return path
    }

    val offsets = points.map { Offset(plotLeft + it.x, it.y) }
    path.moveTo(offsets[0].x, offsets[0].y)
    for (index in 0 until offsets.size - 1) {
        val previous = offsets[maxOf(0, index - 1)]
        val current = offsets[index]
        val next = offsets[index + 1]
        val afterNext = offsets[minOf(offsets.lastIndex, index + 2)]

        val control1X = current.x + (next.x - previous.x) / 6f
        val control1Y = current.y + (next.y - previous.y) / 6f
        val control2X = next.x - (afterNext.x - current.x) / 6f
        val control2Y = next.y - (afterNext.y - current.y) / 6f
        path.cubicTo(control1X, control1Y, control2X, control2Y, next.x, next.y)
    }
    return path
}

@Composable
fun SparklineChart(
    history: List<ScoreHistoryPoint>,
    updatedAt: String?,
    lastPointColor: Color,
    enablePulse: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    lineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
) {
    val density = LocalDensity.current
    val axisWidth = with(density) { 1.dp.toPx() }
    val axisPad = with(density) { 4.dp.toPx() }
    // WB-060: grubsza linia (kosmetyka) — 1.5dp -> 2.5dp.
    val lineStroke = with(density) { 2.5.dp.toPx() }
    val pointRadius = with(density) { 3.5.dp.toPx() }
    val infiniteTransition = rememberInfiniteTransition(label = "sparkline")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "pulseAlpha",
    )

    val style = Sparkline.Style(
        lineColor = lineColor,
        axisColor = axisColor,
        lastPointColor = lastPointColor,
        enablePulse = enablePulse,
    )

    Canvas(
        modifier = modifier
            .height(height)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val plotLeft = axisPad
        val plotBottom = size.height - axisPad - axisWidth
        val plotWidth = (size.width - plotLeft - axisPad).coerceAtLeast(0f)
        val plotHeight = plotBottom.coerceAtLeast(0f)
        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        drawAxes(plotLeft, plotBottom, plotWidth, axisWidth, style.axisColor, showYAxis = false)

        // WB-060: okno renderowania 24h + lekkie wygladzenie (zachowuje realne skoki).
        val end = Sparkline.windowEnd(history, updatedAt)
        val windowed = Sparkline.pointsInWindow(history, end)
        val smoothedHistory = Sparkline.smoothed(windowed)
        val points = Sparkline.plotPoints(smoothedHistory, end, plotWidth, plotHeight, windowHours = Sparkline.DISPLAY_WINDOW_HOURS)
        if (points.isEmpty()) return@Canvas

        val offsetPoints = points.map { Offset(plotLeft + it.x, it.y) }
        drawPath(
            path = buildComposeSmoothPath(offsetPoints),
            color = style.lineColor,
            style = Stroke(width = lineStroke, cap = StrokeCap.Round),
        )

        val last = offsetPoints.last()
        if (style.enablePulse) {
            drawCircle(
                color = style.lastPointColor.copy(alpha = pulseAlpha * 0.45f),
                radius = pointRadius * 2.2f,
                center = last,
            )
        }
        drawCircle(color = style.lastPointColor, radius = pointRadius, center = last)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxes(
    plotLeft: Float,
    plotBottom: Float,
    plotWidth: Float,
    axisWidth: Float,
    axisColor: Color,
    showYAxis: Boolean,
) {
    if (showYAxis) {
        drawLine(
            color = axisColor,
            start = Offset(plotLeft, 0f),
            end = Offset(plotLeft, plotBottom),
            strokeWidth = axisWidth,
            cap = StrokeCap.Butt,
        )
    }
    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotBottom),
        end = Offset(plotLeft + plotWidth, plotBottom),
        strokeWidth = axisWidth,
        cap = StrokeCap.Butt,
    )
}

/** Bitmapa sparkline dla widgetu Glance (WB-003/WB-029). */
object SparklineBitmap {
    private val widgetConfig = Sparkline.RenderConfig(
        showYAxis = false,
        // WB-060: grubsza linia (kosmetyka, proporcjonalnie do mniejszego płótna widgetu).
        lineStrokeScale = 1.6f,
        pointScale = 1.1f,
    )

    fun render(
        context: Context,
        history: List<ScoreHistoryPoint>,
        updatedAt: String?,
        widthPx: Int,
        heightPx: Int,
        lastPointColor: Color = Color.White,
        config: Sparkline.RenderConfig = widgetConfig,
    ): Bitmap {
        val safeWidth = widthPx.coerceAtLeast(1)
        val safeHeight = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val density = context.resources.displayMetrics.density
        val axisPad = 2f * density
        val axisWidth = 1f * density
        val lineStroke = 1f * density * config.lineStrokeScale
        val pointRadius = 2.5f * density * config.pointScale
        val haloRadius = 5f * density * config.pointScale

        val plotLeft = axisPad
        val plotBottom = safeHeight - axisPad - axisWidth
        val plotWidth = (safeWidth - plotLeft - axisPad).coerceAtLeast(0f)
        val plotHeight = plotBottom.coerceAtLeast(0f)

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.copy(alpha = 0.4f).toArgb()
            strokeWidth = axisWidth
            style = Paint.Style.STROKE
        }
        if (config.showYAxis) {
            canvas.drawLine(plotLeft, 0f, plotLeft, plotBottom, axisPaint)
        }
        canvas.drawLine(plotLeft, plotBottom, plotLeft + plotWidth, plotBottom, axisPaint)

        // WB-060: okno renderowania 24h + lekkie wygladzenie (zachowuje realne skoki).
        val end = Sparkline.windowEnd(history, updatedAt)
        val windowed = Sparkline.pointsInWindow(history, end)
        val smoothedHistory = Sparkline.smoothed(windowed)
        val points = Sparkline.plotPoints(smoothedHistory, end, plotWidth, plotHeight, windowHours = Sparkline.DISPLAY_WINDOW_HOURS)
        if (points.isEmpty()) return bitmap

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.copy(alpha = 0.8f).toArgb()
            strokeWidth = lineStroke
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(buildAndroidSmoothPath(points, plotLeft), linePaint)

        val last = points.last()
        val lx = plotLeft + last.x
        val ly = last.y
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.copy(alpha = 0.35f).toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }
        canvas.drawCircle(lx, ly, haloRadius, haloPaint)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lastPointColor.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(lx, ly, pointRadius, dotPaint)

        return bitmap
    }

    fun dpToPx(context: Context, dp: Dp): Int =
        (dp.value * context.resources.displayMetrics.density).roundToInt()
}

/** Mały marker MSE z halo — bitmapa dla widgetu Glance (WB-060: kolor wg LevelPalette.eventBadgeColor). */
object SignificantMarkerBitmap {
    fun render(
        context: Context,
        dotRadiusDp: Dp = ANCHOR_MARKER_RADIUS_DP.dp,
        color: Color = SignificantMarkerColor,
    ): Bitmap {
        val markerColor = color
        val density = context.resources.displayMetrics.density
        val dotRadiusPx = dotRadiusDp.value * density
        val haloRadiusPx = dotRadiusPx * MARKER_HALO_RADIUS_MULTIPLIER
        val sizePx = (haloRadiusPx * 2f).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = markerColor.copy(alpha = MARKER_HALO_ALPHA).toArgb()
            style = Paint.Style.FILL
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = markerColor.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, haloRadiusPx, haloPaint)
        canvas.drawCircle(cx, cy, dotRadiusPx, dotPaint)
        return bitmap
    }
}
