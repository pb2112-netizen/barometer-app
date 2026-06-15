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
    /** WB-030: ISO UTC when current top_events story first entered history. */
    @SerialName("events_anchor_at") val eventsAnchorAt: String? = null,
)

@Serializable
data class ScoreHistoryPoint(
    @SerialName("t") val timestamp: String = "",
    @SerialName("s") val score: Double = 0.0,
)

@Serializable
data class TopEvent(
    val title: String = "",
    val summary: String = "",
    val score: Double = 0.0,
    /** WB-013: sentyment eventu (negative/positive/neutral); null = stary cache -> NEUTRAL. */
    val sentiment: String? = null,
    val nowosc: String? = null,
    val category: String? = null,
    val sources: List<String> = emptyList(),
)
