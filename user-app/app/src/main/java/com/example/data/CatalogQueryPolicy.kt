package com.example.data

/**
 * Pure query, eligibility, pagination, and cache-freshness rules shared across
 * all consumer entry points (Home, Search, Compare, Product Detail, Favorites, Smart Basket).
 */
data class CatalogSyncState(
    val isLoading: Boolean = true,
    val isFromCache: Boolean = false,
    val lastServerSyncAtMs: Long? = null,
    val errorMessage: String? = null,
    val productsPageLimit: Int = FirestoreDealFinderDao.DEFAULT_PRODUCTS_PAGE_SIZE,
    val hasMoreProducts: Boolean = false,
    val isRefreshing: Boolean = false
) {
    /**
     * S5.3 offline/cache policy: show "Data may be outdated" when served from cache
     * without a recent authoritative server timestamp.
     */
    fun isCachePotentiallyStale(nowMs: Long = System.currentTimeMillis(), maxFreshAgeMs: Long = 30 * 60 * 1000L): Boolean {
        if (isFromCache) return true
        val synced = lastServerSyncAtMs ?: return false
        return (nowMs - synced) > maxFreshAgeMs
    }
}

object CatalogQueryPolicy {
    /** Generic search suggestions — never labeled as measured "Trending" or "Popular". */
    val GENERIC_SUGGESTED_SEARCHES: List<String> = listOf(
        "Milk 1L",
        "Eggs 30",
        "Chicken Breast",
        "Basmati Rice 5kg",
        "Detergent",
        "Cooking Oil"
    )

    fun filterStoresForEmirate(stores: List<RetailerEntity>, emirate: String): List<RetailerEntity> {
        return stores.filter { store ->
            store.status.equals("published", ignoreCase = true) &&
                store.emirates.contains(emirate, ignoreCase = true)
        }
    }

    fun filterEligibleFlyers(
        flyers: List<FlyerEntity>,
        allowedStoreIds: Set<String>,
        nowMs: Long = System.currentTimeMillis()
    ): List<FlyerEntity> {
        return flyers.filter { flyer ->
            flyer.retailerId in allowedStoreIds &&
                flyer.status.equals("published", ignoreCase = true) &&
                (flyer.validFrom == 0L || flyer.validFrom <= nowMs) &&
                (flyer.validTo == 0L || flyer.validTo > nowMs)
        }
    }

    fun filterEligibleProducts(
        products: List<ProductEntity>,
        allowedStoreIds: Set<String>,
        nowMs: Long = System.currentTimeMillis(),
        includeDemo: Boolean = false
    ): List<ProductEntity> {
        return products.filter { product ->
            product.retailerId in allowedStoreIds &&
                product.isCurrentlyEligible(nowMs = nowMs, includeDemo = includeDemo)
        }
    }

    fun filterEligibleCoupons(
        coupons: List<CouponEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): List<CouponEntity> {
        return coupons.filter { coupon ->
            coupon.status.equals("published", ignoreCase = true) &&
                (coupon.expiry == 0L || coupon.expiry > nowMs)
        }
    }

    fun filterEligiblePromotions(
        promotions: List<PromotionEntity>,
        allowedStoreIds: Set<String>,
        nowMs: Long = System.currentTimeMillis()
    ): List<PromotionEntity> {
        return promotions.filter { promo ->
            promo.retailerId in allowedStoreIds &&
                promo.status.equals("published", ignoreCase = true) &&
                (promo.validFrom == 0L || promo.validFrom <= nowMs) &&
                (promo.validTo == 0L || promo.validTo > nowMs)
        }
    }

    fun nextProductsPageLimit(currentLimit: Int): Int {
        return (currentLimit + FirestoreDealFinderDao.DEFAULT_PRODUCTS_PAGE_SIZE)
            .coerceAtMost(FirestoreDealFinderDao.MAX_PRODUCTS_PAGE_SIZE)
    }

    private val NESTLE_SHOWCASE_KEYWORDS = listOf(
        "nestle", "nestlé", "nescafe", "nescafé", "kitkat", "maggi",
        "purina", "friskies", "pellegrino", "smarties", "pure life"
    )

    /** Identifies featured Nestlé demo showcase products so they surface first across product feeds. */
    fun isNestleShowcaseProduct(product: ProductEntity): Boolean {
        if (!product.isDemo) return false
        val brandNorm = product.brand.lowercase()
        val nameNorm = product.name.lowercase()
        return NESTLE_SHOWCASE_KEYWORDS.any { kw ->
            brandNorm.contains(kw) ||
                nameNorm.contains(kw) ||
                product.searchKeywords.any { it.equals(kw, ignoreCase = true) }
        }
    }

    /** Orders featured Nestlé demo products ahead of other items while preserving relative order inside each group. */
    fun prioritizeNestleShowcase(products: List<ProductEntity>): List<ProductEntity> {
        if (products.none { isNestleShowcaseProduct(it) }) return products
        val (featured, rest) = products.partition { isNestleShowcaseProduct(it) }
        return featured + rest
    }
}
