package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DealFinderRepository
import com.example.data.FlyerEntity
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.combine

class DealFinderViewModel(private val repository: DealFinderRepository, private val locationManager: com.example.data.LocationManager) : ViewModel() {
    val isLoaded: StateFlow<Boolean> = combine(repository.retailers, repository.flyers, repository.products) { _, _, _ -> true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedEmirate = locationManager.selectedEmirate

    val stores: StateFlow<List<RetailerEntity>> = combine(repository.retailers, selectedEmirate) { allStores, emirate ->
        allStores.filter { it.emirates.contains(emirate, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flyers: StateFlow<List<FlyerEntity>> = combine(repository.flyers, stores) { allFlyers, filteredStores ->
        val storeIds = filteredStores.map { it.id }.toSet()
        allFlyers.filter { it.retailerId in storeIds && (it.status.lowercase() == "published") && (it.validTo == 0L || it.validTo > System.currentTimeMillis()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = combine(repository.products, stores) { allProducts, filteredStores ->
        val storeIds = filteredStores.map { it.id }.toSet()
        allProducts.filter { it.retailerId in storeIds && it.status.lowercase() == "published" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coupons: StateFlow<List<com.example.data.CouponEntity>> = repository.allCoupons
        .map { allCoupons -> allCoupons.filter { (it.expiry == 0L || it.expiry > System.currentTimeMillis()) && it.status.lowercase() == "published" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val promotions: StateFlow<List<com.example.data.PromotionEntity>> = combine(repository.promotions, stores) { allPromos, filteredStores ->
        val storeIds = filteredStores.map { it.id }.toSet()
        allPromos.filter { it.retailerId in storeIds && (it.validTo == 0L || it.validTo > System.currentTimeMillis()) && it.status.lowercase() == "published" }
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

    val trendingSearches = flowOf(listOf(
        "Coca Cola 330ml", "Milk 1L", "Eggs 30", "Chicken Breast", "Detergent", "Rice 5kg"
    )).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    private val searchQueryFlow = MutableStateFlow("")
    
    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    private fun String.normalize() = this.replace(Regex("[^A-Za-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim().lowercase()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val searchResultsProducts = searchQueryFlow
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                products
            } else {
                repository.searchProducts(query).map { searched ->
                    val words = query.normalize().split(" ").filter { it.isNotBlank() }
                    val inMemory = products.value.filter { p ->
                        val searchStr = "${p.name.normalize()} ${p.brand.normalize()} ${p.category.normalize()}"
                        words.all { w -> searchStr.contains(w) }
                    }
                    val remote = searched.filter { p ->
                        val searchStr = "${p.name.normalize()} ${p.brand.normalize()} ${p.category.normalize()}"
                        words.all { w -> searchStr.contains(w) }
                    }
                    (inMemory + remote).distinctBy { it.id }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val searchResultsStores = searchQueryFlow
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList<RetailerEntity>()) else {
                repository.searchRetailers(query).map { searched ->
                    val words = query.normalize().split(" ").filter { it.isNotBlank() }
                    val inMemory = stores.value.filter { s ->
                        val searchStr = "${s.name.normalize()} ${s.emirates.normalize()}"
                        words.all { w -> searchStr.contains(w) }
                    }
                    (inMemory + searched).distinctBy { it.id }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val searchResultsFlyers = searchQueryFlow
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList<FlyerEntity>()) else {
                repository.searchFlyers(query).map { searched ->
                    val inMemory = flyers.value.filter { f ->
                        f.title.normalize().contains(query.normalize())
                    }
                    (inMemory + searched).distinctBy { it.id }
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

    val branches: StateFlow<List<com.example.data.BranchEntity>> = repository.allBranches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getStore(id: String): RetailerEntity? {
        return stores.value.find { it.id == id }
    }

    fun getDistanceToStore(retailerId: String): Double? {
        val userLoc = locationManager.userLocation.value 
        val fallbackEmirate = com.example.data.uaeEmirates.find { it.name == selectedEmirate.value }
        
        val lat = userLoc?.first ?: fallbackEmirate?.lat ?: return null
        val lon = userLoc?.second ?: fallbackEmirate?.lon ?: return null

        val storeBranches = branches.value.filter { it.retailerId == retailerId }
        if (storeBranches.isNotEmpty()) {
            return storeBranches.map { branch ->
                locationManager.distance(lat, lon, branch.latitude, branch.longitude)
            }.minOrNull()
        }
        val store = getStore(retailerId) ?: return null
        val storeEmirate = com.example.data.uaeEmirates.find { store.emirates.contains(it.name, true) } ?: return null
        return locationManager.distance(lat, lon, storeEmirate.lat, storeEmirate.lon)
    }

    fun getNearestBranchAndDistance(retailerId: String): Pair<com.example.data.BranchEntity?, Double?> {
        val userLoc = locationManager.userLocation.value 
        val fallbackEmirate = com.example.data.uaeEmirates.find { it.name == selectedEmirate.value }
        
        val lat = userLoc?.first ?: fallbackEmirate?.lat ?: return Pair(null, null)
        val lon = userLoc?.second ?: fallbackEmirate?.lon ?: return Pair(null, null)

        val storeBranches = branches.value.filter { it.retailerId == retailerId }
        if (storeBranches.isNotEmpty()) {
            val nearest = storeBranches.minByOrNull { branch ->
                locationManager.distance(lat, lon, branch.latitude, branch.longitude)
            }
            val dist = nearest?.let { locationManager.distance(lat, lon, it.latitude, it.longitude) }
            return Pair(nearest, dist)
        }
        return Pair(null, null)
    }

    class Factory(private val repo: DealFinderRepository, private val locationManager: com.example.data.LocationManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DealFinderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DealFinderViewModel(repo, locationManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
