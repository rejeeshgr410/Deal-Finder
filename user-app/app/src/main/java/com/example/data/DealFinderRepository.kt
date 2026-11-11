package com.example.data

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DealFinderRepository(private val dao: DealFinderDao, private val cachedFlyerDao: CachedFlyerDao? = null) {
    val retailers = dao.getStores()
    
    val flyers: Flow<List<FlyerEntity>> = if (cachedFlyerDao != null) {
        flow {
            CoroutineScope(Dispatchers.IO).launch {
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
            cachedFlyerDao.getAllFlyers().collect { cached ->
                emit(cached.map { 
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
                })
            }
        }
    } else {
        dao.getFlyers()
    }

    val products = dao.getProducts()
    val recentSearches = dao.getRecentSearches()

    fun searchProducts(query: String) = dao.searchProducts(query)
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
