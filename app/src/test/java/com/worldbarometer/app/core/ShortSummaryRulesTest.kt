package com.worldbarometer.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortSummaryRulesTest {

    @Test
    fun isDisplayableShortSummary_falseForBlank() {
        assertFalse(ShortSummaryRules.isDisplayableShortSummary(""))
        assertFalse(ShortSummaryRules.isDisplayableShortSummary("   "))
    }

    @Test
    fun isDisplayableShortSummary_falseForQuietNewsCycle() {
        assertFalse(ShortSummaryRules.isDisplayableShortSummary("Quiet news cycle"))
        assertFalse(ShortSummaryRules.isDisplayableShortSummary("quiet news cycle"))
    }

    @Test
    fun isDisplayableShortSummary_falseForMetaPhrases() {
        assertFalse(ShortSummaryRules.isDisplayableShortSummary("Still calm with no new shock"))
        assertFalse(ShortSummaryRules.isDisplayableShortSummary("Status quo unchanged"))
        assertTrue(ShortSummaryRules.isMetaShortSummary("No significant developments today"))
    }

    @Test
    fun isDisplayableShortSummary_trueForRealHeadline() {
        assertTrue(ShortSummaryRules.isDisplayableShortSummary("Major flooding disrupts transport across the region"))
    }
}
