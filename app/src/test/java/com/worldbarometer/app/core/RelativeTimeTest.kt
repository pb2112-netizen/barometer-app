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
}
