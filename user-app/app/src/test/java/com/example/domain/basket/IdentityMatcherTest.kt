package com.example.domain.basket

import com.example.data.ProductEntity
import org.junit.Assert.*
import org.junit.Test

class IdentityMatcherTest {

    @Test
    fun differentSizesAndPackCounts_neverMatchAsIdentical() {
        val milk1L = ProductEntity(id = "1", brand = "Almarai", name = "Almarai Fresh Milk Full Cream 1L", unitSize = 1.0, unitType = "L")
        val milk2L = ProductEntity(id = "2", brand = "Almarai", name = "Almarai Fresh Milk Full Cream 2L", unitSize = 2.0, unitType = "L")
        assertFalse(ProductIdentityMatcher.areExactSameSku(milk1L, milk2L))

        val singleCan = ProductEntity(id = "3", brand = "Coca Cola", name = "Coca Cola Can 330ml", unitSize = 330.0, unitType = "ml", packCount = 1)
        val sixPack = ProductEntity(id = "4", brand = "Coca Cola", name = "Coca Cola Can 330ml (Pack of 6)", unitSize = 330.0, unitType = "ml", packCount = 6)
        assertFalse(ProductIdentityMatcher.areExactSameSku(singleCan, sixPack))

        val eggs30 = ProductEntity(id = "5", brand = "Al Jazira", name = "Fresh Eggs Medium 30 pcs", unitSize = 30.0, unitType = "pcs")
        val eggs12 = ProductEntity(id = "6", brand = "Al Jazira", name = "Fresh Eggs Medium 12 pcs", unitSize = 12.0, unitType = "pcs")
        assertFalse(ProductIdentityMatcher.areExactSameSku(eggs30, eggs12))
    }

    @Test
    fun variantDifferences_neverAutoMerge() {
        val fullCream = ProductEntity(id = "1", brand = "Almarai", name = "Almarai Fresh Milk Full Cream 1L", unitSize = 1.0, unitType = "L")
        val longLife = ProductEntity(id = "2", brand = "Almarai", name = "Almarai Long Life Milk 1L", unitSize = 1.0, unitType = "L")
        assertFalse(ProductIdentityMatcher.areExactSameSku(fullCream, longLife))

        val regular = ProductEntity(id = "3", brand = "Coca Cola", name = "Coca Cola Can 330ml", unitSize = 330.0, unitType = "ml")
        val sugarFree = ProductEntity(id = "4", brand = "Coca Cola", name = "Coca Cola Sugar Free Can 330ml", unitSize = 330.0, unitType = "ml")
        assertFalse(ProductIdentityMatcher.areExactSameSku(regular, sugarFree))
    }

    @Test
    fun gVsKgAndMlVsL_normalizeEquivalentlyForSamePack() {
        val chicken1kg = ProductEntity(id = "1", brand = "Al Rawdah", name = "Al Rawdah Fresh Chicken 1kg", unitSize = 1.0, unitType = "kg")
        val chicken1000g = ProductEntity(id = "2", brand = "Al Rawdah", name = "Al Rawdah Fresh Chicken 1000g", unitSize = 1000.0, unitType = "g")
        assertTrue(ProductIdentityMatcher.areExactSameSku(chicken1kg, chicken1000g))

        val milk1000ml = ProductEntity(id = "3", brand = "Al Ain", name = "Al Ain Fresh Laban 1000ml", unitSize = 1000.0, unitType = "ml")
        val milk1L = ProductEntity(id = "4", brand = "Al Ain", name = "Al Ain Fresh Laban 1L", unitSize = 1.0, unitType = "L")
        assertTrue(ProductIdentityMatcher.areExactSameSku(milk1000ml, milk1L))
    }

    @Test
    fun arabicCuratedAliasesMatchExactPackWhileFuzzyCandidatesDoNotAutoMerge() {
        val arMilk = RequestedItem(
            lineId = "l1",
            name = "حليب المراعي طازج 2L",
            identity = ProductIdentity(brand = "Almarai", name = "حليب المراعي طازج 2L", baseQuantity = 2000.0, baseUnit = "ml", packCount = 1),
            requestedQuantity = 1
        )
        val enMilk = ProductEntity(id = "m1", brand = "Almarai", name = "Almarai Fresh Milk 2L", unitSize = 2.0, unitType = "L")
        assertEquals("EXACT_ALIAS_AND_PACK", ProductIdentityMatcher.matchReason(arMilk, enMilk))

        // Fuzzy different brand should NOT auto-merge
        val otherBrand = ProductEntity(id = "m2", brand = "Lacnor", name = "Lacnor Fresh Milk 2L", unitSize = 2.0, unitType = "L")
        assertNull(ProductIdentityMatcher.matchReason(arMilk, otherBrand))
    }
}
