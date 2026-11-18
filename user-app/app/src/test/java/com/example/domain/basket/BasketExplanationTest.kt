package com.example.domain.basket

import com.example.data.ProductEntity
import org.junit.Assert.*
import org.junit.Test

class BasketExplanationTest {

    @Test
    fun explainDrivers_listsExactLineDifferencesBetweenOneStoreAndTwoStore() {
        val p1 = ProductEntity(id = "p1", retailerId = "s1", name = "Rice 5kg", currentPrice = 32.50, amountMinor = 3250L)
        val p2 = ProductEntity(id = "p2", retailerId = "s2", name = "Rice 5kg", currentPrice = 27.99, amountMinor = 2799L)

        val oneStore = BasketOption(
            storeIds = listOf("s1"),
            storeNames = listOf("Carrefour"),
            pricedLines = listOf(MatchedLine("l1", "Rice 5kg", "s1", "Carrefour", p1, 1, 3250L, "MASTER_ID", null, "admin_reviewed", "Offer price shown; stock not confirmed")),
            unpricedLines = emptyList(),
            complete = true,
            itemCoverage = 1,
            itemCount = 1,
            verifiedItemsSubtotalFils = 3250L,
            knownFeesFils = null,
            totalFils = 3250L,
            unknownFeeDisclosure = "Listed product prices only; travel/delivery fees not included.",
            comparisonDeltaVsBestSingleStoreFils = null,
            savingsBaselineDisclosure = "",
            sourceSnapshotAtMs = 1000L,
            explanatoryWarnings = emptyList()
        )

        val twoStore = oneStore.copy(
            storeIds = listOf("s1", "s2"),
            storeNames = listOf("Carrefour", "Nesto"),
            pricedLines = listOf(MatchedLine("l1", "Rice 5kg", "s2", "Nesto", p2, 1, 2799L, "MASTER_ID", null, "admin_reviewed", "Offer price shown; stock not confirmed")),
            verifiedItemsSubtotalFils = 2799L,
            totalFils = 2799L
        )

        val drivers = BasketExplanations.explainDrivers(oneStore, twoStore)
        assertEquals(1, drivers.size)
        assertTrue(drivers[0].contains("AED 4.51"))
        assertTrue(drivers[0].contains("Nesto"))
    }
}
