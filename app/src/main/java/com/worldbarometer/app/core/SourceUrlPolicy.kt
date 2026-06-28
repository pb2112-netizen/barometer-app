package com.worldbarometer.app.core

import android.net.Uri

/** WB-047: whitelist hostów wydawców — sync z DOZWOLONE_HOSTY_ZRODEL w silnik.py. */
object SourceUrlPolicy {
    private const val MAX_URL_LENGTH = 2048

    private val ALLOWED_HOST_SUFFIXES = listOf(
        "bbc.co.uk",
        "bbc.com",
        "aljazeera.com",
        "theguardian.com",
    )

    fun isAllowed(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("https://", ignoreCase = true)) return false
        if (trimmed.length > MAX_URL_LENGTH) return false
        val host = Uri.parse(trimmed).host?.lowercase() ?: return false
        return ALLOWED_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }
}

fun String.isAllowedSourceUrl(): Boolean = SourceUrlPolicy.isAllowed(this)
