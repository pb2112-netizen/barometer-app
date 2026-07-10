package com.worldbarometer.app.core

import com.worldbarometer.app.data.model.BarometerData
import com.worldbarometer.app.data.model.ScoreHistoryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ContentSafetyTest {

    @Test
    fun sanitized_clampsGlobalScoreToScale() {
        val data = BarometerData(globalScore = 15.0).sanitized()
        assertEquals(10.0, data.globalScore, 0.001)

        val low = BarometerData(globalScore = -3.0).sanitized()
        assertEquals(1.0, low.globalScore, 0.001)

        val nan = BarometerData(globalScore = Double.NaN).sanitized()
        assertEquals(1.0, nan.globalScore, 0.001)
    }

    @Test
    fun sanitized_takeLastKeepsNewest72HistoryPoints() {
        val base = Instant.parse("2026-06-01T00:00:00Z")
        val history = (0 until 80).map { hour ->
            ScoreHistoryPoint(
                timestamp = base.plusSeconds(hour * 3600L).toString(),
                score = hour.toDouble(),
            )
        }
        val sanitized = BarometerData(scoreHistory = history).sanitized()

        assertEquals(72, sanitized.scoreHistory.size)
        assertEquals(base.plusSeconds(8 * 3600L).toString(), sanitized.scoreHistory.first().timestamp)
        assertEquals(base.plusSeconds(79 * 3600L).toString(), sanitized.scoreHistory.last().timestamp)
        assertTrue(sanitized.scoreHistory.zipWithNext().all { (a, b) -> a.timestamp <= b.timestamp })
    }

    @Test
    fun sanitized_clampsHistoryScores() {
        val history = listOf(
            ScoreHistoryPoint(timestamp = "2026-06-01T10:00:00Z", score = 99.0),
            ScoreHistoryPoint(timestamp = "2026-06-01T11:00:00Z", score = Double.NaN),
        )
        val sanitized = BarometerData(scoreHistory = history).sanitized()
        assertEquals(10.0, sanitized.scoreHistory[0].score, 0.001)
        assertEquals(1.0, sanitized.scoreHistory[1].score, 0.001)
    }
}
