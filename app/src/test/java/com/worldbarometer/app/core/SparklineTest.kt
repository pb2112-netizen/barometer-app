package com.worldbarometer.app.core

import com.worldbarometer.app.data.model.ScoreHistoryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SparklineTest {

    private fun point(hour: Int, score: Double = 5.0) =
        ScoreHistoryPoint(timestamp = "2026-06-01T${hour.toString().padStart(2, '0')}:00:00Z", score = score)

    @Test
    fun parseInstant_returnsNull_forInvalidIso() {
        assertNull(Sparkline.parseInstant("bad-iso"))
    }

    // --- WB-060: pointsInWindow (okno renderowania 24h) ---

    @Test
    fun pointsInWindow_filtersOutPointsOlderThan24h() {
        val windowEnd = Instant.parse("2026-06-02T10:00:00Z")
        val history = listOf(
            point(9), // 2026-06-01T09:00:00Z -> 25h before windowEnd, excluded
            point(11), // 2026-06-01T11:00:00Z -> 23h before windowEnd, included
            point(12),
        )
        val windowed = Sparkline.pointsInWindow(history, windowEnd)
        assertEquals(2, windowed.size)
        assertEquals("2026-06-01T11:00:00Z", windowed.first().timestamp)
    }

    @Test
    fun pointsInWindow_keepsPointExactlyAtCutoffBoundary() {
        val windowEnd = Instant.parse("2026-06-02T10:00:00Z")
        val history = listOf(
            ScoreHistoryPoint(timestamp = "2026-06-01T10:00:00Z", score = 5.0), // exactly 24h before -> kept
        )
        val windowed = Sparkline.pointsInWindow(history, windowEnd)
        assertEquals(1, windowed.size)
    }

    // --- WB-060: smoothed (lekkie wygladzenie, zachowuje realne skoki) ---

    @Test
    fun smoothed_leavesHistoryUnchanged_whenFewerThanThreePoints() {
        val history = listOf(point(8, 5.0), point(9, 5.4))
        assertEquals(history, Sparkline.smoothed(history))
    }

    @Test
    fun smoothed_dampensSmallFluctuation_belowThreshold() {
        // Delty sąsiadów: |5.2-5.0|=0.2, |5.0-5.2|=0.2 -- obie < 0.6 -> środkowy punkt wygładzony.
        val history = listOf(point(8, 5.0), point(9, 5.2), point(10, 5.0))
        val smoothed = Sparkline.smoothed(history)
        val middle = smoothed[1].score
        assertTrue("expected smoothing to pull score away from raw 5.2, got $middle", middle != 5.2)
        assertEquals(0.25 * 5.0 + 0.5 * 5.2 + 0.25 * 5.0, middle, 0.0001)
    }

    @Test
    fun smoothed_preservesBigJump_atOrAboveThreshold() {
        // Skok |7.0-5.0|=2.0 >= 0.6 -> punkt zachowany bez tłumienia.
        val history = listOf(point(8, 5.0), point(9, 7.0), point(10, 7.1))
        val smoothed = Sparkline.smoothed(history)
        assertEquals(7.0, smoothed[1].score, 0.0001)
    }

    @Test
    fun smoothed_neverTouchesFirstOrLastPoint() {
        val history = listOf(point(8, 5.0), point(9, 5.1), point(10, 5.0), point(11, 9.0))
        val smoothed = Sparkline.smoothed(history)
        assertEquals(5.0, smoothed.first().score, 0.0001)
        assertEquals(9.0, smoothed.last().score, 0.0001)
    }

    // --- WB-074: mseMarkerPosition (zolty punkt na osi czasu = pierwsze wykrycie MSE) ---

    private val windowEnd: Instant = Instant.parse("2026-06-02T10:00:00Z")

    @Test
    fun mseMarkerPosition_isNull_whenDetectedAtMissingOrInvalid() {
        assertNull(Sparkline.mseMarkerPosition(null, windowEnd))
        assertNull(Sparkline.mseMarkerPosition("", windowEnd))
        assertNull(Sparkline.mseMarkerPosition("nie-iso", windowEnd))
    }

    @Test
    fun mseMarkerPosition_isNull_whenDetectedAtIsInTheFuture() {
        assertNull(Sparkline.mseMarkerPosition("2026-06-02T11:00:00Z", windowEnd))
    }

    @Test
    fun mseMarkerPosition_isHalfwayForTwelveHourOldEvent() {
        // 12h przed koncem okna 24h -> polowa szerokosci wykresu
        val marker = Sparkline.mseMarkerPosition("2026-06-01T22:00:00Z", windowEnd)!!
        assertEquals(0.5f, marker.xRatio, 0.0001f)
        assertTrue(!marker.isClampedToWindowStart)
    }

    @Test
    fun mseMarkerPosition_clampsToLeftEdge_whenTopicStartedBeforeWindow() {
        // Po WB-062 detected_at championa rutynowo przekracza 24h — znacznik przypinany jest
        // do lewej krawedzi i oznaczany flaga, zeby UI narysowalo go wyblaklym (WB-074).
        val marker = Sparkline.mseMarkerPosition("2026-05-30T10:00:00Z", windowEnd)!!
        assertEquals(0f, marker.xRatio, 0.0001f)
        assertTrue(marker.isClampedToWindowStart)
    }

    @Test
    fun mseMarkerPosition_atExactWindowStart_isNotMarkedAsBeforeWindow() {
        val marker = Sparkline.mseMarkerPosition("2026-06-01T10:00:00Z", windowEnd)!!
        assertEquals(0f, marker.xRatio, 0.0001f)
        assertTrue(!marker.isClampedToWindowStart)
    }
}
