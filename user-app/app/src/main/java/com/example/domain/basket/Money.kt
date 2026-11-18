package com.example.domain.basket

import kotlin.math.roundToLong

/**
 * Integer-fils money utilities (1 AED = 100 fils) to avoid binary floating-point rounding drift.
 */
object Money {
    fun fromAed(aed: Double): Long {
        if (!aed.isFinite() || aed <= 0.0) return 0L
        return (aed * 100.0).roundToLong()
    }

    fun toAed(fils: Long): Double = fils / 100.0

    fun formatAed(fils: Long): String {
        val negative = fils < 0L
        val abs = if (negative) -fils else fils
        val whole = abs / 100L
        val decimals = abs % 100L
        val sign = if (negative) "-" else ""
        return "${sign}AED $whole.${decimals.toString().padStart(2, '0')}"
    }
}
