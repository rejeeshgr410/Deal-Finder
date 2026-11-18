package com.example.data

import org.junit.Assert.*
import org.junit.Test

class OfferContractV2Test {

    @Test
    fun branchCoordinateEligibility_rejectsSyntheticOrInvalidCoords() {
        val zeroBranch = BranchEntity(id = "b0", latitude = 0.0, longitude = 0.0, coordsVerified = true)
        assertFalse(zeroBranch.isEligibleForDistance())

        val outsideUae = BranchEntity(id = "b1", latitude = 40.71, longitude = -74.00, coordsVerified = true)
        assertFalse(outsideUae.isEligibleForDistance())

        val unverifiedSeed = BranchEntity(
            id = "b2",
            latitude = 25.15,
            longitude = 55.22,
            coordsVerified = false,
            coordSource = "unverified_seed"
        )
        assertFalse(unverifiedSeed.isEligibleForDistance())

        val verifiedDubai = BranchEntity(
            id = "b3",
            latitude = 25.1181,
            longitude = 55.2007,
            coordsVerified = true,
            coordSource = "merchant_entered"
        )
        assertTrue(verifiedDubai.isEligibleForDistance())
    }

    @Test
    fun productEligibility_enforcesExpiryDemoOutOfStockAndPrice() {
        val now = 1_700_000_000_000L

        val validV1 = ProductEntity(
            id = "p1",
            name = "Fresh Milk 2L",
            currentPrice = 10.50,
            validFrom = now - 10_000L,
            validTo = now + 86_400_000L,
            status = "published"
        )
        assertTrue(validV1.isCurrentlyEligible(now))
        assertEquals(1050L, validV1.effectiveAmountMinor())

        val expired = validV1.copy(id = "p2", validTo = now - 1L)
        assertFalse(expired.isCurrentlyEligible(now))

        val demoItem = validV1.copy(id = "p3", isDemo = true, sourceType = "demo")
        assertFalse(demoItem.isCurrentlyEligible(now))
        assertTrue(demoItem.isCurrentlyEligible(now, includeDemo = true))

        val outOfStock = validV1.copy(id = "p4", availability = "out_of_stock")
        assertFalse(outOfStock.isCurrentlyEligible(now))

        val zeroPrice = validV1.copy(id = "p5", currentPrice = 0.0, amountMinor = 0L)
        assertFalse(zeroPrice.isCurrentlyEligible(now))

        val v2MissingExpiry = validV1.copy(id = "p6", schemaVersion = 2, validTo = 0L)
        assertFalse(v2MissingExpiry.isCurrentlyEligible(now))
    }

    @Test
    fun gtinCheckDigit_validatesCorrectly() {
        assertTrue(OfferContractV2.isValidGtin("5449000000996"))
        assertFalse(OfferContractV2.isValidGtin("5449000000997"))
        assertFalse(OfferContractV2.isValidGtin("0000000000000"))
    }
}
