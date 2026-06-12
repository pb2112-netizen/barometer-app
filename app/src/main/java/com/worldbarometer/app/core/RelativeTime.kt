package com.worldbarometer.app.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatowanie updated_at (ISO UTC) na czytelny czas ABSOLUTNY lokalny (po angielsku).
 * Parsowanie jest bezpieczne: nieprawidłowy/obcy format → fallback ("no timestamp").
 */
object RelativeTime {

    private val localFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)

    private val timeOnlyFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    private val fullDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

    fun parseOrNull(isoUtc: String?): Instant? {
        if (isoUtc.isNullOrBlank()) return null
        return runCatching { Instant.parse(isoUtc) }.getOrNull()
    }

    /**
     * Czas ABSOLUTNY (lokalny) — używany w widgecie i UI dla „ostatniej aktualizacji".
     * Nie starzeje się: zamrożony napis pozostaje prawdziwy, nawet gdy widget długo się
     * nie przerysuje (to był problem czasu względnego). Dzisiaj → „HH:mm"; inny dzień → „d MMM, HH:mm"
     * (żeby stare/offline dane nie wyglądały na świeże).
     */
    fun formatAbsolute(isoUtc: String?, nowMillis: Long = System.currentTimeMillis()): String {
        val instant = parseOrNull(isoUtc) ?: return "no timestamp"
        val zone = ZoneId.systemDefault()
        val updated = instant.atZone(zone)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val formatter = if (updated.toLocalDate() == today) timeOnlyFormatter else localFormatter
        return updated.format(formatter)
    }

    /**
     * Pełny czas absolutny z rokiem — „d MMM yyyy, HH:mm" (np. „12 Jun 2026, 14:05").
     * Używany tam, gdzie jest miejsce na pełną datę (ekran Settings: „Last data update").
     */
    fun formatAbsoluteFull(isoUtc: String?): String {
        val instant = parseOrNull(isoUtc) ?: return "no timestamp"
        return instant.atZone(ZoneId.systemDefault()).format(fullDateTimeFormatter)
    }
}
