package com.worldbarometer.app.core

/**
 * Poziom zagrożenia spójny z etykietami silnika (level_label) i zakresami score.
 * Źródło zakresów: makiety/paleta.json + START_TUTAJ.md §4.
 */
enum class Level(val label: String) {
    STABLE("Stable"),
    LOW("Low"),
    ELEVATED("Elevated"),
    HIGH("High"),
    CRITICAL("Critical");

    companion object {
        /** Dopasowanie po etykiecie z JSON (priorytet), niewrażliwe na wielkość liter. */
        fun fromLabel(label: String?): Level? =
            label?.let { l -> entries.firstOrNull { it.label.equals(l.trim(), ignoreCase = true) } }

        /** Wyliczenie poziomu z oceny 1.0–10.0 (fallback, gdy brak/nieznana etykieta). */
        fun fromScore(score: Double): Level = when {
            score < 3.0 -> STABLE
            score < 5.0 -> LOW
            score < 7.0 -> ELEVATED
            score < 9.0 -> HIGH
            else -> CRITICAL
        }

        /** Preferuj etykietę z backendu; gdy jej nie ma — policz ze score. */
        fun resolve(label: String?, score: Double): Level = fromLabel(label) ?: fromScore(score)
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
