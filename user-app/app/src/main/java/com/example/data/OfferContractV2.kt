package com.example.data

import kotlin.math.roundToLong

object OfferContractV2 {
    const val UAE_LAT_MIN = 22.5
    const val UAE_LAT_MAX = 26.5
    const val UAE_LON_MIN = 51.0
    const val UAE_LON_MAX = 56.6

    private val VERIFIED_COORD_SOURCES = setOf(
        "verified_branch_source",
        "merchant_entered",
        "official_feed"
    )

    fun aedToFils(amountAed: Double): Long {
        if (!amountAed.isFinite() || amountAed < 0.0) return 0L
        return (amountAed * 100.0).roundToLong()
    }

    fun filsToAed(amountFils: Long): Double = amountFils / 100.0

    fun formatAedFromFils(amountFils: Long): String {
        val negative = amountFils < 0L
        val absFils = if (negative) -amountFils else amountFils
        val whole = absFils / 100L
        val frac = absFils % 100L
        val prefix = if (negative) "-" else ""
        return "${prefix}AED $whole.${frac.toString().padStart(2, '0')}"
    }

    fun isValidUaeCoordinate(latitude: Double, longitude: Double): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in UAE_LAT_MIN..UAE_LAT_MAX && longitude in UAE_LON_MIN..UAE_LON_MAX
    }

    fun isVerifiedCoordSource(coordSource: String): Boolean {
        return coordSource.trim() in VERIFIED_COORD_SOURCES
    }

    fun isValidGtin(raw: String?): Boolean {
        val digits = raw?.trim().orEmpty()
        if (!digits.matches(Regex("^(\\d{8}|\\d{12}|\\d{13}|\\d{14})$"))) return false
        if (digits.all { it == '0' }) return false
        val body = digits.dropLast(1)
        val expectedCheck = digits.last().digitToInt()
        var sum = 0
        for (i in body.indices) {
            val digit = body[body.length - 1 - i].digitToInt()
            sum += digit * if (i % 2 == 0) 3 else 1
        }
        val computed = (10 - (sum % 10)) % 10
        return computed == expectedCheck
    }
}

fun BranchEntity.isEligibleForDistance(): Boolean {
    if (!OfferContractV2.isValidUaeCoordinate(latitude, longitude)) return false
    if (coordsVerified) return true
    return OfferContractV2.isVerifiedCoordSource(coordSource)
}

fun ProductEntity.effectiveAmountMinor(): Long {
    val explicit = amountMinor
    if (explicit != null && explicit > 0L) return explicit
    return OfferContractV2.aedToFils(currentPrice)
}

fun ProductEntity.effectiveComparisonPriceMinor(): Long? {
    val explicit = comparisonPriceMinor
    if (explicit != null && explicit > effectiveAmountMinor()) return explicit
    val legacy = oldPrice ?: return null
    val legacyMinor = OfferContractV2.aedToFils(legacy)
    return if (legacyMinor > effectiveAmountMinor()) legacyMinor else null
}

fun ProductEntity.isCurrentlyEligible(
    nowMs: Long = System.currentTimeMillis(),
    includeDemo: Boolean = false
): Boolean {
    if (!includeDemo && (isDemo || sourceType == "demo")) return false
    if (!status.equals("published", ignoreCase = true)) return false
    if (availability == "out_of_stock") return false
    if (effectiveAmountMinor() <= 0L) return false
    if (validFrom > 0L && validFrom > nowMs) return false
    if (schemaVersion >= 2 && validTo <= 0L) return false
    if (validTo > 0L && validTo <= nowMs) return false
    return true
}
