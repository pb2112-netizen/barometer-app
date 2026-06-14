package com.worldbarometer.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldbarometer.app.data.model.ScoreHistoryPoint
import java.time.Duration
import java.time.Instant

/** Wspólna logika sparkline (WB-003): stała skala Y 1–10, okno czasu 72 h. */
object Sparkline {
    const val Y_MIN = 1.0
    const val Y_MAX = 10.0
    private val WINDOW_HOURS = 72L

    data class PlotPoint(val x: Float, val y: Float)

    data class Style(
        val lineColor: Color,
        val axisColor: Color,
        val lastPointColor: Color,
        val enablePulse: Boolean = false,
        val haloColor: Color? = null,
    )

    fun plotPoints(
        history: List<ScoreHistoryPoint>,
        windowEnd: Instant,
        plotWidth: Float,
        plotHeight: Float,
    ): List<PlotPoint> {
        if (plotWidth <= 0f || plotHeight <= 0f) return emptyList()
        val windowStart = windowEnd.minus(Duration.ofHours(WINDOW_HOURS))
        val windowMillis = Duration.ofHours(WINDOW_HOURS).toMillis().toFloat().coerceAtLeast(1f)

        return history.mapNotNull { point ->
            val instant = parseInstant(point.timestamp) ?: return@mapNotNull null
            val xRatio = ((instant.toEpochMilli() - windowStart.toEpochMilli()).toFloat() / windowMillis)
                .coerceIn(0f, 1f)
            val yRatio = ((point.score - Y_MIN) / (Y_MAX - Y_MIN)).toFloat().coerceIn(0f, 1f)
            PlotPoint(
                x = xRatio * plotWidth,
                y = plotHeight * (1f - yRatio),
            )
        }
    }

    fun windowEnd(history: List<ScoreHistoryPoint>, fallbackIso: String?): Instant {
        val fromHistory = history.mapNotNull { parseInstant(it.timestamp) }.maxOrNull()
        if (fromHistory != null) return fromHistory
        return parseInstant(fallbackIso.orEmpty()) ?: Instant.now()
    }

    private fun parseInstant(iso: String): Instant? =
        runCatching { Instant.parse(iso) }.getOrNull()
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
    val lineStroke = with(density) { 1.5.dp.toPx() }
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
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val plotLeft = axisPad + axisWidth
        val plotBottom = size.height - axisPad - axisWidth
        val plotWidth = (size.width - plotLeft).coerceAtLeast(0f)
        val plotHeight = plotBottom.coerceAtLeast(0f)
        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        drawAxes(plotLeft, plotBottom, plotWidth, axisWidth, style.axisColor)

        val end = Sparkline.windowEnd(history, updatedAt)
        val points = Sparkline.plotPoints(history, end, plotWidth, plotHeight)
        if (points.isEmpty()) return@Canvas

        val offsetPoints = points.map { Offset(plotLeft + it.x, it.y) }

        for (i in 0 until offsetPoints.size - 1) {
            drawLine(
                color = style.lineColor,
                start = offsetPoints[i],
                end = offsetPoints[i + 1],
                strokeWidth = lineStroke,
                cap = StrokeCap.Round,
            )
        }

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
) {
    drawLine(
        color = axisColor,
        start = Offset(plotLeft, 0f),
        end = Offset(plotLeft, plotBottom),
        strokeWidth = axisWidth,
        cap = StrokeCap.Butt,
    )
    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotBottom),
        end = Offset(plotLeft + plotWidth, plotBottom),
        strokeWidth = axisWidth,
        cap = StrokeCap.Butt,
    )
}

/** Bitmapa sparkline dla widgetu Glance (WB-003 §5.2). */
object SparklineBitmap {
    private const val WIDGET_WIDTH_DP = 36f
    private const val WIDGET_HEIGHT_DP = 24f

    fun render(
        context: Context,
        history: List<ScoreHistoryPoint>,
        updatedAt: String?,
        lastPointColor: Color = Color.White,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val widthPx = (WIDGET_WIDTH_DP * density).toInt().coerceAtLeast(1)
        val heightPx = (WIDGET_HEIGHT_DP * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val axisPad = 2f * density
        val axisWidth = 1f * density
        val lineStroke = 1f * density
        val pointRadius = 2.5f * density
        val haloRadius = 5f * density

        val plotLeft = axisPad + axisWidth
        val plotBottom = heightPx - axisPad - axisWidth
        val plotWidth = (widthPx - plotLeft).coerceAtLeast(0f)
        val plotHeight = plotBottom.coerceAtLeast(0f)

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.copy(alpha = 0.4f).toArgb()
            strokeWidth = axisWidth
            style = Paint.Style.STROKE
        }
        canvas.drawLine(plotLeft, 0f, plotLeft, plotBottom, axisPaint)
        canvas.drawLine(plotLeft, plotBottom, plotLeft + plotWidth, plotBottom, axisPaint)

        val end = Sparkline.windowEnd(history, updatedAt)
        val points = Sparkline.plotPoints(history, end, plotWidth, plotHeight)
        if (points.isEmpty()) return bitmap

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.copy(alpha = 0.8f).toArgb()
            strokeWidth = lineStroke
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val path = Path()
        points.forEachIndexed { index, p ->
            val x = plotLeft + p.x
            val y = p.y
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)

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
}
