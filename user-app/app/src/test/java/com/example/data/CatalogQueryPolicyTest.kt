package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogQueryPolicyTest {

    private val now = 1_750_000_000_000L

    @Test
    fun `filterStoresForEmirate only returns published stores in selected emirate`() {
        val stores = listOf(
            RetailerEntity(id = "r1", name = "Lulu Dubai", emirates = "Dubai, Sharjah", status = "published"),
            RetailerEntity(id = "r2", name = "Draft Store", emirates = "Dubai", status = "draft"),
            RetailerEntity(id = "r3", name = "Abu Dhabi Store", emirates = "Abu Dhabi", status = "published")
        )

        val result = CatalogQueryPolicy.filterStoresForEmirate(stores, "Dubai")
        assertEquals(listOf("r1"), result.map { it.id })
    }

    @Test
    fun `filterEligibleProducts excludes expired, future, draft, out-of-stock, and out-of-emirate offers`() {
        val allowed = setOf("r1")
        val products = listOf(
            ProductEntity(id = "ok", retailerId = "r1", currentPrice = 12.50, status = "published", validFrom = now - 1000, validTo = now + 60_000),
            ProductEntity(id = "expired", retailerId = "r1", currentPrice = 12.50, status = "published", validFrom = now - 10_000, validTo = now - 1),
            ProductEntity(id = "future", retailerId = "r1", currentPrice = 12.50, status = "published", validFrom = now + 60_000, validTo = now + 120_000),
            ProductEntity(id = "withdrawn", retailerId = "r1", currentPrice = 12.50, status = "withdrawn", validFrom = 0, validTo = now + 60_000),
            ProductEntity(id = "oos", retailerId = "r1", currentPrice = 12.50, status = "published", availability = "out_of_stock", validTo = now + 60_000),
            ProductEntity(id = "other_emirate", retailerId = "r2", currentPrice = 12.50, status = "published", validTo = now + 60_000)
        )

        val eligible = CatalogQueryPolicy.filterEligibleProducts(products, allowed, nowMs = now)
        assertEquals(listOf("ok"), eligible.map { it.id })
    }

    @Test
    fun `filterEligibleFlyers excludes expired and future flyers even when loaded from cache`() {
        val allowed = setOf("r1")
        val flyers = listOf(
            FlyerEntity(id = "f_ok", retailerId = "r1", status = "published", validFrom = now - 5000, validTo = now + 5000),
            FlyerEntity(id = "f_expired", retailerId = "r1", status = "published", validFrom = now - 5000, validTo = now - 1),
            FlyerEntity(id = "f_future", retailerId = "r1", status = "published", validFrom = now + 5000, validTo = now + 10_000),
            FlyerEntity(id = "f_draft", retailerId = "r1", status = "draft", validFrom = 0, validTo = now + 5000)
        )

        val eligible = CatalogQueryPolicy.filterEligibleFlyers(flyers, allowed, nowMs = now)
        assertEquals(listOf("f_ok"), eligible.map { it.id })
    }

    @Test
    fun `cache staleness policy flags cached snapshots and old server sync timestamps`() {
        val fromCache = CatalogSyncState(isLoading = false, isFromCache = true, lastServerSyncAtMs = now)
        assertTrue(fromCache.isCachePotentiallyStale(nowMs = now))

        val freshServer = CatalogSyncState(isLoading = false, isFromCache = false, lastServerSyncAtMs = now - 60_000)
        assertFalse(freshServer.isCachePotentiallyStale(nowMs = now))

        val staleServer = CatalogSyncState(isLoading = false, isFromCache = false, lastServerSyncAtMs = now - 45 * 60 * 1000L)
        assertTrue(staleServer.isCachePotentiallyStale(nowMs = now))
    }

    @Test
    fun `pagination expands page limit by 100 up to max cap`() {
        assertEquals(200, CatalogQueryPolicy.nextProductsPageLimit(100))
        assertEquals(300, CatalogQueryPolicy.nextProductsPageLimit(200))
        assertEquals(
            FirestoreDealFinderDao.MAX_PRODUCTS_PAGE_SIZE,
            CatalogQueryPolicy.nextProductsPageLimit(FirestoreDealFinderDao.MAX_PRODUCTS_PAGE_SIZE)
        )
    }
}
