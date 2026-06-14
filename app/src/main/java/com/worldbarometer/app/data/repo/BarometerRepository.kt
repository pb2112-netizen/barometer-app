package com.worldbarometer.app.data.repo

import com.worldbarometer.app.core.Level
import com.worldbarometer.app.core.Tone
import com.worldbarometer.app.core.Trend
import com.worldbarometer.app.core.sanitized
import com.worldbarometer.app.data.local.BarometerStore
import com.worldbarometer.app.data.local.SettingsStore
import com.worldbarometer.app.data.model.BarometerData
import com.worldbarometer.app.data.remote.BarometerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Łączy sieć (BarometerApi) z lokalnym cache (BarometerStore) per lens.
 */
class BarometerRepository(
    private val api: BarometerApi,
    private val store: BarometerStore,
    private val settingsStore: SettingsStore,
    private val json: Json,
    private val now: () -> Long = System::currentTimeMillis,
) {

    data class Snapshot(
        val data: BarometerData,
        val fetchedAtMillis: Long,
        val lensId: String,
    ) {
        // WB-014: pasmo wyłącznie ze score (level_label z JSON = legacy, ignorowany).
        val level: Level get() = Level.fromScore(data.globalScore)
        val tone: Tone get() = Tone.fromString(data.tone)
        val trend: Trend get() = Trend.fromString(data.trend)
        val scoreHistory get() = data.scoreHistory

        fun isStale(nowMillis: Long, maxAgeMillis: Long = STALE_AFTER_MILLIS): Boolean =
            fetchedAtMillis <= 0L || (nowMillis - fetchedAtMillis) > maxAgeMillis
    }

    sealed interface RefreshResult {
        data class Success(val snapshot: Snapshot) : RefreshResult
        data class Failure(val cause: Throwable) : RefreshResult
    }

    /** Jednorazowy odczyt cache dla aktywnego lensu — bez wyścigu flatMapLatest (widget). */
    suspend fun currentSnapshot(): Snapshot? {
        val lensId = settingsStore.currentLensId()
        return decodeSnapshot(lensId, store.snapshot(lensId).first())
    }

    private fun decodeSnapshot(lensId: String, cached: BarometerStore.CachedSnapshot?): Snapshot? =
        cached?.let {
            runCatching {
                val data = json.decodeFromString(BarometerData.serializer(), it.rawJson).sanitized()
                Snapshot(data, it.fetchedAtMillis, lensId)
            }.getOrNull()
        }

    fun observe(): Flow<Snapshot?> = settingsStore.lensId.flatMapLatest { lensId ->
        store.snapshot(lensId).map { cached -> decodeSnapshot(lensId, cached) }
    }

    suspend fun refresh(): RefreshResult {
        val lensId = settingsStore.currentLensId()
        return when (val result = api.fetch(lensId)) {
            is BarometerApi.Result.Success -> {
                val millis = now()
                val data = result.data.sanitized()
                val raw = json.encodeToString(BarometerData.serializer(), data)
                store.save(lensId, raw, millis)
                RefreshResult.Success(Snapshot(data, millis, lensId))
            }

            is BarometerApi.Result.Error -> RefreshResult.Failure(result.cause)
        }
    }

    companion object {
        const val STALE_AFTER_MILLIS: Long = 90L * 60L * 1000L
    }
}
