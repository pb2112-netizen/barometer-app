package com.worldbarometer.app.core

import com.worldbarometer.app.data.model.BarometerData
import com.worldbarometer.app.data.model.ScoreHistoryPoint
import com.worldbarometer.app.data.model.SourceLink
import com.worldbarometer.app.data.model.TopEvent

/**
 * Sanityzacja niezaufanej treści z barometer.json (defense-in-depth).
 * Mimo że host jest zaufany (HTTPS), traktujemy dane jako wejście niezaufane:
 * - clamp ocen do skali 1.0–10.0 (model/źródło nie wymusi wartości spoza skali),
 * - limit liczby eventów (UI pokazuje TOP 3),
 * - usunięcie znaków sterujących i przycięcie długości (ochrona layoutu/pamięci).
 * Treść jest tylko renderowana jako tekst (Compose Text) — bez HTML/WebView/linkify.
 */

private const val MAX_SUMMARY = 200
private const val MAX_TITLE = 200
private const val MAX_EVENT_SUMMARY = 600
private const val MAX_RATIONALE = 1500
private const val MAX_SOURCE = 40
private const val MAX_SOURCES = 8
private const val MAX_SOURCE_LINKS = 3
private const val MAX_URL = 2048
private const val MAX_EVENTS = 3
private const val MAX_HISTORY_POINTS = 72

private fun ScoreHistoryPoint.sanitized(): ScoreHistoryPoint = copy(
    timestamp = timestamp.sanitizeText(40),
    score = score.clampScore(),
)

private fun String.sanitizeText(maxLength: Int): String {
    // Usuń znaki sterujące (poza spacją), zwiń whitespace, przytnij długość.
    val cleaned = buildString(length.coerceAtMost(maxLength * 2)) {
        for (ch in this@sanitizeText) {
            if (ch == '\n' || ch == '\t' || ch == '\r') append(' ')
            else if (!ch.isISOControl()) append(ch)
        }
    }.replace(Regex("\\s{2,}"), " ").trim()
    return if (cleaned.length > maxLength) cleaned.take(maxLength).trimEnd() + "…" else cleaned
}

private fun Double.clampScore(): Double =
    if (isNaN() || isInfinite()) 1.0 else coerceIn(1.0, 10.0)

private fun SourceLink.sanitized(): SourceLink? {
    val cleanName = name.sanitizeText(MAX_SOURCE)
    val cleanUrl = url.sanitizeText(MAX_URL)
    if (cleanName.isBlank() || !cleanUrl.isAllowedSourceUrl()) return null
    return SourceLink(name = cleanName, url = cleanUrl)
}

private fun TopEvent.sanitized(): TopEvent = copy(
    title = title.sanitizeText(MAX_TITLE),
    summary = summary.sanitizeText(MAX_EVENT_SUMMARY),
    score = score.clampScore(),
    sentiment = sentiment?.sanitizeText(20),
    nowosc = nowosc?.sanitizeText(40),
    category = category?.sanitizeText(40),
    sources = sources.asSequence()
        .map { it.sanitizeText(MAX_SOURCE) }
        .filter { it.isNotBlank() }
        .take(MAX_SOURCES)
        .toList(),
    sourceLinks = sourceLinks.asSequence()
        .mapNotNull { it.sanitized() }
        .distinctBy { it.url }
        .take(MAX_SOURCE_LINKS)
        .toList(),
)

fun BarometerData.sanitized(): BarometerData = copy(
    globalScore = globalScore.clampScore(),
    shortSummary = shortSummary.sanitizeText(MAX_SUMMARY),
    rationale = rationale.sanitizeText(MAX_RATIONALE),
    topEvents = topEvents.take(MAX_EVENTS).map { it.sanitized() },
    scoreHistory = scoreHistory
        .map { it.sanitized() }
        .filter { it.timestamp.isNotBlank() }
        .sortedBy { it.timestamp }
        .take(MAX_HISTORY_POINTS),
    levelLabel = levelLabel?.sanitizeText(20),
    tone = tone?.sanitizeText(20),
    trend = trend?.sanitizeText(20),
    eventsAnchorAt = eventsAnchorAt?.sanitizeText(40),
    // updated_at walidowane przy parsowaniu czasu (RelativeTime); tryb tylko do podglądu.
    tryb = tryb.sanitizeText(80),
)
