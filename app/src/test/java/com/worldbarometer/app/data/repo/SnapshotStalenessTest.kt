package com.worldbarometer.app.data.repo

import com.worldbarometer.app.data.model.BarometerData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * WB-070: „nieaktualne" musi wynikać z dwóch zegarów — pobrania przez apkę ORAZ publikacji
 * przez silnik. Regresja z audytu 2026-07-27: przy zatrzymanym silniku apka pobierała ten
 * sam stary JSON co godzinę i nigdy nie pokazała bannera.
 */
class SnapshotStalenessTest {

    private val now = Instant.parse("2026-07-27T12:00:00Z").toEpochMilli()

    private fun snapshot(updatedAt: String?, fetchedAgoMinutes: Long) =
        BarometerRepository.Snapshot(
            data = BarometerData(updatedAt = updatedAt),
            fetchedAtMillis = now - fetchedAgoMinutes * 60_000L,
            lensId = "pl",
        )

    @Test
    fun freshFetchOfFreshPublication_isNotStale() {
        assertFalse(snapshot("2026-07-27T11:30:00Z", fetchedAgoMinutes = 5).isStale(now))
    }

    @Test
    fun freshFetchOfElevenDayOldPublication_isStale() {
        // Dokładnie scenariusz z pauzy silnika: sieć działa, dane sprzed 11 dni.
        assertTrue(snapshot("2026-07-16T12:41:12Z", fetchedAgoMinutes = 5).isStale(now))
    }

    @Test
    fun oldFetch_isStale_evenWhenPublicationTimestampLooksFresh() {
        assertTrue(snapshot("2026-07-27T11:55:00Z", fetchedAgoMinutes = 240).isStale(now))
    }

    @Test
    fun missingUpdatedAt_fallsBackToFetchClockOnly() {
        assertFalse(snapshot(null, fetchedAgoMinutes = 5).isStale(now))
        assertTrue(snapshot(null, fetchedAgoMinutes = 240).isStale(now))
    }

    @Test
    fun unparsableUpdatedAt_doesNotMarkFreshFetchAsStale() {
        assertFalse(snapshot("nie-iso", fetchedAgoMinutes = 5).isStale(now))
    }

    @Test
    fun neverFetched_isStale() {
        val never = BarometerRepository.Snapshot(
            data = BarometerData(updatedAt = "2026-07-27T11:55:00Z"),
            fetchedAtMillis = 0L,
            lensId = "pl",
        )
        assertTrue(never.isStale(now))
    }
}
