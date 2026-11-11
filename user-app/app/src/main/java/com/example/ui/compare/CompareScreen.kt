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
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {},
    onNavigateToFlyer: (String) -> Unit = {}
) {
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
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
           Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
               Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
               Spacer(modifier = Modifier.height(16.dp))
               
               Text("Category", style = MaterialTheme.typography.titleSmall)
               LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                   val cats = listOf("All", "Groceries", "Electronics", "Fashion", "Home")
                   items(cats) { cat ->
                       val isSelected = (cat == "All" && selectedCategory == null) || cat == selectedCategory
                       FilterChip(selected = isSelected, onClick = { selectedCategory = if (cat == "All") null else cat }, label = { Text(cat) })
                   }
               }

               Text("Retailer", style = MaterialTheme.typography.titleSmall)
               val allStores = stores
               LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                   item {
                       FilterChip(selected = selectedRetailerId == null, onClick = { selectedRetailerId = null }, label = { Text("All") })
                   }
                   items(allStores) { store ->
                       FilterChip(selected = selectedRetailerId == store.id, onClick = { selectedRetailerId = store.id }, label = { Text(store.name) })
                   }
               }
               
               Text("Max Price: AED ${maxPrice.toInt()}", style = MaterialTheme.typography.titleSmall)
               Slider(value = maxPrice, onValueChange = { maxPrice = it }, valueRange = 0f..1000f)
               
               Text("Min Discount: ${minDiscount.toInt()}%", style = MaterialTheme.typography.titleSmall)
               Slider(value = minDiscount, onValueChange = { minDiscount = it }, valueRange = 0f..100f)
               
               Spacer(modifier = Modifier.height(16.dp))
               Button(onClick = { showFilterSheet = false }, modifier = Modifier.fillMaxWidth()) {
                   Text("Apply Filters")
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
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                            if (searchQuery.isEmpty()) {
                                Text("Search products, brands, stores...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.CenterStart))
                            }
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Cancel, contentDescription = "Clear", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(GoldBackground, CircleShape)
                                .clickable { showFilterSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.FilterList, contentDescription = null, tint = Color.Black)
                        }
                    }
                }
            }

            if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text("Recent Searches", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentSearches) { search ->
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    onClick = { searchQuery = search.query }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                                         Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                         Spacer(modifier = Modifier.width(8.dp))
                                         androidx.compose.foundation.layout.Box(modifier = Modifier.padding(vertical = 6.dp)) {
                                             Text(search.query, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                         }
                                         Spacer(modifier = Modifier.width(4.dp))
                                         IconButton(onClick = { viewModel.deleteSearchHistory(search.query) }, modifier = Modifier.size(24.dp)) {
                                             Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text("Trending Searches", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(trendingSearches) { search ->
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    onClick = { searchQuery = search }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                         Icon(Icons.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = Purple40)
                                         Spacer(modifier = Modifier.width(8.dp))
                                         Text(search, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Results for \"$searchQuery\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { sortExpanded = true }
                            ) {
                                Text("Sort by", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sortBy, style = MaterialTheme.typography.labelMedium, color = Purple40, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Purple40)
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
                                    AsyncImage(model = store.logoUrl, contentDescription = null, modifier = Modifier.padding(16.dp).fillMaxSize(), contentScale = ContentScale.Fit)
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
                    text = "Search",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Find products across all stores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(userLocation, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                BadgedBox(
                    badge = { Badge(modifier = Modifier.offset(x = (-6).dp, y = 6.dp), containerColor = Color(0xFFE53935)) { } }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.padding(8.dp))
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
    val unitPrice = if (product.unitSize > 0) product.currentPrice / product.unitSize else product.currentPrice
    val savings = if (product.oldPrice != null) product.oldPrice - product.currentPrice else 0.0
    val savedPercentage = if (product.oldPrice != null && product.oldPrice > 0) (savings / product.oldPrice) * 100 else 0.0

    val validityText = if (product.validTo > 0) {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        "Until ${sdf.format(java.util.Date(product.validTo))}"
    } else {
        "Always Valid"
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                         Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(product.brand, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isCheapest) {
                        Surface(color = CatGreen, shape = RoundedCornerShape(8.dp)) {
                            Text("Cheapest", color = Teal500, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                         Surface(color = CatGreen, shape = RoundedCornerShape(8.dp)) {
                             Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                 Text("4.6", style = MaterialTheme.typography.labelSmall, color = Teal500, fontWeight = FontWeight.Bold)
                                 Icon(Icons.Default.Star, contentDescription = null, tint = Teal500, modifier = Modifier.size(10.dp).padding(start = 2.dp))
                             }
                         }
                         Spacer(modifier = Modifier.height(8.dp))
                         Text("1 ${product.unitType} = AED ${"%.2f".format(unitPrice)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(validityText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                         Text("AED ${"%.2f".format(product.currentPrice)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = Color(0xFFD32F2F))
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             if (product.oldPrice != null && savedPercentage > 0) {
                                 Text(
                                     "AED ${"%.2f".format(product.oldPrice)}",
                                     style = MaterialTheme.typography.labelMedium,
                                     color = MaterialTheme.colorScheme.outline,
                                     textDecoration = TextDecoration.LineThrough
                                 )
                                 Spacer(modifier = Modifier.width(6.dp))
                                 Surface(color = CatRed, shape = RoundedCornerShape(6.dp)) {
                                     Text("Save AED ${"%.2f".format(savings)} (-${savedPercentage.toInt()}%)", color = CatRedIcon, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                 }
                             }
                         }
                         Spacer(modifier = Modifier.height(12.dp))
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier.clickable(onClick = onStoreClick)
                         ) {
                             AsyncImage(model = store?.logoUrl, contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
                             Spacer(modifier = Modifier.width(8.dp))
                             Column(horizontalAlignment = Alignment.End) {
                                 Text(store?.name ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                 if (distanceKm != null) {
                                     val branchInfo = if (nearestBranchName != null) " ($nearestBranchName)" else ""
                                      Text("${"%.1f".format(distanceKm)} km away$branchInfo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.outline)
                                 }
                             }
                         }
                    }
                }
            }
        }
    }
}
