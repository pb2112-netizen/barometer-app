package com.worldbarometer.app.core

/**
 * Pasmo istotności liczone WYŁĄCZNIE ze score (WB-014). Pole level_label z JSON jest
 * legacy (silnik utrzymuje je dla apek <= v0.6.x) i NIE jest używane w prezentacji —
 * etykiety składa lokalnie słownik (pasmo x ton) w LevelPalette.labelRes().
 */
enum class Level {
    STABLE,    // 1.0–2.9 (Calm)
    LOW,       // 3.0–4.9 (Quiet)
    ELEVATED,  // 5.0–6.9
    HIGH,      // 7.0–8.9
    CRITICAL;  // 9.0–10.0

    /** Pas spokoju (score < 5) — brand teal/sage, bez tonowania (WB-014 §3). */
    val isCalmBand: Boolean get() = this == STABLE || this == LOW

    companion object {
        /** Wyliczenie pasma z oceny 1.0–10.0 (progi bez zmian od MVP). */
        fun fromScore(score: Double): Level = when {
            score < 3.0 -> STABLE
            score < 5.0 -> LOW
            score < 7.0 -> ELEVATED
            score < 9.0 -> HIGH
            else -> CRITICAL
        }
    }
}

enum class Trend {
    RISING, FALLING, STABLE;

    companion object {
        fun fromString(value: String?): Trend = when (value?.trim()?.lowercase()) {
            "rising" -> RISING
            "falling" -> FALLING
            else -> STABLE
        }
    }
}
