package com.worldbarometer.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RelativeTimeTest {

    @Test
    fun parseOrNull_returnsNull_forInvalidIso() {
        assertEquals(null, RelativeTime.parseOrNull(null))
        assertEquals(null, RelativeTime.parseOrNull(""))
        assertEquals(null, RelativeTime.parseOrNull("not-iso"))
    }

    @Test
    fun formatAbsolute_returnsNoTimestamp_forInvalidIso() {
        assertEquals("no timestamp", RelativeTime.formatAbsolute(null))
        assertEquals("no timestamp", RelativeTime.formatAbsolute("bad-iso"))
    }

    @Test
    fun formatAbsoluteFull_formatsValidIso() {
        val iso = "2026-06-12T14:05:00Z"
        val expected = Instant.parse(iso)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH))
        assertEquals(expected, RelativeTime.formatAbsoluteFull(iso))
    }

    // --- WB-059: formatShortAgo ---

    @Test
    fun formatShortAgo_returnsNull_forInvalidOrMissingIso() {
        assertEquals(null, RelativeTime.formatShortAgo(null))
        assertEquals(null, RelativeTime.formatShortAgo(""))
        assertEquals(null, RelativeTime.formatShortAgo("not-iso"))
    }

    @Test
    fun formatShortAgo_lessThanOneHour_returnsJustNow() {
        val now = Instant.parse("2026-07-12T12:00:00Z")
        val detected = now.minus(java.time.Duration.ofMinutes(30))
        assertEquals("just now", RelativeTime.formatShortAgo(detected.toString(), now.toEpochMilli()))
    }

    @Test
    fun formatShortAgo_twelveHours_returnsHoursAgo() {
        val now = Instant.parse("2026-07-12T12:00:00Z")
        val detected = now.minus(java.time.Duration.ofHours(12))
        assertEquals("12h ago", RelativeTime.formatShortAgo(detected.toString(), now.toEpochMilli()))
    }

    // --- WB-060: cap na 24h+ (bez liczenia dni) ---

    @Test
    fun formatShortAgo_twentyThreeHours_returnsHoursAgo_borderline() {
        val now = Instant.parse("2026-07-12T12:00:00Z")
        val detected = now.minus(java.time.Duration.ofHours(23))
        assertEquals("23h ago", RelativeTime.formatShortAgo(detected.toString(), now.toEpochMilli()))
    }

    @Test
    fun formatShortAgo_twentyFourHours_capsAt24hPlus() {
        val now = Instant.parse("2026-07-12T12:00:00Z")
        val detected = now.minus(java.time.Duration.ofHours(24))
        assertEquals("24h+", RelativeTime.formatShortAgo(detected.toString(), now.toEpochMilli()))
    }

    @Test
    fun formatShortAgo_seventyTwoHours_capsAt24hPlus() {
        val now = Instant.parse("2026-07-12T12:00:00Z")
        val detected = now.minus(java.time.Duration.ofHours(72))
        assertEquals("24h+", RelativeTime.formatShortAgo(detected.toString(), now.toEpochMilli()))
    }
}
