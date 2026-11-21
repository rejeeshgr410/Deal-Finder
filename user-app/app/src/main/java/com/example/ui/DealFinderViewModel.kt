package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DealFinderViewModel(
    private val repository: DealFinderRepository,
    private val locationManager: LocationManager
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0L)
    private val isRefreshingState = MutableStateFlow(false)
    private val productsPageLimit = MutableStateFlow(FirestoreDealFinderDao.DEFAULT_PRODUCTS_PAGE_SIZE)
    private val demoEnabled = DemoRegistry.isDemoEnabled

    private val localReadNotificationIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            try {
                locationManager.tryAutoDetectIfPermitted()
            } catch (_: Exception) {}
        }
    }

    private val storesEnvelope: StateFlow<SnapshotEnvelope<RetailerEntity>> = refreshTick
        .flatMapLatest { repository.getStoresEnvelope() }
        .onEach { isRefreshingState.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SnapshotEnvelope())

    private val flyersEnvelope: StateFlow<SnapshotEnvelope<FlyerEntity>> = refreshTick
        .flatMapLatest { repository.getFlyersEnvelope() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SnapshotEnvelope())

    private val productsEnvelope: StateFlow<SnapshotEnvelope<ProductEntity>> =
        combine(refreshTick, productsPageLimit) { _, limit -> limit }
            .flatMapLatest { limit -> repository.getProductsEnvelope(limit) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SnapshotEnvelope())

    private val couponsStream: StateFlow<List<CouponEntity>> = refreshTick
        .flatMapLatest { repository.allCoupons }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val promotionsStream: StateFlow<List<PromotionEntity>> = refreshTick
        .flatMapLatest { repository.promotions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val branchesStream: StateFlow<List<BranchEntity>> = refreshTick
        .flatMapLatest { repository.allBranches }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val syncedFlyersStream: StateFlow<List<FlyerEntity>> = refreshTick
        .flatMapLatest { repository.flyers }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoaded: StateFlow<Boolean> = combine(storesEnvelope, flyersEnvelope, productsEnvelope, demoEnabled) { s, f, p, isDemo ->
        (s.pageLimit > 0 && f.pageLimit > 0 && p.pageLimit > 0) ||
            (isDemo && DemoRegistry.isDemoAvailable.value) ||
            (s.pageLimit > 0 || p.pageLimit > 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncState: StateFlow<CatalogSyncState> = combine(
        storesEnvelope,
        flyersEnvelope,
        productsEnvelope,
        productsPageLimit,
        isRefreshingState
    ) { s, f, p, limit, refreshing ->
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
            isRefreshing = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CatalogSyncState())

    fun refreshCatalog() {
        isRefreshingState.value = true
        refreshTick.value = System.currentTimeMillis()
    }

    fun loadMoreProducts() {
        val next = CatalogQueryPolicy.nextProductsPageLimit(productsPageLimit.value)
        if (next != productsPageLimit.value) {
            productsPageLimit.value = next
        }
    }

    val selectedEmirate: StateFlow<String> = locationManager.selectedEmirate
    val hasActiveGpsLocation: StateFlow<Boolean> = locationManager.userLocation
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val allStoresCombined: StateFlow<List<RetailerEntity>> =
        combine(storesEnvelope, demoEnabled) { envelope, demoOn ->
            val live = envelope.items
            if (!demoOn) return@combine live
            val demoSnap = DemoRegistry.currentSnapshot()
            // Enrich any live store missing a logoUrl with matching demo brand logo,
            // and append demo stores that do not already exist in Firestore by name key.
            val enrichedLive = live.map { liveStore ->
                if (liveStore.logoUrl.isNotBlank()) liveStore
                else {
                    val match = demoSnap.retailers.firstOrNull { d ->
                        val key = d.name.substringBefore(" ").lowercase()
                        liveStore.name.lowercase().contains(key)
                    }
                    if (match != null) liveStore.copy(logoUrl = match.logoUrl) else liveStore
                }
            }
            val liveKeys = enrichedLive.map { it.name.substringBefore(" ").lowercase() }.toSet()
            val extraDemo = demoSnap.retailers.filter { d ->
                val key = d.name.substringBefore(" ").lowercase()
                key !in liveKeys
            }
            enrichedLive + extraDemo
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stores: StateFlow<List<RetailerEntity>> =
        combine(allStoresCombined, selectedEmirate) { allStores, emirate ->
            CatalogQueryPolicy.filterStoresForEmirate(allStores, emirate)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun remapDemoRetailerId(
        retailerId: String,
        demoSnap: DemoCatalogSnapshot,
        activeStores: List<RetailerEntity>
    ): String {
        val demoRet = demoSnap.retailers.firstOrNull { it.id == retailerId } ?: return retailerId
        val key = demoRet.name.substringBefore(" ").lowercase()
        val liveMatch = activeStores.firstOrNull { s ->
            !s.id.startsWith("demo_") && s.name.lowercase().contains(key)
        }
        return liveMatch?.id ?: retailerId
    }

    val flyers: StateFlow<List<FlyerEntity>> =
        combine(syncedFlyersStream, flyersEnvelope, stores, demoEnabled) { cachedFlyers, rawEnvelope, filteredStores, demoOn ->
            val mergedLive = (cachedFlyers + rawEnvelope.items).distinctBy { it.id }
            val storeIds = filteredStores.map { it.id }.toSet()
            val eligibleLive = CatalogQueryPolicy.filterEligibleFlyers(mergedLive, storeIds)
            if (!demoOn) return@combine eligibleLive

            val demoSnap = DemoRegistry.currentSnapshot()
            val remappedDemo = demoSnap.flyers.map { f ->
                f.copy(retailerId = remapDemoRetailerId(f.retailerId, demoSnap, filteredStores))
            }
            val eligibleDemo = CatalogQueryPolicy.filterEligibleFlyers(remappedDemo, storeIds)
            val existingRetailersWithFlyers = eligibleLive.map { it.retailerId }.toSet()
            val filteredDemo = eligibleDemo.filter { it.retailerId !in existingRetailersWithFlyers && it.id !in eligibleLive.map { f -> f.id } }
            eligibleLive + filteredDemo
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> =
        combine(productsEnvelope, stores, demoEnabled) { envelope, filteredStores, demoOn ->
            val storeIds = filteredStores.map { it.id }.toSet()
            val liveProducts = CatalogQueryPolicy.filterEligibleProducts(envelope.items, storeIds)
            if (!demoOn) return@combine liveProducts

            val demoSnap = DemoRegistry.currentSnapshot()
            val remappedDemo = demoSnap.products.map { p ->
                p.copy(retailerId = remapDemoRetailerId(p.retailerId, demoSnap, filteredStores))
            }
            val eligibleDemo = CatalogQueryPolicy.filterEligibleProducts(
                products = remappedDemo,
                allowedStoreIds = storeIds,
                includeDemo = true
            )
            CatalogQueryPolicy.prioritizeNestleShowcase((liveProducts + eligibleDemo).distinctBy { it.id })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<CouponEntity>> =
        combine(couponsStream, stores, demoEnabled) { allCoupons, filteredStores, demoOn ->
            val storeIds = filteredStores.map { it.id }.toSet()
            val eligibleLive = CatalogQueryPolicy.filterEligibleCoupons(allCoupons).filter { coupon ->
                coupon.isGlobal || coupon.retailerId.isBlank() || coupon.retailerId in storeIds
            }
            if (!demoOn) return@combine eligibleLive

            val demoSnap = DemoRegistry.currentSnapshot()
            val liveCodes = eligibleLive.map { it.code.trim().uppercase() }.toSet()
            val remappedDemo = demoSnap.coupons.map { c ->
                if (c.retailerId.isBlank()) c
                else c.copy(retailerId = remapDemoRetailerId(c.retailerId, demoSnap, filteredStores))
            }
            val extraDemo = CatalogQueryPolicy.filterEligibleCoupons(remappedDemo)
                .filter { it.code.trim().uppercase() !in liveCodes }
            eligibleLive + extraDemo
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val promotions: StateFlow<List<PromotionEntity>> =
        combine(promotionsStream, stores, demoEnabled) { allPromos, filteredStores, demoOn ->
            val storeIds = filteredStores.map { it.id }.toSet()
            val livePromos = CatalogQueryPolicy.filterEligiblePromotions(allPromos, storeIds)
            if (!demoOn) return@combine livePromos

            val demoSnap = DemoRegistry.currentSnapshot()
            val remappedDemo = demoSnap.promotions.map { p ->
                p.copy(retailerId = remapDemoRetailerId(p.retailerId, demoSnap, filteredStores))
            }
            val demoPromos = CatalogQueryPolicy.filterEligiblePromotions(remappedDemo, storeIds)
            (livePromos + demoPromos).distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clearanceBundles: StateFlow<List<ClearanceBundle>> =
        combine(stores, demoEnabled) { filteredStores, demoOn ->
            if (!demoOn) return@combine emptyList()
            val storeIds = filteredStores.map { it.id }.toSet()
            val demoSnap = DemoRegistry.currentSnapshot()
            val now = System.currentTimeMillis()
            demoSnap.clearanceBundles
                .map { b -> b.copy(retailerId = remapDemoRetailerId(b.retailerId, demoSnap, filteredStores)) }
                .filter { b -> b.retailerId in storeIds && (b.expiryDateMillis == 0L || b.expiryDateMillis > now) }
                .sortedByDescending { it.discountPercentage() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> =
        combine(repository.notifications, demoEnabled, localReadNotificationIds) { liveNotifs, demoOn, readIds ->
            val base = if (liveNotifs.isNotEmpty()) {
                liveNotifs
            } else if (demoOn) {
                DemoRegistry.currentSnapshot().notifications
            } else {
                emptyList()
            }
            base.map { notif ->
                if (notif.id in readIds) notif.copy(isRead = true) else notif
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markNotificationRead(notificationId: String) {
        localReadNotificationIds.value = localReadNotificationIds.value + notificationId
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun markAllNotificationsRead() {
        val ids = notifications.value.map { it.id }.toSet()
        localReadNotificationIds.value = localReadNotificationIds.value + ids
        viewModelScope.launch {
            ids.forEach { repository.markNotificationRead(it) }
        }
    }

    val savedDeals: StateFlow<List<ProductEntity>> = repository.savedDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedFlyers: StateFlow<List<FlyerEntity>> = repository.savedFlyers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCoupons: StateFlow<List<CouponEntity>> = repository.savedCoupons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followedRetailers: StateFlow<List<RetailerEntity>> = repository.followedRetailers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSavedDeal(product: ProductEntity) {
        viewModelScope.launch { repository.toggleSavedDeal(product) }
    }

    fun toggleSavedFlyer(flyer: FlyerEntity) {
        viewModelScope.launch { repository.toggleSavedFlyer(flyer) }
    }

    fun toggleSavedCoupon(coupon: CouponEntity) {
        viewModelScope.launch { repository.toggleSavedCoupon(coupon) }
    }

    fun toggleFollowedRetailer(retailer: RetailerEntity) {
        viewModelScope.launch { repository.toggleFollowedRetailer(retailer) }
    }

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestedSearches: StateFlow<List<String>> = demoEnabled
        .map { demoOn ->
            val demoTerms = if (demoOn) DemoRegistry.currentSnapshot().trendingSearches else emptyList()
            (demoTerms + CatalogQueryPolicy.GENERIC_SUGGESTED_SEARCHES).distinct()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CatalogQueryPolicy.GENERIC_SUGGESTED_SEARCHES)

    @Deprecated("Use suggestedSearches", ReplaceWith("suggestedSearches"))
    val trendingSearches: StateFlow<List<String>> = suggestedSearches

    private val searchQueryFlow = MutableStateFlow("")

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    private fun String.normalize(): String {
        val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return decomposed
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    val searchResultsProducts = searchQueryFlow
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                products
            } else {
                combine(repository.searchProducts(query, productsPageLimit.value), products, stores) { searched, activeProducts, activeStores ->
                    val words = query.normalize().split(" ").filter { it.isNotBlank() }
                    val storeIds = activeStores.map { it.id }.toSet()
                    val eligibleRemote = CatalogQueryPolicy.filterEligibleProducts(searched, storeIds)
                    fun matchesQuery(p: ProductEntity): Boolean {
                        val keywords = p.searchKeywords.joinToString(" ").normalize()
                        val searchStr = "${p.name.normalize()} ${p.brand.normalize()} ${p.category.normalize()} $keywords"
                        return words.all { w -> searchStr.contains(w) }
                    }
                    val inMemory = activeProducts.filter(::matchesQuery)
                    val remote = eligibleRemote.filter(::matchesQuery)
                    CatalogQueryPolicy.prioritizeNestleShowcase((inMemory + remote).distinctBy { it.id })
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsStores = searchQueryFlow
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) stores else {
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
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) flyers else {
                combine(repository.searchFlyers(query), flyers, stores) { searched, activeFlyers, activeStores ->
                    val storeIds = activeStores.map { it.id }.toSet()
                    val eligibleRemote = CatalogQueryPolicy.filterEligibleFlyers(searched, storeIds)
                    val q = query.normalize()
                    val inMemory = activeFlyers.filter { f ->
                        val storeName = activeStores.firstOrNull { it.id == f.retailerId }?.name.orEmpty()
                        f.title.normalize().contains(q) ||
                            storeName.normalize().contains(q) ||
                            f.searchKeywords.any { it.normalize().contains(q) }
                    }
                    (inMemory + eligibleRemote).distinctBy { it.id }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertSearchHistory(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.insertSearchHistory(clean)
        }
    }

    fun deleteSearchHistory(query: String) {
        viewModelScope.launch {
            repository.deleteSearchHistory(query)
        }
    }

    val branches: StateFlow<List<BranchEntity>> =
        combine(branchesStream, allStoresCombined, demoEnabled) { liveBranches, allStores, demoOn ->
            if (!demoOn) liveBranches
            else {
                val demoSnap = DemoRegistry.currentSnapshot()
                val remapped = demoSnap.branches.map { b ->
                    b.copy(retailerId = remapDemoRetailerId(b.retailerId, demoSnap, allStores))
                }
                (liveBranches + remapped).distinctBy { it.id }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getStore(id: String): RetailerEntity? {
        return allStoresCombined.value.find { it.id == id }
    }

    fun getBranchesForStoreInEmirate(retailerId: String): List<BranchEntity> {
        val emirate = selectedEmirate.value
        val storeBranches = branches.value.filter { it.retailerId == retailerId }
        val inEmirate = storeBranches.filter { it.emirate.equals(emirate, ignoreCase = true) }
        return if (inEmirate.isNotEmpty()) inEmirate else storeBranches
    }

    fun getDistanceToStore(retailerId: String): Double? {
        val userLoc = locationManager.userLocation.value
        val fallbackEmirate = uaeEmirates.find { it.name == selectedEmirate.value }

        val lat = userLoc?.first ?: fallbackEmirate?.lat ?: return null
        val lon = userLoc?.second ?: fallbackEmirate?.lon ?: return null

        val storeBranches = branches.value.filter { it.retailerId == retailerId && it.isEligibleForDistance() }
        if (storeBranches.isNotEmpty()) {
            return storeBranches.minOfOrNull { branch ->
                locationManager.distance(lat, lon, branch.latitude, branch.longitude)
            }
        }
        return null
    }

    fun getNearestBranchAndDistance(retailerId: String): Pair<BranchEntity?, Double?> {
        val userLoc = locationManager.userLocation.value
        val fallbackEmirate = uaeEmirates.find { it.name == selectedEmirate.value }

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
        private val locationManager: LocationManager
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
