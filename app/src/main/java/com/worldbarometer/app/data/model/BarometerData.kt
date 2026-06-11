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
    @SerialName("short_summary") val shortSummary: String = "",
    val rationale: String = "",
    @SerialName("top_events") val topEvents: List<TopEvent> = emptyList(),
    val tryb: String = "",
    @SerialName("level_label") val levelLabel: String? = null,
    val trend: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("liczba_naglowkow") val headlineCount: Int = 0,
    @SerialName("lens_id") val lensId: String? = null,
    @SerialName("lens_name_en") val lensNameEn: String? = null,
)

@Serializable
data class TopEvent(
    val title: String = "",
    val summary: String = "",
    val score: Double = 0.0,
    val nowosc: String? = null,
    val category: String? = null,
    val sources: List<String> = emptyList(),
)
