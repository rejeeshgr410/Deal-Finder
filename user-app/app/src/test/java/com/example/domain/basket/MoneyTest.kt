package com.example.domain.basket

import org.junit.Assert.*
import org.junit.Test

class MoneyTest {

    @Test
    fun fromAed_handlesDecimalsAndInvalidValues() {
        assertEquals(1050L, Money.fromAed(10.50))
        assertEquals(999L, Money.fromAed(9.99))
        assertEquals(0L, Money.fromAed(0.0))
        assertEquals(0L, Money.fromAed(-5.0))
        assertEquals(0L, Money.fromAed(Double.NaN))
    }

    @Test
    fun formatAed_formatsExactFilsWithoutRoundingDrift() {
        assertEquals("AED 0.00", Money.formatAed(0L))
        assertEquals("AED 9.05", Money.formatAed(905L))
        assertEquals("AED 47.73", Money.formatAed(999L + 975L + 2799L))
        assertEquals("-AED 11.20", Money.formatAed(-1120L))
    }
}
