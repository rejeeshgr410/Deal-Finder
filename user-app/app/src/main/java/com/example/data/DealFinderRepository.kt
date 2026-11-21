package com.example.data

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DealFinderRepository(private val dao: DealFinderDao, private val cachedFlyerDao: CachedFlyerDao? = null) {
    val retailers = dao.getStores()

    fun getStoresEnvelope(limit: Int = FirestoreDealFinderDao.DEFAULT_STORES_LIMIT): Flow<SnapshotEnvelope<RetailerEntity>> =
        dao.getStoresEnvelope(limit)

    fun getFlyersEnvelope(limit: Int = FirestoreDealFinderDao.DEFAULT_FLYERS_LIMIT): Flow<SnapshotEnvelope<FlyerEntity>> =
        dao.getFlyersEnvelope(limit)

    fun getProductsEnvelope(limit: Int = FirestoreDealFinderDao.DEFAULT_PRODUCTS_PAGE_SIZE): Flow<SnapshotEnvelope<ProductEntity>> =
        dao.getProductsEnvelope(limit)

    /**
     * Lifecycle-safe flyer stream: uses `channelFlow` so the Firestore listener cancels
     * automatically when downstream collection stops (no leaked `CoroutineScope(Dispatchers.IO)`).
     * Also enforces S5.3 offline/cache policy: cached flyers are filtered against current time
     * so an expired promotion never remains valid while offline.
     */
    val flyers: Flow<List<FlyerEntity>> = if (cachedFlyerDao != null) {
        channelFlow {
            val syncJob = launch(Dispatchers.IO) {
                dao.getFlyers().collect { networkFlyers ->
                    val cachedEntities = networkFlyers.map {
                        CachedFlyerEntity(
                            id = it.id,
                            retailerId = it.retailerId,
                            title = it.title,
                            imageUrl = it.imageUrl,
                            pdfUrl = it.pdfUrl,
                            pages = it.pages,
                            status = it.status,
                            validFrom = it.validFrom,
                            validTo = it.validTo,
                            views = it.views,
                            clicks = it.clicks,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    }
                    cachedFlyerDao.insertFlyers(cachedEntities)
                }
            }
            try {
                cachedFlyerDao.getAllFlyers().collect { cached ->
                    val now = System.currentTimeMillis()
                    send(
                        cached
                            .filter {
                                it.status.equals("published", ignoreCase = true) &&
                                    (it.validFrom == 0L || it.validFrom <= now) &&
                                    (it.validTo == 0L || it.validTo > now)
                            }
                            .map {
                                FlyerEntity(
                                    id = it.id,
                                    retailerId = it.retailerId,
                                    title = it.title,
                                    imageUrl = it.imageUrl,
                                    pdfUrl = it.pdfUrl,
                                    pages = it.pages,
                                    status = it.status,
                                    searchKeywords = emptyList(),
                                    validFrom = it.validFrom,
                                    validTo = it.validTo,
                                    views = it.views,
                                    clicks = it.clicks,
                                    createdAt = it.createdAt,
                                    updatedAt = it.updatedAt
                                )
                            }
                    )
                }
            } finally {
                syncJob.cancel()
            }
        }
    } else {
        dao.getFlyers()
    }

    val products = dao.getProducts()
    val recentSearches = dao.getRecentSearches()

    fun searchProducts(query: String, limit: Int = FirestoreDealFinderDao.DEFAULT_PRODUCTS_PAGE_SIZE) =
        dao.searchProducts(query, limit)
    fun searchRetailers(query: String) = dao.searchStores(query)
    fun searchFlyers(query: String) = dao.searchFlyers(query)

    suspend fun insertSearchHistory(query: String) {
        dao.insertSearchHistory(SearchHistoryEntity(query, System.currentTimeMillis()))
    }

    suspend fun deleteSearchHistory(query: String) {
        dao.deleteSearchHistory(query)
    }

    val allBranches: Flow<List<BranchEntity>> = dao.getAllBranches()
    fun getBranchesForStore(retailerId: String) = dao.getBranchesForStore(retailerId)

    suspend fun getRetailerById(id: String): RetailerEntity? {
        return dao.getStoreById(id)
    }

    // Data access operations
    val allCoupons: Flow<List<CouponEntity>> = dao.getCoupons()
    val promotions: Flow<List<PromotionEntity>> = dao.getPromotions()
    val notifications: Flow<List<NotificationEntity>> = dao.getNotifications()

    suspend fun markNotificationRead(id: String) = dao.markNotificationRead(id)
    suspend fun clearCachedFlyers() { cachedFlyerDao?.clearFlyers() }

    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)

    val savedDeals: Flow<List<ProductEntity>> = dao.getSavedDeals()
    suspend fun toggleSavedDeal(product: ProductEntity) = dao.toggleSavedDeal(product)

    val savedFlyers: Flow<List<FlyerEntity>> = dao.getSavedFlyers()
    suspend fun toggleSavedFlyer(flyer: FlyerEntity) = dao.toggleSavedFlyer(flyer)

    val savedCoupons: Flow<List<CouponEntity>> = dao.getSavedCoupons()
    suspend fun toggleSavedCoupon(coupon: CouponEntity) = dao.toggleSavedCoupon(coupon)

    val followedRetailers: Flow<List<RetailerEntity>> = dao.getFollowedRetailers()
    suspend fun toggleFollowedRetailer(retailer: RetailerEntity) = dao.toggleFollowedRetailer(retailer)
}
