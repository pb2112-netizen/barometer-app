package com.worldbarometer.app.core

import com.worldbarometer.app.data.model.ScoreHistoryPoint
import com.worldbarometer.app.data.model.TopEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SparklineTest {

    private fun point(hour: Int, score: Double = 5.0) =
        ScoreHistoryPoint(timestamp = "2026-06-01T${hour.toString().padStart(2, '0')}:00:00Z", score = score)

    @Test
    fun findEventsAnchor_returnsNull_whenHistoryHasFewerThanTwoPoints() {
        val history = listOf(point(10))
        assertNull(findEventsAnchor(history, "2026-06-01T09:00:00Z"))
    }

    @Test
    fun findEventsAnchor_returnsNull_whenAnchorIsoInvalid() {
        val history = listOf(point(8), point(9), point(10))
        assertNull(findEventsAnchor(history, "not-an-iso-timestamp"))
    }

    @Test
    fun findEventsAnchor_picksLastPointAtOrBeforeAnchor() {
        val history = listOf(point(8, 4.0), point(9, 5.0), point(10, 6.0))
        val anchor = findEventsAnchor(history, "2026-06-01T09:30:00Z")
        assertNotNull(anchor)
        assertEquals(1, anchor!!.historyIndex)
        assertEquals(5.0, anchor.score, 0.001)
    }

    @Test
    fun resolveVisibleEventsAnchor_returnsNull_whenAnchorIsLastHistoryPoint() {
        val history = listOf(point(8), point(9), point(10))
        val events = listOf(TopEvent(title = "Event", summary = "Summary", score = 6.0))
        val anchor = resolveVisibleEventsAnchor(
            history = history,
            eventsAnchorAt = "2026-06-01T10:00:00Z",
            topEvents = events,
            shortSummary = "Breaking news headline",
        )
        assertNull(anchor)
    }

    @Test
    fun resolveVisibleEventsAnchor_returnsAnchor_whenInPastAndSummaryDisplayable() {
        val history = listOf(point(8, 4.0), point(9, 5.0), point(10, 6.0))
        val events = listOf(TopEvent(title = "Event", summary = "Summary", score = 6.0))
        val anchor = resolveVisibleEventsAnchor(
            history = history,
            eventsAnchorAt = "2026-06-01T09:00:00Z",
            topEvents = events,
            shortSummary = "Breaking news headline",
        )
        assertNotNull(anchor)
        assertEquals(1, anchor!!.historyIndex)
    }

    @Test
    fun resolveVisibleEventsAnchor_returnsNull_whenShortSummaryIsMeta() {
        val history = listOf(point(8), point(9), point(10))
        val events = listOf(TopEvent(title = "Event", summary = "Summary", score = 6.0))
        assertNull(
            resolveVisibleEventsAnchor(
                history = history,
                eventsAnchorAt = "2026-06-01T09:00:00Z",
                topEvents = events,
                shortSummary = "No significant change in the news cycle",
            ),
        )
    }

    @Test
    fun parseInstant_returnsNull_forInvalidIso() {
        assertNull(Sparkline.parseInstant("bad-iso"))
    }
}
