package com.worldbarometer.app.core

/**
 * Hardcoded lista 5 lensow (WB-008 MVP). URL pattern: barometer_{id}.json.
 * Future-proof: opcjonalny fetch manifest.json — poza zakresem MVP.
 */
object LensCatalog {

    data class Lens(val id: String, val nameEn: String)

    val ALL: List<Lens> = listOf(
        Lens("pl", "Poland"),
        Lens("ro", "Romania"),
        Lens("pt", "Portugal"),
        Lens("ua", "Ukraine"),
        Lens("us", "United States"),
    )

    const val DEFAULT_LENS_ID = "pl"

    fun nameFor(id: String): String =
        ALL.find { it.id == id }?.nameEn ?: "Poland"

    fun isValid(id: String): Boolean =
        ALL.any { it.id == id }

    fun sanitize(id: String): String =
        if (isValid(id)) id else DEFAULT_LENS_ID
}
