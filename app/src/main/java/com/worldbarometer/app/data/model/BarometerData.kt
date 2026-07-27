package com.worldbarometer.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Kontrakt danych = barometer.json (patrz START_TUTAJ.md §3).
 * Język treści: angielski. Domyślne wartości chronią przed brakiem pól w przyszłych wersjach.
 */
@Serializable
data class BarometerData(
    @SerialName("global_score") val globalScore: Double = 0.0,
    /** WB-013: globalny ton lensu (negative/positive/neutral); null = stary cache -> NEUTRAL. */
    @SerialName("tone") val tone: String? = null,
    @SerialName("short_summary") val shortSummary: String = "",
    val rationale: String = "",
    @SerialName("top_events") val topEvents: List<TopEvent> = emptyList(),
    val tryb: String = "",
    /** Legacy (WB-014): nieużywane w prezentacji — etykiety składa apka ze score + tone. */
    @SerialName("level_label") val levelLabel: String? = null,
    val trend: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("liczba_naglowkow") val headlineCount: Int = 0,
    @SerialName("lens_id") val lensId: String? = null,
    @SerialName("lens_name_en") val lensNameEn: String? = null,
    /** WB-003/WB-029: rolling 48h history of global_score (sorted ascending by t). */
    @SerialName("score_history") val scoreHistory: List<ScoreHistoryPoint> = emptyList(),
    /** WB-060: MSE = highest peak_score within a 24h window (replaces events_anchor_at). */
    @SerialName("most_significant_event") val mostSignificantEvent: MostSignificantEvent? = null,
)

/** WB-060: "Most significant event" — highest peak_score among topics detected within 24h. */
@Serializable
data class MostSignificantEvent(
    val label: String = "",
    /**
     * WB-064: MSE's OWN impact description, frozen together with the peak (engine
     * `peak_summary`). Before WB-064 the app had nothing to show but the lens-level
     * `rationale`, which explains the CURRENT cycle's dominant event — a different story
     * whenever the champion sits outside the visible top-3. Empty on pre-WB-064 cache.
     */
    val summary: String = "",
    /** WB-064: champion's RSS title — lets the UI link MSE back to its card in topEvents. */
    val title: String = "",
    val score: Double = 0.0,
    val sentiment: String? = null,
    /** First time the topic was ever detected (WB-059) — drives the "5h ago" marker. */
    @SerialName("detected_at") val detectedAt: String? = null,
    /** WB-068: moment of the peak, inside the 24h window; optional tick on the chart. */
    @SerialName("peak_at") val peakAt: String? = null,
)

@Serializable
data class ScoreHistoryPoint(
    @SerialName("t") val timestamp: String = "",
    @SerialName("s") val score: Double = 0.0,
)

@Serializable
data class SourceLink(
    val name: String = "",
    val url: String = "",
)

@Serializable
data class TopEvent(
    val title: String = "",
    /** WB-061: krótka etykieta EN per event (to samo brzmienie, co MSE dla championa). */
    val label: String = "",
    val summary: String = "",
    val score: Double = 0.0,
    /** WB-013: sentyment eventu (negative/positive/neutral); null = stary cache -> NEUTRAL. */
    val sentiment: String? = null,
    val nowosc: String? = null,
    val category: String? = null,
    val sources: List<String> = emptyList(),
    /** WB-047: klikalne linki do artykułów wydawców (deterministycznie z RSS w silniku). */
    @SerialName("source_links") val sourceLinks: List<SourceLink> = emptyList(),
    /** WB-059: ISO UTC pierwszego wykrycia tematu w top_events (silnik, deterministyczne). */
    @SerialName("detected_at") val detectedAt: String? = null,
)
