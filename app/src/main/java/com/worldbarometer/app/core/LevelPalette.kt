package com.worldbarometer.app.core

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.worldbarometer.app.R

/**
 * JEDNO ŹRÓDŁO PRAWDY dwuosiowego systemu (WB-014 §4.2/§4.3): para (pasmo score x ton)
 * -> etykieta + kolor + opis a11y. Współdzielone przez dashboard (Compose) i widget
 * (Glance + drawable w BarometerWidget.backgroundFor). Zakaz drugiego mapowania.
 *
 * Pas spokoju (score < 5): brand teal/sage NIEZALEŻNIE od tonu — zieleń występuje
 * w UI wyłącznie jako positive >= 5. Negative = dotychczasowa rampa amber->czerwień.
 */
object LevelPalette {

    /** Etykieta poziomu ze słownika (pasmo x ton) — WB-014 §4.2. */
    @StringRes
    fun labelRes(level: Level, tone: Tone): Int = when (level) {
        Level.STABLE -> R.string.level_calm
        Level.LOW -> R.string.level_quiet
        Level.ELEVATED -> when (tone) {
            Tone.NEGATIVE -> R.string.level_elevated
            Tone.NEUTRAL -> R.string.level_active
            Tone.POSITIVE -> R.string.level_promising
        }
        Level.HIGH -> when (tone) {
            Tone.NEGATIVE -> R.string.level_high
            Tone.NEUTRAL -> R.string.level_significant
            Tone.POSITIVE -> R.string.level_positive
        }
        Level.CRITICAL -> when (tone) {
            Tone.NEGATIVE -> R.string.level_severe
            Tone.NEUTRAL -> R.string.level_major
            Tone.POSITIVE -> R.string.level_breakthrough
        }
    }

    /**
     * Kolor bazowy poziomu (wielka cyfra, pasek 0–10, kropka etykiety, badge eventu)
     * — macierz WB-014 §4.3. Negative = dotychczasowe wartości LevelPalette (bez zmian).
     */
    fun color(level: Level, tone: Tone): Color = when (level) {
        Level.STABLE -> BrandPalette.calmTealDeep
        Level.LOW -> Color(0xFF5E8C82)
        Level.ELEVATED -> when (tone) {
            Tone.NEGATIVE, Tone.NEUTRAL -> Color(0xFFD97706)
            Tone.POSITIVE -> Color(0xFF059669)
        }
        Level.HIGH -> when (tone) {
            Tone.NEGATIVE -> Color(0xFFEA580C)
            Tone.NEUTRAL -> Color(0xFFB45309)
            Tone.POSITIVE -> Color(0xFF047857)
        }
        Level.CRITICAL -> when (tone) {
            Tone.NEGATIVE -> Color(0xFFB91C1C)
            Tone.NEUTRAL -> Color(0xFF92400E)
            Tone.POSITIVE -> Color(0xFF15803D)
        }
    }

    /**
     * Szablon contentDescription dla score/poziomu (WB-014 §4.6): ton dokładany tylko
     * w pasmach sygnału (>= 5); pas spokoju bez dopisku tonu. Args: %1$s score, %2$s etykieta.
     */
    @StringRes
    fun scoreDescriptionRes(level: Level, tone: Tone): Int =
        if (level.isCalmBand) {
            R.string.score_description_calm
        } else when (tone) {
            Tone.NEGATIVE -> R.string.score_description_negative
            Tone.POSITIVE -> R.string.score_description_positive
            Tone.NEUTRAL -> R.string.score_description_neutral
        }

    /** Fraza tonu do opisów a11y (widget; spójna ze score_description_*). */
    @StringRes
    fun tonePhraseRes(tone: Tone): Int = when (tone) {
        Tone.NEGATIVE -> R.string.tone_phrase_negative
        Tone.POSITIVE -> R.string.tone_phrase_positive
        Tone.NEUTRAL -> R.string.tone_phrase_neutral
    }

    /**
     * Kolor strzałki trendu (WB-014 §4.4): falling = zawsze spokojny teal (bez wartościowania),
     * rising = per ton, stable = neutralny szary.
     */
    fun trendColor(trend: Trend, tone: Tone, darkTheme: Boolean): Color = when (trend) {
        Trend.FALLING -> if (darkTheme) BrandPalette.calmTeal else BrandPalette.calmTealDeep
        Trend.RISING -> when (tone) {
            Tone.NEGATIVE -> Color(0xFFDC2626)
            Tone.POSITIVE -> Color(0xFF059669)
            Tone.NEUTRAL -> Color(0xFFD97706)
        }
        Trend.STABLE -> Color(0xFF94A3B8)
    }

    /** Badge eventu z pary (score eventu x sentiment eventu) — NIE z globalnego tone (AC-5). */
    fun eventBadgeColor(score: Double, sentiment: Tone): Color =
        color(Level.fromScore(score), sentiment)
}

/** Kolory neutralne (tło/tekst/karta/obrys) dla trybu light i dark. */
object NeutralPalette {
    object Light {
        val background = BrandPalette.warmCream
        val text = Color(0xFF1B1B1D)
        val textSecondary = Color(0xFF45464D)
        val card = BrandPalette.warmCard
        val outline = BrandPalette.warmSand
    }

    object Dark {
        val background = Color(0xFF0C1218)
        val text = Color(0xFFF3F0F2)
        val textSecondary = Color(0xFF94A3B8)
        val card = Color(0xFF1A2229)
        val outline = Color(0xFF3D454C)
    }
}
