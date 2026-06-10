package com.worldbarometer.app.core

import androidx.compose.ui.graphics.Color

/**
 * Paleta = makiety/paleta.json. Tylko kolory/gradienty (bez ikon — decyzja MVP).
 * Współdzielona przez ekran główny (Compose) i widget (Glance) — oba używają
 * androidx.compose.ui.graphics.Color.
 */
object LevelPalette {

    /** Kolor bazowy poziomu (wielka cyfra, pasek 0–10, kropka etykiety, badge eventu). */
    fun color(level: Level): Color = when (level) {
        Level.STABLE -> Color(0xFF15803D)
        Level.LOW -> Color(0xFF65A30D)
        Level.ELEVATED -> Color(0xFFD97706)
        Level.HIGH -> Color(0xFFEA580C)
        Level.CRITICAL -> Color(0xFFB91C1C)
    }

    /** Gradient 2-stopniowy (tło widgetu). first = ciemniejszy, second = jaśniejszy. */
    fun gradient(level: Level): Pair<Color, Color> = when (level) {
        Level.STABLE -> Color(0xFF064E3B) to Color(0xFF16A34A)
        Level.LOW -> Color(0xFF3F6212) to Color(0xFF84CC16)
        Level.ELEVATED -> Color(0xFF92400E) to Color(0xFFF59E0B)
        Level.HIGH -> Color(0xFF9A3412) to Color(0xFFFB923C)
        Level.CRITICAL -> Color(0xFF7F1D1D) to Color(0xFFEF4444)
    }

    /** Kolor strzałki trendu (niezależny od poziomu). */
    fun trendColor(trend: Trend): Color = when (trend) {
        Trend.RISING -> Color(0xFFDC2626)
        Trend.FALLING -> Color(0xFF16A34A)
        Trend.STABLE -> Color(0xFF94A3B8)
    }

    /** Kolor badge dla eventu liczony z jego oceny. */
    fun eventBadgeColor(score: Double): Color = color(Level.fromScore(score))
}

/** Kolory neutralne (tło/tekst/karta/obrys) dla trybu light i dark. */
object NeutralPalette {
    object Light {
        val background = Color(0xFFFCF8FA)
        val text = Color(0xFF1B1B1D)
        val textSecondary = Color(0xFF45464D)
        val card = Color(0xFFFFFFFF)
        val outline = Color(0xFFE2E8F0)
    }

    object Dark {
        val background = Color(0xFF020617)
        val text = Color(0xFFF3F0F2)
        val textSecondary = Color(0xFF94A3B8)
        val card = Color(0xFF1E293B)
        val outline = Color(0xFF334155)
    }
}
