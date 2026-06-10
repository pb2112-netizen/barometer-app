package com.worldbarometer.app.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatowanie updated_at (ISO UTC) na czytelny, względny czas (po angielsku).
 * Parsowanie jest bezpieczne: nieprawidłowy/obcy format → null (UI pokaże fallback).
 */
object RelativeTime {

    private val localFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)

    fun parseOrNull(isoUtc: String?): Instant? {
        if (isoUtc.isNullOrBlank()) return null
        return runCatching { Instant.parse(isoUtc) }.getOrNull()
    }

    fun format(isoUtc: String?, nowMillis: Long = System.currentTimeMillis()): String {
        val instant = parseOrNull(isoUtc) ?: return "no timestamp"
        val diffMs = nowMillis - instant.toEpochMilli()
        if (diffMs < 0) return "just now"

        val minutes = diffMs / 60_000
        val hours = minutes / 60
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            else -> instant.atZone(ZoneId.systemDefault()).format(localFormatter)
        }
    }
}
