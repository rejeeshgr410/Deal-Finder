package com.example.ui.compare

import com.example.data.isIdenticalTo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: com.example.ui.DealFinderViewModel, 
    onAddToList: (com.example.data.ProductEntity) -> Unit, 
    listViewModel: com.example.ui.lists.ListViewModel? = null,
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {},
    onNavigateToFlyer: (String) -> Unit = {},
    onNavigateToList: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Relevance") }
    var sortExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Filters
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedRetailerId by remember { mutableStateOf<String?>(null) }
    var maxPrice by remember { mutableStateOf(500f) }
    var minDiscount by remember { mutableStateOf(0f) }

    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }

    val products by viewModel.searchResultsProducts.collectAsState()
    val stores by viewModel.searchResultsStores.collectAsState()
    val flyers by viewModel.searchResultsFlyers.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val trendingSearches by viewModel.trendingSearches.collectAsState()
    val userLocation = viewModel.selectedEmirate.collectAsState()
    val shoppingItems = listViewModel?.uiState?.collectAsState()?.value ?: emptyList()
    val allProducts by viewModel.products.collectAsState()
    val allStores by viewModel.stores.collectAsState()
    
    val filteredProducts = products.filter { product ->
        val matchesCategory = selectedCategory == null || product.category.equals(selectedCategory, true)
        val matchesRetailer = selectedRetailerId == null || product.retailerId == selectedRetailerId
        val matchesPrice = product.currentPrice <= maxPrice
        val discount = if (product.oldPrice != null && product.oldPrice > 0) ((product.oldPrice - product.currentPrice)/product.oldPrice * 100).toFloat() else 0f
        val matchesDiscount = discount >= minDiscount
        matchesCategory && matchesRetailer && matchesPrice && matchesDiscount
    }.let { list ->
        when (sortBy) {
            "Cheapest" -> list.sortedBy { it.currentPrice }
            "Nearest" -> list.sortedBy { viewModel.getDistanceToStore(it.retailerId) ?: Double.MAX_VALUE }
            "Biggest Discount" -> list.sortedByDescending { if (it.oldPrice != null && it.oldPrice > 0) (it.oldPrice - it.currentPrice)/it.oldPrice else 0.0 }
            "Newest" -> list.sortedByDescending { it.validFrom }
            else -> list
        }
    }
    
    val filteredStores = stores
    val filteredFlyers = flyers
    
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
           Column(modifier = Modifier.padding(20.dp).padding(bottom = 32.dp)) {
               Text("Filter Deals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
               Spacer(modifier = Modifier.height(16.dp))
               
               Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
               LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                   val cats = listOf("All", "Groceries", "Electronics", "Fashion", "Home")
                   items(cats) { cat ->
                       val isSelected = (cat == "All" && selectedCategory == null) || cat == selectedCategory
                       FilterChip(
                           selected = isSelected, 
                           onClick = { selectedCategory = if (cat == "All") null else cat }, 
                           label = { Text(cat, style = MaterialTheme.typography.labelMedium) },
                           shape = RoundedCornerShape(10.dp),
                           colors = FilterChipDefaults.filterChipColors(
                               selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                               selectedLabelColor = MaterialTheme.colorScheme.primary
                           )
                       )
                   }
               }

               Spacer(modifier = Modifier.height(8.dp))
               Text("Hypermarket", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
               val allStores = stores
               LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                   item {
                       FilterChip(
                           selected = selectedRetailerId == null, 
                           onClick = { selectedRetailerId = null }, 
                           label = { Text("All Stores", style = MaterialTheme.typography.labelMedium) },
                           shape = RoundedCornerShape(10.dp),
                           colors = FilterChipDefaults.filterChipColors(
                               selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                               selectedLabelColor = MaterialTheme.colorScheme.primary
                           )
                       )
                   }
                   items(allStores) { store ->
                       FilterChip(
                           selected = selectedRetailerId == store.id, 
                           onClick = { selectedRetailerId = store.id }, 
                           label = { Text(store.name, style = MaterialTheme.typography.labelMedium) },
                           shape = RoundedCornerShape(10.dp),
                           colors = FilterChipDefaults.filterChipColors(
                               selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                               selectedLabelColor = MaterialTheme.colorScheme.primary
                           )
                       )
                   }
               }
               
               Spacer(modifier = Modifier.height(12.dp))
               Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                   Text("Max Price", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                   Text("AED ${maxPrice.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
               }
               Slider(
                   value = maxPrice, 
                   onValueChange = { maxPrice = it }, 
                   valueRange = 0f..1000f,
                   colors = SliderDefaults.colors(
                       thumbColor = MaterialTheme.colorScheme.primary,
                       activeTrackColor = MaterialTheme.colorScheme.primary
                   )
               )
               
               Spacer(modifier = Modifier.height(8.dp))
               Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                   Text("Minimum Discount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                   Text("${minDiscount.toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
               }
               Slider(
                   value = minDiscount, 
                   onValueChange = { minDiscount = it }, 
                   valueRange = 0f..100f,
                   colors = SliderDefaults.colors(
                       thumbColor = MaterialTheme.colorScheme.primary,
                       activeTrackColor = MaterialTheme.colorScheme.primary
                   )
               )
               
               Spacer(modifier = Modifier.height(20.dp))
               Button(
                   onClick = { showFilterSheet = false }, 
                   modifier = Modifier.fillMaxWidth().height(48.dp),
                   shape = RoundedCornerShape(12.dp),
                   colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
               ) {
                   Text("Apply Filters", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
               }
           }
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SearchHeader(userLocation.value) }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Product Search", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Basket Optimizer", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            if (selectedTab == 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "UAE Multi-Store Price Engine",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Automatically calculates the cheapest hypermarket (Lulu, Carrefour, Nesto & Viva) for your entire cart.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                if (shoppingItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.ShoppingBasket,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Your Shopping Basket is Empty",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Add groceries or deals to your list, and this engine will calculate the cheapest supermarket and optimal split strategy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(onClick = onNavigateToList) {
                                    Text("Open My Shopping List")
                                }
                            }
                        }
                    }
                } else {
                    item {
                        com.example.ui.lists.BasketOptimizationEngine(
                            items = shoppingItems,
                            products = allProducts,
                            stores = allStores,
                            getNearestBranchAndDistance = { viewModel.getNearestBranchAndDistance(it) }
                        )
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search products, brands, stores…", 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                }
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Cancel, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp),
                                onClick = { showFilterSheet = true }
                            ) {
                                Icon(
                                    Icons.Outlined.FilterList, 
                                    contentDescription = "Filter", 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

            if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Recent Searches", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recentSearches) { search ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    onClick = { searchQuery = search.query }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
                                         Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                         Spacer(modifier = Modifier.width(6.dp))
                                         androidx.compose.foundation.layout.Box(modifier = Modifier.padding(vertical = 6.dp)) {
                                             Text(search.query, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                         }
                                         Spacer(modifier = Modifier.width(4.dp))
                                         IconButton(onClick = { viewModel.deleteSearchHistory(search.query) }, modifier = Modifier.size(22.dp)) {
                                             Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                         }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (searchQuery.isBlank() && trendingSearches.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("Trending in UAE", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(trendingSearches) { search ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    onClick = { searchQuery = search }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                         Icon(Icons.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                         Spacer(modifier = Modifier.width(6.dp))
                                         Text(search, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (searchQuery.isNotBlank()) {
                item {
                    LaunchedEffect(searchQuery) {
                        kotlinx.coroutines.delay(1500)
                        viewModel.insertSearchHistory(searchQuery)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Results for \"$searchQuery\"", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { sortExpanded = true }
                            ) {
                                Text("Sort by: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(sortBy, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                                listOf("Relevance", "Cheapest", "Nearest", "Biggest Discount", "Newest").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { sortBy = option; sortExpanded = false })
                                }
                            }
                        }
                    }
                }
            }

            if (searchQuery.isNotBlank() && (filteredStores.isNotEmpty() || filteredFlyers.isNotEmpty())) {
                item {
                    if (filteredStores.isNotEmpty()) {
                        Text("Matching Stores", modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(filteredStores) { store ->
                                Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.size(80.dp), onClick = { onNavigateToRetailer(store.id) }) {
                                    AsyncImage(
                                        model = store.logoUrl.ifBlank { "file:///android_asset/logos/default_logo.png" },
                                        error = coil.compose.rememberAsyncImagePainter("file:///android_asset/logos/default_logo.png"),
                                        contentDescription = null,
                                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                    if (filteredFlyers.isNotEmpty()) {
                        Text("Matching Flyers", modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(filteredFlyers) { flyer ->
                                Card(
                                    onClick = { onNavigateToFlyer(flyer.id) },
                                    modifier = Modifier.width(140.dp).height(180.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(flyer.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("View Flyer", color = Purple40, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(filteredProducts) { product ->
                val identicals = filteredProducts.filter { 
                    it.isIdenticalTo(product)
                }
                val minPrice = identicals.minByOrNull { it.currentPrice }?.currentPrice ?: Double.MAX_VALUE
                val isCheapest = product.currentPrice <= minPrice

                val (nearestBranch, distanceKm) = viewModel.getNearestBranchAndDistance(product.retailerId)
                SearchResultItem(
                    product = product,
                    store = viewModel.getStore(product.retailerId),
                    isCheapest = isCheapest,
                    distanceKm = distanceKm,
                    nearestBranchName = nearestBranch?.name,
                    onAddToList = { onAddToList(product) },
                    onClick = { onNavigateToProduct(product.id) },
                    onStoreClick = { product.retailerId?.let { onNavigateToRetailer(it) } }
                )
            }
            }
            
            // item { MissingItemBanner() }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun MissingItemBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = LightPeriwinkle
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
             Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(48.dp)) {
                  Icon(Icons.Outlined.LocalOffer, contentDescription = null, modifier = Modifier.padding(12.dp), tint = Purple40)
             }
             Spacer(modifier = Modifier.width(16.dp))
             Column(modifier = Modifier.weight(1f)) {
                 Text("Can't find what you're looking for?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                 Spacer(modifier = Modifier.height(4.dp))
                 Text("Create a price alert and we'll notify you when a deal is available.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
             }
             Spacer(modifier = Modifier.width(16.dp))
             val context = androidx.compose.ui.platform.LocalContext.current
             Button(
                 onClick = { android.widget.Toast.makeText(context, "Alert created successfully", android.widget.Toast.LENGTH_SHORT).show() },
                 colors = ButtonDefaults.buttonColors(containerColor = Purple40),
                 shape = RoundedCornerShape(20.dp),
             ) {
                 Text("Create Alert", color = Color.White)
                 Spacer(modifier = Modifier.width(8.dp))
                 Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
             }
        }
    }
}

@Composable
fun SearchHeader(userLocation: String) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Price Compare",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Find the lowest price in UAE stores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LocationOn, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        userLocation, 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultItem(
    product: ProductEntity,
    store: RetailerEntity? = null,
    isCheapest: Boolean = false,
    distanceKm: Double? = null,
    nearestBranchName: String? = null,
    onClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onAddToList: () -> Unit
) {
    val unitPrice = if (product.unitSize > 0) product.currentPrice / product.unitSize else product.currentPrice
    val savings = if (product.oldPrice != null) product.oldPrice - product.currentPrice else 0.0
    val savedPercentage = if (product.oldPrice != null && product.oldPrice > 0) (savings / product.oldPrice) * 100 else 0.0

    val validityText = if (product.validTo > 0) {
        val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
        "Until ${sdf.format(java.util.Date(product.validTo))}"
    } else {
        "Active offer"
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(92.dp)
                ) {
                    AsyncImage(
                        model = product.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                        error = coil.compose.rememberAsyncImagePainter("file:///android_asset/products/default_deal.jpg"),
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                product.name, 
                                fontWeight = FontWeight.SemiBold, 
                                style = MaterialTheme.typography.titleSmall, 
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                product.brand, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCheapest) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer, 
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "CHEAPEST", 
                                    color = MaterialTheme.colorScheme.primary, 
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "AED ", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "%.2f".format(product.currentPrice), 
                                    fontWeight = FontWeight.Bold, 
                                    style = MaterialTheme.typography.titleMedium, 
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (product.oldPrice != null && savedPercentage > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "AED %.2f".format(product.oldPrice),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer, 
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "-${savedPercentage.toInt()}%", 
                                            color = MaterialTheme.colorScheme.error, 
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        FilledTonalButton(
                            onClick = onAddToList,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onStoreClick)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(24.dp)
                    ) {
                        AsyncImage(
                            model = store?.logoUrl ?: "file:///android_asset/logos/default_logo.png", 
                            error = coil.compose.rememberAsyncImagePainter("file:///android_asset/logos/default_logo.png"),
                            contentDescription = null, 
                            modifier = Modifier.fillMaxSize().padding(2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        store?.name ?: "Store", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.SemiBold
                    )
                    if (distanceKm != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "• ${"%.1f".format(distanceKm)} km away", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    validityText, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
