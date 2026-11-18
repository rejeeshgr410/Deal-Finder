package com.example.domain.basket

import com.example.data.ProductEntity
import org.junit.Assert.*
import org.junit.Test

class OfferEligibilityTest {

    private val now = 1_700_000_000_000L

    @Test
    fun expiryBoundaryAndStockTruth_enforcedDeterministically() {
        val baseReq = BasketRequest(items = emptyList(), selectedEmirate = "Dubai", nowMs = now)
        val validOffer = ProductEntity(id = "p1", name = "Milk 1L", currentPrice = 6.50, validFrom = now - 1000, validTo = now + 1000, status = "published")
        assertTrue(OfferEligibility.isProductEligibleForRequest(validOffer, 1, baseReq))

        // Exactly at expiry boundary -> ineligible
        val atBoundary = validOffer.copy(validTo = now)
        assertFalse(OfferEligibility.isProductEligibleForRequest(atBoundary, 1, baseReq))

        // Retailer-confirmed OOS -> excluded
        val oos = validOffer.copy(availability = "out_of_stock")
        assertFalse(OfferEligibility.isProductEligibleForRequest(oos, 1, baseReq))

        // Zero/negative price -> excluded
        assertFalse(OfferEligibility.isProductEligibleForRequest(validOffer.copy(currentPrice = 0.0), 1, baseReq))
        assertFalse(OfferEligibility.isProductEligibleForRequest(validOffer.copy(currentPrice = -2.0), 1, baseReq))
    }

    @Test
    fun loyaltyAndMultibuyConditions_enforcedWithoutIllegalSingleUnitDiscount() {
        val loyaltyDeal = ProductEntity(
            id = "loy1",
            retailerId = "r1",
            name = "Nescafe 200g",
            currentPrice = 19.75,
            loyaltyRequired = true,
            validTo = now + 10_000,
            status = "published"
        )
        val noCardReq = BasketRequest(items = emptyList(), selectedEmirate = "Dubai", hasUserLoyaltyCard = false, nowMs = now)
        val withCardReq = noCardReq.copy(hasUserLoyaltyCard = true)
        assertFalse(OfferEligibility.isProductEligibleForRequest(loyaltyDeal, 1, noCardReq))
        assertTrue(OfferEligibility.isProductEligibleForRequest(loyaltyDeal, 1, withCardReq))

        // Buy 2 Get 1 Free: qty=1 pays 1 unit; qty=3 pays 2 units!
        val bogo = ProductEntity(
            id = "bogo1",
            retailerId = "r1",
            name = "Tea 100s",
            currentPrice = 10.0,
            amountMinor = 1000L,
            buyUnits = 2,
            freeUnits = 1,
            minUnits = 2,
            validTo = now + 10_000,
            status = "published"
        )
        assertFalse(OfferEligibility.isProductEligibleForRequest(bogo, 1, noCardReq))
        assertTrue(OfferEligibility.isProductEligibleForRequest(bogo, 2, noCardReq))
        assertEquals(2000L, OfferEligibility.computeLineSubtotalFils(bogo, 3))
    }

    @Test
    fun conflictingSameStoreOffers_picksLowestDeterministicOffer() {
        val reqItem = RequestedItem(
            lineId = "line-1",
            name = "Almarai Milk 2L",
            identity = ProductIdentity(masterProductId = "m-milk-2l", baseQuantity = 2000.0, baseUnit = "ml"),
            requestedQuantity = 1
        )
        val req = BasketRequest(items = listOf(reqItem), selectedEmirate = "Dubai", nowMs = now)
        val offerA = ProductEntity(id = "a", retailerId = "r1", masterProductId = "m-milk-2l", name = "Almarai Milk 2L", currentPrice = 11.00, validTo = now + 10_000, status = "published")
        val offerB = ProductEntity(id = "b", retailerId = "r1", masterProductId = "m-milk-2l", name = "Almarai Milk 2L", currentPrice = 9.75, validTo = now + 10_000, status = "published")

        val picked = OfferEligibility.pickBestOfferForStore(reqItem, listOf(offerA, offerB), req)
        assertEquals("b", picked?.product?.id)
        assertEquals(975L, picked?.lineSubtotalFils)
    }
}
