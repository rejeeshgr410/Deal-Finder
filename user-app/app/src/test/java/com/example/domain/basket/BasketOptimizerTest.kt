package com.example.domain.basket

import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import org.junit.Assert.*
import org.junit.Test

class BasketOptimizerTest {

    private val now = 1_700_000_000_000L

    private val storeA = RetailerEntity(id = "store-a", name = "Carrefour", status = "published", emirates = "Dubai")
    private val storeB = RetailerEntity(id = "store-b", name = "Lulu", status = "published", emirates = "Dubai")
    private val storeC = RetailerEntity(id = "store-c", name = "Nesto", status = "published", emirates = "Dubai")

    private fun prod(id: String, storeId: String, masterId: String, name: String, price: Double, unitSize: Double = 1.0, unitType: String = "unit"): ProductEntity {
        return ProductEntity(
            id = id,
            retailerId = storeId,
            masterProductId = masterId,
            name = name,
            currentPrice = price,
            amountMinor = Money.fromAed(price),
            unitSize = unitSize,
            unitType = unitType,
            validFrom = now - 10_000,
            validTo = now + 86_400_000,
            status = "published",
            verificationLabel = "admin_reviewed"
        )
    }

    @Test
    fun duplicateItemNamesWithDifferentLineIds_bothPreservedAndSummed() {
        val products = listOf(
            prod("p1", "store-a", "m-milk", "Fresh Milk 2L", 10.00, 2.0, "L")
        )
        val items = listOf(
            RequestedItem("line-1", "Fresh Milk 2L", ProductIdentity(masterProductId = "m-milk", baseQuantity = 2000.0, baseUnit = "ml"), 1),
            RequestedItem("line-2", "Fresh Milk 2L", ProductIdentity(masterProductId = "m-milk", baseQuantity = 2000.0, baseUnit = "ml"), 2)
        )
        val res = BasketOptimizer.optimize(
            BasketRequest(items = items, selectedEmirate = "Dubai", nowMs = now),
            products,
            listOf(storeA)
        )
        val one = res.bestOneStore!!
        assertTrue(one.complete)
        assertEquals(2, one.itemCoverage)
        assertEquals(3000L, one.totalFils)
    }

    @Test
    fun missingOfferMakesBasketIncompleteWithNullTotalAndKnownSubtotal() {
        val products = listOf(
            prod("p1", "store-a", "m-milk", "Fresh Milk 2L", 10.00, 2.0, "L")
        )
        val items = listOf(
            RequestedItem("l1", "Fresh Milk 2L", ProductIdentity(masterProductId = "m-milk", baseQuantity = 2000.0, baseUnit = "ml"), 1),
            RequestedItem("l2", "Rare Saffron 10g", ProductIdentity(masterProductId = "m-saffron", baseQuantity = 10.0, baseUnit = "g"), 1)
        )
        val res = BasketOptimizer.optimize(
            BasketRequest(items = items, selectedEmirate = "Dubai", nowMs = now),
            products,
            listOf(storeA)
        )
        val one = res.bestOneStore!!
        assertFalse(one.complete)
        assertNull(one.totalFils)
        assertEquals(1000L, one.verifiedItemsSubtotalFils)
        assertEquals(1, one.unpricedLines.size)
        assertEquals("Rare Saffron 10g", one.unpricedLines[0].itemName)
        assertNull(res.bestTwoStore) // Only 1 store exists
    }

    @Test
    fun tenCuratedBasketScenarios_computeExactDeterministicTotalsAndSavingsBaseline() {
        val catalog = listOf(
            // Milk 2L: A=11.00, B=10.50, C=9.75
            prod("m-a", "store-a", "m-milk", "Almarai Milk 2L", 11.00, 2.0, "L"),
            prod("m-b", "store-b", "m-milk", "Almarai Milk 2L", 10.50, 2.0, "L"),
            prod("m-c", "store-c", "m-milk", "Almarai Milk 2L", 9.75, 2.0, "L"),
            // Rice 5kg: A=32.50, B=29.90, C=27.99
            prod("r-a", "store-a", "m-rice", "Sunwhite Rice 5kg", 32.50, 5.0, "kg"),
            prod("r-b", "store-b", "m-rice", "Sunwhite Rice 5kg", 29.90, 5.0, "kg"),
            prod("r-c", "store-c", "m-rice", "Sunwhite Rice 5kg", 27.99, 5.0, "kg"),
            // Oil 2x1.5L: A=24.00 (cheapest!), B=26.90, C=29.50
            prod("o-a", "store-a", "m-oil", "Noor Oil 2x1.5L", 24.00, 1500.0, "ml").copy(packCount = 2),
            prod("o-b", "store-b", "m-oil", "Noor Oil 2x1.5L", 26.90, 1500.0, "ml").copy(packCount = 2),
            prod("o-c", "store-c", "m-oil", "Noor Oil 2x1.5L", 29.50, 1500.0, "ml").copy(packCount = 2)
        )

        val basket = listOf(
            RequestedItem("i1", "Almarai Milk 2L", ProductIdentity(masterProductId = "m-milk", baseQuantity = 2000.0, baseUnit = "ml"), 2),
            RequestedItem("i2", "Sunwhite Rice 5kg", ProductIdentity(masterProductId = "m-rice", baseQuantity = 5000.0, baseUnit = "g"), 1),
            RequestedItem("i3", "Noor Oil 2x1.5L", ProductIdentity(masterProductId = "m-oil", baseQuantity = 1500.0, baseUnit = "ml", packCount = 2), 1)
        )

        // 1-store totals:
        // store-a: 2*11.00 + 32.50 + 24.00 = 78.50 (7850 fils)
        // store-b: 2*10.50 + 29.90 + 26.90 = 77.80 (7780 fils)
        // store-c: 2*9.75 + 27.99 + 29.50 = 76.99 (7699 fils) -> best 1-store!
        // 2-store split (store-c + store-a): 2*9.75 + 27.99 + 24.00 = 71.49 (7149 fils) -> 5.50 AED lower!
        val run1 = BasketOptimizer.optimize(
            BasketRequest(items = basket, selectedEmirate = "Dubai", nowMs = now),
            catalog,
            listOf(storeA, storeB, storeC)
        )
        val run2 = BasketOptimizer.optimize(
            BasketRequest(items = basket, selectedEmirate = "Dubai", nowMs = now),
            catalog,
            listOf(storeA, storeB, storeC)
        )
        assertEquals(run1, run2) // Deterministic

        assertEquals(7699L, run1.bestOneStore?.totalFils)
        assertEquals(listOf("store-c"), run1.bestOneStore?.storeIds)
        assertEquals(7149L, run1.bestTwoStore?.totalFils)
        assertEquals(550L, run1.bestTwoStore?.comparisonDeltaVsBestSingleStoreFils)
        assertTrue(run1.bestTwoStore?.savingsBaselineDisclosure?.contains("AED 5.50") == true)
        assertTrue(run1.bestTwoStore?.savingsBaselineDisclosure?.contains("travel cost not included") == true)

        // With known delivery fee of AED 10.00 (1000 fils) per store:
        // 1-store total: 7699 + 1000 = 8699; 2-store total: 7149 + 2000 = 9149 -> 2-store saves 0!
        val withFees = BasketOptimizer.optimize(
            BasketRequest(
                items = basket,
                selectedEmirate = "Dubai",
                knownFeesByStoreFils = mapOf("store-a" to 1000L, "store-b" to 1000L, "store-c" to 1000L),
                nowMs = now
            ),
            catalog,
            listOf(storeA, storeB, storeC)
        )
        assertEquals(8699L, withFees.bestOneStore?.totalFils)
        assertEquals(9149L, withFees.bestTwoStore?.totalFils)
        assertEquals(0L, withFees.bestTwoStore?.comparisonDeltaVsBestSingleStoreFils)
        assertTrue(withFees.bestTwoStore!!.savingsBaselineDisclosure.contains("No savings"))
    }

    @Test
    fun storesOverCap_setsTruncationFlagAndDisclosure() {
        val manyStores = (1..45).map { RetailerEntity(id = "s-${it.toString().padStart(2, '0')}", name = "Store $it", status = "published") }
        val res = BasketOptimizer.optimize(
            BasketRequest(
                items = listOf(RequestedItem("l1", "Milk", ProductIdentity(masterProductId = "m1"), 1)),
                selectedEmirate = "Dubai",
                nowMs = now
            ),
            listOf(prod("p1", "s-01", "m1", "Milk 1L", 5.0, 1.0, "L")),
            manyStores
        )
        assertTrue(res.storesTruncated)
        assertEquals(BasketOptimizer.MAX_CANDIDATE_STORES, res.candidateStoreCount)
        assertTrue(res.coverageDisclosure.contains("capped at ${BasketOptimizer.MAX_CANDIDATE_STORES}"))
    }
}
