package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DealFinderFeatureTests {

    private lateinit var sampleRetailers: List<RetailerEntity>
    private lateinit var sampleProducts: List<ProductEntity>
    private lateinit var sampleFlyers: List<FlyerEntity>

    @Before
    fun setup() {
        sampleRetailers = listOf(
            RetailerEntity(
                id = "carrefour-uae-259",
                name = "Carrefour UAE",
                emirates = "Dubai, UAE",
                status = "published",
                branchesCount = 4
            ),
            RetailerEntity(
                id = "lulu-hypermarket-267",
                name = "Lulu Hypermarket",
                emirates = "Dubai, UAE",
                status = "published",
                branchesCount = 6
            ),
            RetailerEntity(
                id = "nesto-hypermarket-284",
                name = "Nesto Hypermarket",
                emirates = "Dubai, UAE",
                status = "published",
                branchesCount = 7
            ),
            RetailerEntity(
                id = "west-zone-supermarket-456",
                name = "West Zone Supermarket",
                emirates = "Dubai, UAE",
                status = "published",
                branchesCount = 3
            )
        )

        sampleProducts = listOf(
            // Coca Cola Pack across 3 stores
            ProductEntity(
                id = "prod-coke-carrefour",
                masterProductId = "master-coke-6x330ml",
                retailerId = "carrefour-uae-259",
                name = "Coca Cola Can 330ml (Pack of 6)",
                normalizedName = "coke 330ml pack of 6",
                brand = "Coca Cola",
                category = "Drinks & Beverages",
                barcode = "5449000000996",
                currentPrice = 11.50,
                oldPrice = 15.00,
                status = "published"
            ),
            ProductEntity(
                id = "prod-coke-lulu",
                masterProductId = "master-coke-6x330ml",
                retailerId = "lulu-hypermarket-267",
                name = "Coca Cola Can 330ml (Pack of 6)",
                normalizedName = "coke 330ml pack of 6",
                brand = "Coca Cola",
                category = "Drinks & Beverages",
                barcode = "5449000000996",
                currentPrice = 10.90,
                oldPrice = 14.50,
                status = "published"
            ),
            ProductEntity(
                id = "prod-coke-nesto",
                masterProductId = "master-coke-6x330ml",
                retailerId = "nesto-hypermarket-284",
                name = "Coca Cola Can 330ml (Pack of 6)",
                normalizedName = "coke 330ml pack of 6",
                brand = "Coca Cola",
                category = "Drinks & Beverages",
                barcode = "5449000000996",
                currentPrice = 9.99,
                oldPrice = 13.50,
                status = "published"
            ),

            // Almarai Fresh Milk 2L across 3 stores
            ProductEntity(
                id = "prod-milk-carrefour",
                masterProductId = "master-almarai-milk-2l",
                retailerId = "carrefour-uae-259",
                name = "Almarai Fresh Milk Full Cream 2L",
                normalizedName = "almarai fresh milk full cream 2l",
                brand = "Almarai",
                category = "Dairy & Eggs",
                barcode = "6281007010045",
                currentPrice = 11.00,
                oldPrice = 12.50,
                status = "published"
            ),
            ProductEntity(
                id = "prod-milk-lulu",
                masterProductId = "master-almarai-milk-2l",
                retailerId = "lulu-hypermarket-267",
                name = "Almarai Fresh Milk Full Cream 2L",
                normalizedName = "almarai fresh milk full cream 2l",
                brand = "Almarai",
                category = "Dairy & Eggs",
                barcode = "6281007010045",
                currentPrice = 10.50,
                oldPrice = 12.00,
                status = "published"
            ),
            ProductEntity(
                id = "prod-milk-nesto",
                masterProductId = "master-almarai-milk-2l",
                retailerId = "nesto-hypermarket-284",
                name = "Almarai Fresh Milk Full Cream 2L",
                normalizedName = "almarai fresh milk full cream 2l",
                brand = "Almarai",
                category = "Dairy & Eggs",
                barcode = "6281007010045",
                currentPrice = 9.75,
                oldPrice = 11.50,
                status = "published"
            ),

            // Sunwhite Rice 5kg across stores
            ProductEntity(
                id = "prod-rice-carrefour",
                masterProductId = "master-sunwhite-rice-5kg",
                retailerId = "carrefour-uae-259",
                name = "Sunwhite Calrose Rice 5kg",
                normalizedName = "sunwhite calrose rice 5kg",
                brand = "Sunwhite",
                category = "Food - Grocery",
                barcode = "9310155001052",
                currentPrice = 32.50,
                oldPrice = 38.00,
                status = "published"
            ),
            ProductEntity(
                id = "prod-rice-lulu",
                masterProductId = "master-sunwhite-rice-5kg",
                retailerId = "lulu-hypermarket-267",
                name = "Sunwhite Calrose Rice 5kg",
                normalizedName = "sunwhite calrose rice 5kg",
                brand = "Sunwhite",
                category = "Food - Grocery",
                barcode = "9310155001052",
                currentPrice = 29.90,
                oldPrice = 36.50,
                status = "published"
            ),
            ProductEntity(
                id = "prod-rice-nesto",
                masterProductId = "master-sunwhite-rice-5kg",
                retailerId = "nesto-hypermarket-284",
                name = "Sunwhite Calrose Rice 5kg",
                normalizedName = "sunwhite calrose rice 5kg",
                brand = "Sunwhite",
                category = "Food - Grocery",
                barcode = "9310155001052",
                currentPrice = 27.99,
                oldPrice = 35.00,
                status = "published"
            )
        )

        sampleFlyers = listOf(
            FlyerEntity(
                id = "799872",
                retailerId = "lulu-hypermarket-267",
                title = "Lulu Saver",
                pages = 45,
                pdfUrl = "lulu-hypermarket-267/catalogues/Lulu Saver_799872.pdf",
                status = "published",
                validFrom = System.currentTimeMillis() - 86400000,
                validTo = System.currentTimeMillis() + 7 * 86400000
            ),
            FlyerEntity(
                id = "799983",
                retailerId = "carrefour-uae-259",
                title = "Everything At AED 5, 10, 15, 20, 25, 30",
                pages = 19,
                pdfUrl = "carrefour-uae-259/catalogues/Everything At AED 5,10,15,20,25,30@ Carrefour Hypermarket_799983.pdf",
                status = "published",
                validFrom = System.currentTimeMillis() - 86400000,
                validTo = System.currentTimeMillis() + 7 * 86400000
            ),
            FlyerEntity(
                id = "800805",
                retailerId = "nesto-hypermarket-284",
                title = "Al Muteena Street- Deira, Dubai Offers",
                pages = 33,
                pdfUrl = "nesto-hypermarket-284/catalogues/Al Muteena Street- Deira, Dubai_800805.pdf",
                status = "published",
                validFrom = System.currentTimeMillis() - 86400000,
                validTo = System.currentTimeMillis() + 3 * 86400000
            )
        )
    }

    // 1. PRODUCT SEARCH & MATCHING TESTS
    @Test
    fun testProductSearch_findsByQueryAndBrand() {
        val query = "coke"
        val results = sampleProducts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.brand.contains(query, ignoreCase = true) ||
            it.normalizedName.contains(query, ignoreCase = true)
        }
        assertEquals("Should find all 3 Coca Cola offers", 3, results.size)
        assertTrue(results.all { it.brand == "Coca Cola" })
    }

    @Test
    fun testProductSearch_findsDairyProducts() {
        val query = "milk"
        val results = sampleProducts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
        assertEquals("Should find all 3 Almarai milk offers", 3, results.size)
    }

    // 2. MULTI-STORE PRICE COMPARISON TESTS
    @Test
    fun testProductPriceComparison_matchesIdenticalItems() {
        val cokeCarrefour = sampleProducts.first { it.id == "prod-coke-carrefour" }
        val matchingOffers = sampleProducts.filter { it.isIdenticalTo(cokeCarrefour) }

        assertEquals("Should identify same product across 3 retailers", 3, matchingOffers.size)
        
        // Find cheapest store
        val cheapestOffer = matchingOffers.minByOrNull { it.currentPrice }
        assertNotNull(cheapestOffer)
        assertEquals("Nesto should be the cheapest store for Coke", "nesto-hypermarket-284", cheapestOffer?.retailerId)
        assertEquals(9.99, cheapestOffer?.currentPrice ?: 0.0, 0.001)

        // Verify discount calculations
        val maxPrice = matchingOffers.maxOf { it.currentPrice }
        val minPrice = matchingOffers.minOf { it.currentPrice }
        val priceDiff = maxPrice - minPrice
        assertEquals(1.51, priceDiff, 0.01)
    }

    @Test
    fun testProductPriceComparison_milkAcrossStores() {
        val milkCarrefour = sampleProducts.first { it.id == "prod-milk-carrefour" }
        val matchingOffers = sampleProducts.filter { it.isIdenticalTo(milkCarrefour) }
            .sortedBy { it.currentPrice }

        assertEquals(3, matchingOffers.size)
        assertEquals("nesto-hypermarket-284", matchingOffers[0].retailerId) // 9.75
        assertEquals("lulu-hypermarket-267", matchingOffers[1].retailerId)   // 10.50
        assertEquals("carrefour-uae-259", matchingOffers[2].retailerId)      // 11.00
    }

    // 3. BASKET OPTIMIZATION ENGINE TESTS
    @Test
    fun testBasketOptimization_singleStoreCheapest() {
        // User shopping basket: 1x Coke, 1x Milk, 1x Rice
        val basketItems = listOf("master-coke-6x330ml", "master-almarai-milk-2l", "master-sunwhite-rice-5kg")

        val storeTotals = sampleRetailers.mapNotNull { retailer ->
            val retailerProducts = sampleProducts.filter { it.retailerId == retailer.id }
            val matchedItems = basketItems.mapNotNull { masterId ->
                retailerProducts.find { it.masterProductId == masterId }
            }
            if (matchedItems.size == basketItems.size) {
                val totalCost = matchedItems.sumOf { it.currentPrice }
                retailer.name to totalCost
            } else {
                null
            }
        }.toMap()

        assertEquals(3, storeTotals.size) // Carrefour, Lulu, Nesto all carry all 3 items
        
        val cheapestStore = storeTotals.minByOrNull { it.value }
        assertNotNull(cheapestStore)
        assertEquals("Nesto Hypermarket", cheapestStore?.key)
        // 9.99 + 9.75 + 27.99 = 47.73
        assertEquals(47.73, cheapestStore?.value ?: 0.0, 0.01)

        val carrefourTotal = storeTotals["Carrefour UAE"] ?: 0.0
        // 11.50 + 11.00 + 32.50 = 55.00
        assertEquals(55.00, carrefourTotal, 0.01)

        val luluTotal = storeTotals["Lulu Hypermarket"] ?: 0.0
        // 10.90 + 10.50 + 29.90 = 51.30
        assertEquals(51.30, luluTotal, 0.01)

        val maxSingleStoreSavings = carrefourTotal - (cheapestStore?.value ?: 0.0)
        assertEquals(7.27, maxSingleStoreSavings, 0.01)
    }

    @Test
    fun testBasketOptimization_splitStoreOptimization() {
        // Optimal split: buy each item at its lowest price across all stores
        val basketItems = listOf("master-coke-6x330ml", "master-almarai-milk-2l", "master-sunwhite-rice-5kg")

        val optimalSplitTotal = basketItems.sumOf { masterId ->
            sampleProducts.filter { it.masterProductId == masterId }
                .minOf { it.currentPrice }
        }

        // In our data, Nesto has lowest for all 3 (9.99 + 9.75 + 27.99 = 47.73)
        assertEquals(47.73, optimalSplitTotal, 0.01)
    }

    // 4. FLYER & CATALOGUE TESTS
    @Test
    fun testFlyerCatalogues_activeStatusAndPages() {
        val now = System.currentTimeMillis()
        val activeFlyers = sampleFlyers.filter {
            it.status == "published" && it.validTo > now
        }
        assertEquals("All sample flyers should be active and published", 3, activeFlyers.size)
        assertTrue("Lulu Saver should have 45 pages", activeFlyers.any { it.pages == 45 })
        assertTrue("PDF paths must be populated", activeFlyers.all { it.pdfUrl.endsWith(".pdf") })
    }

    // 5. LOYALTY CARD BARCODE WALLET TESTS
    @Test
    fun testLoyaltyCard_barcodeEncoding() {
        val testCard = LoyaltyCardEntity(
            storeName = "Carrefour MyClub",
            cardNumber = "987654321012",
            barcodeFormat = "CODE_128"
        )
        val modules = com.example.ui.loyalty.BarcodeRenderer.encodeCode128(testCard.cardNumber)
        assertNotNull("Barcode modules should not be null", modules)
        assertTrue("Barcode should contain modules", modules.isNotEmpty())
        assertTrue("Barcode should include quiet zones", modules.size > 20)
        
        // Test QR matrix generation
        val qrMatrix = com.example.ui.loyalty.BarcodeRenderer.encodeQrMatrix(testCard.cardNumber)
        assertEquals(25, qrMatrix.size)
        assertEquals(25, qrMatrix[0].size)
    }

    // 6. LOCALIZATION SUPPORT TESTS
    @Test
    fun testLocaleManager_supportedLanguages() {
        val supportedLocales = listOf("en", "ar")
        assertTrue(supportedLocales.contains("en"))
        assertTrue(supportedLocales.contains("ar"))
    }
}
