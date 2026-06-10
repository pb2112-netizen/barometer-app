package com.worldbarometer.app.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatowanie updated_at (ISO UTC) na czytelny, względny czas po polsku.
 * Parsowanie jest bezpieczne: nieprawidłowy/obcy format → null (UI pokaże fallback).
 */
object RelativeTime {

    private val localFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("pl"))

    fun parseOrNull(isoUtc: String?): Instant? {
        if (isoUtc.isNullOrBlank()) return null
        return runCatching { Instant.parse(isoUtc) }.getOrNull()
    }

    fun format(isoUtc: String?, nowMillis: Long = System.currentTimeMillis()): String {
        val instant = parseOrNull(isoUtc) ?: return "brak danych o czasie"
        val diffMs = nowMillis - instant.toEpochMilli()
        if (diffMs < 0) return "przed chwilą"

        val minutes = diffMs / 60_000
        val hours = minutes / 60
        return when {
            minutes < 1 -> "przed chwilą"
            minutes < 60 -> "$minutes min temu"
            hours < 24 -> "$hours godz. temu"
            else -> instant.atZone(ZoneId.systemDefault()).format(localFormatter)
        }
    }
}
