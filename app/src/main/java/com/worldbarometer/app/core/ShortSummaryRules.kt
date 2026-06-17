package com.worldbarometer.app.core

/** WB-033: blocklista meta short_summary — sync z _META_SHORT_SUMMARY_PHRASES w silnik.py */
object ShortSummaryRules {
    private const val QUIET_NEWS_CYCLE = "Quiet news cycle"

    private val META_PHRASES = listOf(
        "background noise", "ongoing background", "no new shock", "no new change",
        "nothing significant", "no significant", "still calm", "still quiet",
        "without change", "unchanged", "no change", "same as before",
        "quiet news", "news cycle", "calm period", "routine cycle", "status quo",
    )

    fun isMetaShortSummary(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return true
        if (t.equals(QUIET_NEWS_CYCLE, ignoreCase = true)) return false
        val lower = t.lowercase()
        return META_PHRASES.any { phrase -> lower.contains(phrase) }
    }

    /** Summary do pokazania użytkownikowi (nie meta, nie pusty). Quiet news cycle = false. */
    fun isDisplayableShortSummary(text: String): Boolean {
        val t = text.trim()
        if (t.isBlank()) return false
        if (t.equals(QUIET_NEWS_CYCLE, ignoreCase = true)) return false
        if (isMetaShortSummary(t)) return false
        return true
    }
}
