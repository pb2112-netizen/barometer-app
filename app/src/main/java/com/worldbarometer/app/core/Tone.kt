package com.worldbarometer.app.core

/**
 * Oś sentymentu (WB-013/WB-014): KIERUNEK zmiany, niezależny od istotności (score).
 * Źródło: pole "tone" (globalnie per lens) i "sentiment" (per event) z barometer.json.
 */
enum class Tone {
    NEGATIVE, POSITIVE, NEUTRAL;

    companion object {
        /**
         * Twarda zasada kompatybilności: null / pusta / nieznana wartość -> NEUTRAL
         * (stary cache bez pól WB-013 musi działać bez crasha i bez pustych etykiet).
         */
        fun fromString(value: String?): Tone = when (value?.trim()?.lowercase()) {
            "negative" -> NEGATIVE
            "positive" -> POSITIVE
            else -> NEUTRAL
        }
    }
}
