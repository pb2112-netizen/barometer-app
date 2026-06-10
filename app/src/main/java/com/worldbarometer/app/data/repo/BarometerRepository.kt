package com.worldbarometer.app.data.repo

import com.worldbarometer.app.core.Level
import com.worldbarometer.app.core.Trend
import com.worldbarometer.app.core.sanitized
import com.worldbarometer.app.data.local.BarometerStore
import com.worldbarometer.app.data.model.BarometerData
import com.worldbarometer.app.data.remote.BarometerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Łączy sieć (BarometerApi) z lokalnym cache (BarometerStore).
 * - refresh(): pobiera świeże dane i zapisuje do cache.
 * - observe(): strumień ostatniego znanego wyniku (zasila UI i widget; działa offline).
 */
class BarometerRepository(
    private val api: BarometerApi,
    private val store: BarometerStore,
    private val json: Json,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Gotowy do prezentacji wynik + metadane (poziom, trend, wiek danych). */
    data class Snapshot(
        val data: BarometerData,
        val fetchedAtMillis: Long,
    ) {
        val level: Level get() = Level.resolve(data.levelLabel, data.globalScore)
        val trend: Trend get() = Trend.fromString(data.trend)

        fun isStale(nowMillis: Long, maxAgeMillis: Long = STALE_AFTER_MILLIS): Boolean =
            fetchedAtMillis <= 0L || (nowMillis - fetchedAtMillis) > maxAgeMillis
    }

    sealed interface RefreshResult {
        data class Success(val snapshot: Snapshot) : RefreshResult
        data class Failure(val cause: Throwable) : RefreshResult
    }

    /** Ostatni znany wynik z cache (null = brak danych jeszcze nie pobranych). */
    fun observe(): Flow<Snapshot?> = store.snapshot.map { cached ->
        cached?.let {
            runCatching {
                val data = json.decodeFromString(BarometerData.serializer(), it.rawJson).sanitized()
                Snapshot(data, it.fetchedAtMillis)
            }.getOrNull()
        }
    }

    suspend fun refresh(): RefreshResult = when (val result = api.fetch()) {
        is BarometerApi.Result.Success -> {
            val millis = now()
            val data = result.data.sanitized()
            val raw = json.encodeToString(BarometerData.serializer(), data)
            store.save(raw, millis)
            RefreshResult.Success(Snapshot(data, millis))
        }

        is BarometerApi.Result.Error -> RefreshResult.Failure(result.cause)
    }

    companion object {
        /**
         * Dane uznajemy za nieaktualne po 90 min. Backend liczy ~raz na godzinę
         * (cron `17 * * * *`), a appka odświeża co ~60 min — 90 min toleruje pełny
         * cykl + jitter WorkManagera, a baner pokazuje dopiero realnie stare dane.
         */
        const val STALE_AFTER_MILLIS: Long = 90L * 60L * 1000L
    }
}
