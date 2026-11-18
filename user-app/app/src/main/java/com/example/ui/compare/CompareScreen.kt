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
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SearchHeader(userLocation.value) }

            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null, 
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Slate900,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "rice 5kg", 
                                    color = Slate400, 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                            }
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MockupYellowPill,
                            modifier = Modifier.size(42.dp),
                            onClick = { showFilterSheet = true }
                        ) {
                            Icon(
                                Icons.Default.Tune, 
                                contentDescription = "Filter", 
                                tint = Slate900,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            item {
                PopularSearchesRow(onSelect = { query -> searchQuery = query })
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val headingText = if (searchQuery.isNotBlank()) "Results for \"$searchQuery\"" else "All Deals"
                        Text(
                            text = headingText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${filteredProducts.size} results found",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { sortExpanded = true }
                        ) {
                            Text(
                                "Sort by ", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = Slate500
                            )
                            Text(
                                sortBy, 
                                style = MaterialTheme.typography.labelMedium, 
                                color = Color(0xFF6D28D9), 
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp), 
                                tint = Color(0xFF6D28D9)
                            )
                        }
                        DropdownMenu(
                            expanded = sortExpanded, 
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            listOf("Relevance", "Cheapest", "Nearest", "Biggest Discount", "Newest").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) }, 
                                    onClick = { 
                                        sortBy = option 
                                        sortExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = Slate400, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No matching offers found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                "Try clearing filters or searching a different brand or pack size.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                }
            } else {
                items(filteredProducts) { product ->
                    val identicals = filteredProducts.filter { it.isIdenticalTo(product) }
                    val minPrice = identicals.minByOrNull { it.currentPrice }?.currentPrice ?: Double.MAX_VALUE
                    val isCheapest = identicals.size > 1 && product.currentPrice <= minPrice

                    val (nearestBranch, distanceKm) = viewModel.getNearestBranchAndDistance(product.retailerId)
                    SearchResultItem(
                        product = product,
                        store = viewModel.getStore(product.retailerId),
                        isCheapest = isCheapest,
                        distanceKm = distanceKm,
                        nearestBranchName = nearestBranch?.name,
                        onAddToList = { onAddToList(product) },
                        onClick = { onNavigateToProduct(product.id) },
                        onStoreClick = { onNavigateToRetailer(product.retailerId) }
                    )
                }
            }

            item { MissingItemBanner() }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PopularSearchesRow(onSelect: (String) -> Unit) {
    val items = listOf(
        Triple("Milk", Icons.Outlined.LocalDrink, MockupBlueBg to Color(0xFF0284C7)),
        Triple("Chicken", Icons.Outlined.Restaurant, MockupPeachBg to Color(0xFFEA580C)),
        Triple("Detergent", Icons.Outlined.CleaningServices, MockupPurpleBg to Color(0xFF7C3AED)),
        Triple("Banana", Icons.Outlined.Eco, MockupYellowContainer to Color(0xFFD97706)),
        Triple("Eggs", Icons.Outlined.Egg, MockupMintBg to Color(0xFF16A34A))
    )

    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = "Popular Searches",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { (name, icon, colors) ->
                val (bg, iconColor) = colors
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = bg,
                    modifier = Modifier.clickable { onSelect(name) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }
                }
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
                    text = "Search",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Find products, brands, stores and more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp),
                            tint = Slate600
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            userLocation, 
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Slate500
                        )
                    }
                }
                BadgedBox(
                    badge = { 
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Notifications, 
                            contentDescription = "Notifications", 
                            modifier = Modifier.padding(10.dp),
                            tint = Slate800
                        )
                    }
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
    val unitPriceText = com.example.domain.basket.OfferEligibility.computeUnitPriceText(product)
    val comparisonOldPrice = product.oldPrice
    val hasValidComparisonPrice = comparisonOldPrice != null && comparisonOldPrice > product.currentPrice
    val savings = if (hasValidComparisonPrice) (comparisonOldPrice!! - product.currentPrice) else 0.0
    val savedPercentage = if (hasValidComparisonPrice && comparisonOldPrice!! > 0) ((savings / comparisonOldPrice) * 100).toInt() else 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.size(80.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                    error = coil.compose.rememberAsyncImagePainter("file:///android_asset/products/default_deal.jpg"),
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Slate900
                )
                Text(
                    text = product.category.ifBlank { "General" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sourceBadgeText = when (product.verificationLabel) {
                        "merchant_confirmed" -> "Merchant Confirmed"
                        "admin_reviewed" -> "Reviewed Offer"
                        else -> "Flyer Listing"
                    }
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = sourceBadgeText,
                            color = Slate600,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (unitPriceText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = unitPriceText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Slate500
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (isCheapest) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Cheapest",
                            color = Color(0xFF15803D),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = "AED %.2f".format(product.currentPrice),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCheapest) Color(0xFFDC2626) else Slate900
                )
                if (hasValidComparisonPrice) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AED %.2f".format(product.oldPrice),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Slate400,
                            textDecoration = TextDecoration.LineThrough
                        )
                        if (savedPercentage > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "-$savedPercentage%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = store?.logoUrl ?: "file:///android_asset/logos/default_logo.png",
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = store?.name ?: "Unknown Retailer",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Slate600,
                            maxLines = 1
                        )
                        Text(
                            text = if (distanceKm != null) "%.1f km straight-line".format(distanceKm) else "Distance unavailable",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissingItemBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF3E8FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape, 
                color = Color.White, 
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Outlined.LocalOffer, 
                    contentDescription = null, 
                    modifier = Modifier.padding(11.dp), 
                    tint = Color(0xFF6D28D9)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Can't find what you're looking for?", 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Create a price alert and we'll notify you when a deal is available.", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Slate600
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = { android.widget.Toast.makeText(context, "Price alert created for this search", android.widget.Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Create Alert", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
            }
        }
    }
}
