package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CatalogQueryPolicy
import com.example.data.CatalogSyncState
import com.example.data.DealFinderRepository
import com.example.data.FirestoreDealFinderDao
import com.example.data.FlyerEntity
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.data.SnapshotEnvelope
import com.example.data.isEligibleForDistance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DealFinderViewModel(
    private val repository: DealFinderRepository,
    private val locationManager: com.example.data.LocationManager
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0L)
    private val productsPageLimit = MutableStateFlow(FirestoreDealFinderDao.DEFAULT_PRODUCTS_PAGE_SIZE)

    private val storesEnvelope: StateFlow<SnapshotEnvelope<RetailerEntity>> = refreshTick
        .flatMapLatest { repository.getStoresEnvelope() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SnapshotEnvelope())

    private val flyersEnvelope: StateFlow<SnapshotEnvelope<FlyerEntity>> = refreshTick
        .flatMapLatest { repository.getFlyersEnvelope() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SnapshotEnvelope())

    private val productsEnvelope: StateFlow<SnapshotEnvelope<ProductEntity>> =
        combine(refreshTick, productsPageLimit) { _, limit -> limit }
            .flatMapLatest { limit -> repository.getProductsEnvelope(limit) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SnapshotEnvelope())

    val isLoaded: StateFlow<Boolean> = combine(storesEnvelope, flyersEnvelope, productsEnvelope) { s, f, p ->
        s.pageLimit > 0 || f.pageLimit > 0 || p.pageLimit > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncState: StateFlow<CatalogSyncState> = combine(
        storesEnvelope,
        flyersEnvelope,
        productsEnvelope,
        productsPageLimit
    ) { s, f, p, limit ->
        val loaded = s.pageLimit > 0 || f.pageLimit > 0 || p.pageLimit > 0
        val anyFromCache = s.isFromCache || f.isFromCache || p.isFromCache
        val latestSync = listOfNotNull(s.lastServerSyncAtMs, f.lastServerSyncAtMs, p.lastServerSyncAtMs).maxOrNull()
        val firstError = p.errorMessage ?: s.errorMessage ?: f.errorMessage
        CatalogSyncState(
            isLoading = !loaded,
            isFromCache = anyFromCache,
            lastServerSyncAtMs = latestSync,
            errorMessage = firstError,
            productsPageLimit = limit,
            hasMoreProducts = p.isTruncated && limit < FirestoreDealFinderDao.MAX_PRODUCTS_PAGE_SIZE,
            isRefreshing = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CatalogSyncState())

    fun refreshCatalog() {
        refreshTick.value = System.currentTimeMillis()
    }

    fun loadMoreProducts() {
        val next = CatalogQueryPolicy.nextProductsPageLimit(productsPageLimit.value)
        if (next != productsPageLimit.value) {
            productsPageLimit.value = next
        }
    }

    val selectedEmirate = locationManager.selectedEmirate
    val hasActiveGpsLocation: StateFlow<Boolean> = locationManager.userLocation
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val stores: StateFlow<List<RetailerEntity>> = combine(storesEnvelope, selectedEmirate) { envelope, emirate ->
        CatalogQueryPolicy.filterStoresForEmirate(envelope.items, emirate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flyers: StateFlow<List<FlyerEntity>> = combine(repository.flyers, stores) { allFlyers, filteredStores ->
        val storeIds = filteredStores.map { it.id }.toSet()
        CatalogQueryPolicy.filterEligibleFlyers(allFlyers, storeIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = combine(productsEnvelope, stores) { envelope, filteredStores ->
        val storeIds = filteredStores.map { it.id }.toSet()
        CatalogQueryPolicy.filterEligibleProducts(envelope.items, storeIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<com.example.data.CouponEntity>> = repository.allCoupons
        .map { allCoupons -> CatalogQueryPolicy.filterEligibleCoupons(allCoupons) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val promotions: StateFlow<List<com.example.data.PromotionEntity>> = combine(repository.promotions, stores) { allPromos, filteredStores ->
        val storeIds = filteredStores.map { it.id }.toSet()
        CatalogQueryPolicy.filterEligiblePromotions(allPromos, storeIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedDeals: StateFlow<List<ProductEntity>> = repository.savedDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedFlyers: StateFlow<List<FlyerEntity>> = repository.savedFlyers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCoupons: StateFlow<List<com.example.data.CouponEntity>> = repository.savedCoupons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followedRetailers: StateFlow<List<RetailerEntity>> = repository.followedRetailers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSavedDeal(product: ProductEntity) {
        viewModelScope.launch { repository.toggleSavedDeal(product) }
    }

    fun toggleSavedFlyer(flyer: FlyerEntity) {
        viewModelScope.launch { repository.toggleSavedFlyer(flyer) }
    }

    fun toggleSavedCoupon(coupon: com.example.data.CouponEntity) {
        viewModelScope.launch { repository.toggleSavedCoupon(coupon) }
    }

    fun toggleFollowedRetailer(retailer: RetailerEntity) {
        viewModelScope.launch { repository.toggleFollowedRetailer(retailer) }
    }

    val recentSearches: StateFlow<List<com.example.data.SearchHistoryEntity>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Generic curated search suggestions (never labeled as measured trends). */
    val suggestedSearches: StateFlow<List<String>> = flowOf(CatalogQueryPolicy.GENERIC_SUGGESTED_SEARCHES)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CatalogQueryPolicy.GENERIC_SUGGESTED_SEARCHES)

    @Deprecated("Use suggestedSearches — not measured trend data", ReplaceWith("suggestedSearches"))
    val trendingSearches: StateFlow<List<String>> = suggestedSearches

    private val searchQueryFlow = MutableStateFlow("")

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    private fun String.normalize() =
        this.replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim().lowercase()

    val searchResultsProducts = searchQueryFlow
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                products
            } else {
                combine(repository.searchProducts(query, productsPageLimit.value), products, stores) { searched, activeProducts, activeStores ->
                    val words = query.normalize().split(" ").filter { it.isNotBlank() }
                    val storeIds = activeStores.map { it.id }.toSet()
                    val eligibleRemote = CatalogQueryPolicy.filterEligibleProducts(searched, storeIds)
                    val inMemory = activeProducts.filter { p ->
                        val searchStr = "${p.name.normalize()} ${p.brand.normalize()} ${p.category.normalize()}"
                        words.all { w -> searchStr.contains(w) }
                    }
                    val remote = eligibleRemote.filter { p ->
                        val searchStr = "${p.name.normalize()} ${p.brand.normalize()} ${p.category.normalize()}"
                        words.all { w -> searchStr.contains(w) }
                    }
                    (inMemory + remote).distinctBy { it.id }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsStores = searchQueryFlow
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList<RetailerEntity>()) else {
                combine(repository.searchRetailers(query), stores, selectedEmirate) { searched, activeStores, emirate ->
                    val words = query.normalize().split(" ").filter { it.isNotBlank() }
                    val eligibleRemote = CatalogQueryPolicy.filterStoresForEmirate(searched, emirate)
                    val inMemory = activeStores.filter { s ->
                        val searchStr = "${s.name.normalize()} ${s.emirates.normalize()}"
                        words.all { w -> searchStr.contains(w) }
                    }
                    (inMemory + eligibleRemote).distinctBy { it.id }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsFlyers = searchQueryFlow
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList<FlyerEntity>()) else {
                combine(repository.searchFlyers(query), flyers, stores) { searched, activeFlyers, activeStores ->
                    val storeIds = activeStores.map { it.id }.toSet()
                    val eligibleRemote = CatalogQueryPolicy.filterEligibleFlyers(searched, storeIds)
                    val inMemory = activeFlyers.filter { f ->
                        f.title.normalize().contains(query.normalize())
                    }
                    (inMemory + eligibleRemote).distinctBy { it.id }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertSearchHistory(query: String) {
        viewModelScope.launch {
            repository.insertSearchHistory(query)
        }
    }

    fun deleteSearchHistory(query: String) {
        viewModelScope.launch {
            repository.deleteSearchHistory(query)
        }
    }

    val branches: StateFlow<List<com.example.data.BranchEntity>> =
        repository.allBranches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getStore(id: String): RetailerEntity? {
        return stores.value.find { it.id == id }
    }

    fun getBranchesForStoreInEmirate(retailerId: String): List<com.example.data.BranchEntity> {
        val emirate = selectedEmirate.value
        val storeBranches = branches.value.filter { it.retailerId == retailerId }
        val inEmirate = storeBranches.filter { it.emirate.equals(emirate, ignoreCase = true) }
        return if (inEmirate.isNotEmpty()) inEmirate else storeBranches
    }

    fun getDistanceToStore(retailerId: String): Double? {
        val userLoc = locationManager.userLocation.value
        val fallbackEmirate = com.example.data.uaeEmirates.find { it.name == selectedEmirate.value }

        val lat = userLoc?.first ?: fallbackEmirate?.lat ?: return null
        val lon = userLoc?.second ?: fallbackEmirate?.lon ?: return null

        val storeBranches = branches.value.filter { it.retailerId == retailerId && it.isEligibleForDistance() }
        if (storeBranches.isNotEmpty()) {
            return storeBranches.map { branch ->
                locationManager.distance(lat, lon, branch.latitude, branch.longitude)
            }.minOrNull()
        }
        return null
    }

    fun getNearestBranchAndDistance(retailerId: String): Pair<com.example.data.BranchEntity?, Double?> {
        val userLoc = locationManager.userLocation.value
        val fallbackEmirate = com.example.data.uaeEmirates.find { it.name == selectedEmirate.value }

        val lat = userLoc?.first ?: fallbackEmirate?.lat ?: return Pair(null, null)
        val lon = userLoc?.second ?: fallbackEmirate?.lon ?: return Pair(null, null)

        val storeBranches = branches.value.filter { it.retailerId == retailerId && it.isEligibleForDistance() }
        if (storeBranches.isNotEmpty()) {
            val nearest = storeBranches.minByOrNull { branch ->
                locationManager.distance(lat, lon, branch.latitude, branch.longitude)
            }
            val dist = nearest?.let { locationManager.distance(lat, lon, it.latitude, it.longitude) }
            return Pair(nearest, dist)
        }
        return Pair(null, null)
    }

    class Factory(
        private val repo: DealFinderRepository,
        private val locationManager: com.example.data.LocationManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DealFinderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DealFinderViewModel(repo, locationManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
