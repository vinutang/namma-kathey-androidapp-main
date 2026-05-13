package com.example.myapplication.data.karnataka

/**
 * Compares district labels from the map, JSON seed, and Room without missing matches
 * due to stray spaces or non-breaking spaces.
 */
object DistrictNameMatch {
    fun normalizeWhitespace(s: String): String =
        s.trim().replace('\u00a0', ' ').trim()

    fun equalsCanonical(a: String, b: String): Boolean =
        normalizeWhitespace(a).equals(normalizeWhitespace(b), ignoreCase = true)
}
