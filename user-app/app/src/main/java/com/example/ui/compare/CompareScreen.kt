package com.example.ui.compare

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FlyerEntity
import com.example.data.LocationManager
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.data.isIdenticalTo
import com.example.domain.basket.OfferEligibility
import com.example.ui.DealFinderViewModel
import com.example.ui.lists.ListViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class SearchFilterTab(
    val label: String,
    val icon: ImageVector?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: DealFinderViewModel,
    onAddToList: (ProductEntity) -> Unit,
    listViewModel: ListViewModel? = null,
    openFiltersInitially: Boolean = false,
    onConsumedOpenFilters: () -> Unit = {},
    initialSearchQuery: String? = null,
    onConsumedInitialSearchQuery: () -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {},
    onNavigateToFlyer: (String) -> Unit = {},
    onNavigateToList: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = onNavigateToNotifications,
    onBack: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Relevance") }
    var showFilterSheet by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedRetailerId by remember { mutableStateOf<String?>(null) }
    var maxPrice by remember { mutableFloatStateOf(5000f) }
    var minDiscount by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(openFiltersInitially) {
        if (openFiltersInitially) {
            showFilterSheet = true
            onConsumedOpenFilters()
        }
    }

    LaunchedEffect(initialSearchQuery) {
        if (!initialSearchQuery.isNullOrBlank()) {
            searchQuery = initialSearchQuery
            selectedTab = 0
            onConsumedInitialSearchQuery()
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }

    val products by viewModel.searchResultsProducts.collectAsState()
    val stores by viewModel.searchResultsStores.collectAsState()
    val flyers by viewModel.searchResultsFlyers.collectAsState()
    val allStores by viewModel.stores.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val suggestedSearches by viewModel.suggestedSearches.collectAsState()
    val listItems by (listViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }
    val currency = stringResource(R.string.currency_aed)

    val filteredProducts = remember(
        products,
        selectedTab,
        selectedCategory,
        selectedRetailerId,
        maxPrice,
        minDiscount,
        sortBy,
        branches
    ) {
        products.filter { product ->
            val hasDiscount = product.oldPrice != null && product.oldPrice > product.currentPrice
            val matchesDealsTab = selectedTab != 1 || hasDiscount
            val matchesCategory = selectedCategory == null || product.category.contains(selectedCategory!!, ignoreCase = true)
            val matchesRetailer = selectedRetailerId == null || product.retailerId == selectedRetailerId
            val matchesPrice = product.currentPrice <= maxPrice
            val discount = if (product.oldPrice != null && product.oldPrice > 0) {
                ((product.oldPrice - product.currentPrice) / product.oldPrice * 100).toFloat()
            } else 0f
            matchesDealsTab && matchesCategory && matchesRetailer && matchesPrice && discount >= minDiscount
        }.let { list ->
            when (sortBy) {
                "Cheapest" -> list.sortedBy { it.currentPrice }
                "Nearest" -> list.sortedBy { viewModel.getDistanceToStore(it.retailerId) ?: Double.MAX_VALUE }
                "Biggest Discount" -> list.sortedByDescending {
                    if (it.oldPrice != null && it.oldPrice > 0) (it.oldPrice - it.currentPrice) / it.oldPrice else 0.0
                }
                else -> list
            }
        }
    }

    val filteredFlyers = remember(flyers, selectedRetailerId) {
        if (selectedRetailerId == null) flyers
        else flyers.filter { it.retailerId == selectedRetailerId }
    }

    val totalResultsCount = when (selectedTab) {
        2 -> filteredFlyers.size
        4 -> stores.size
        else -> filteredProducts.size
    }

    val filterTabs = listOf(
        SearchFilterTab(stringResource(R.string.tab_all), null),
        SearchFilterTab(stringResource(R.string.tab_deals), Icons.Outlined.LocalOffer),
        SearchFilterTab(stringResource(R.string.tab_flyers), Icons.Outlined.Storefront),
        SearchFilterTab(stringResource(R.string.tab_products), Icons.Outlined.ShoppingCart),
        SearchFilterTab(stringResource(R.string.tab_stores), Icons.Outlined.GridView)
    )

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = palette.surfaceGradStart,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_and_sort),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    TextButton(onClick = {
                        selectedCategory = null
                        selectedRetailerId = null
                        maxPrice = 5000f
                        minDiscount = 0f
                        sortBy = "Relevance"
                    }) {
                        Text(stringResource(R.string.reset_filters), color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                    }
                }

                // Sort By
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.sort_by), fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                    val sortOptions = listOf(
                        "Relevance" to stringResource(R.string.sort_relevance),
                        "Cheapest" to stringResource(R.string.sort_cheapest),
                        "Nearest" to stringResource(R.string.sort_nearest),
                        "Biggest Discount" to stringResource(R.string.sort_discount)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sortOptions) { (key, label) ->
                            val isSel = sortBy == key
                            FilterChip(
                                selected = isSel,
                                onClick = { sortBy = key },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.crimsonPillBg,
                                    selectedLabelColor = palette.crimsonLink
                                )
                            )
                        }
                    }
                }

                // Category
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.filter_category), fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                    val categories = listOf(null to stringResource(R.string.any), "Fresh Food" to "Fresh Food", "Groceries" to "Groceries", "Electronics" to "Electronics", "Home" to "Home & Kitchen", "Beauty" to "Beauty")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { (catKey, label) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = { selectedCategory = if (selectedCategory == catKey) null else catKey },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.crimsonPillBg,
                                    selectedLabelColor = palette.crimsonLink
                                )
                            )
                        }
                    }
                }

                // Store / Retailer Filter
                if (allStores.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.filter_store), fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedRetailerId == null,
                                    onClick = { selectedRetailerId = null },
                                    label = { Text(stringResource(R.string.all_stores), fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.crimsonPillBg,
                                        selectedLabelColor = palette.crimsonLink
                                    )
                                )
                            }
                            items(allStores, key = { it.id }) { store ->
                                FilterChip(
                                    selected = selectedRetailerId == store.id,
                                    onClick = {
                                        selectedRetailerId = if (selectedRetailerId == store.id) null else store.id
                                    },
                                    label = { Text(store.name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.crimsonPillBg,
                                        selectedLabelColor = palette.crimsonLink
                                    )
                                )
                            }
                        }
                    }
                }

                Column {
                    val priceLabel = if (maxPrice >= 5000f) stringResource(R.string.any) else "$currency ${maxPrice.toInt()}"
                    Text("${stringResource(R.string.max_price)}: $priceLabel", fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                    Slider(
                        value = maxPrice,
                        onValueChange = { maxPrice = it },
                        valueRange = 10f..5000f,
                        colors = SliderDefaults.colors(thumbColor = FigmaCrimsonEnd, activeTrackColor = FigmaCrimsonStart)
                    )
                }

                Column {
                    Text("${stringResource(R.string.min_discount)}: ${minDiscount.toInt()}%", fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                    Slider(
                        value = minDiscount,
                        onValueChange = { minDiscount = it },
                        valueRange = 0f..80f,
                        colors = SliderDefaults.colors(thumbColor = FigmaCrimsonEnd, activeTrackColor = FigmaCrimsonStart)
                    )
                }

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
                ) {
                    Text(stringResource(R.string.apply_filters), fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Bar: Back + Elevated Pill Search Input + Location & List Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .shadow(7.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x2224203C), spotColor = Color(0x2224203C))
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp))
                            .padding(start = 14.dp, end = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_placeholder),
                                        style = TextStyle(
                                            color = palette.textMuted,
                                            fontSize = 12.sp,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchQuery.isNotBlank()) {
                                                viewModel.insertSearchHistory(searchQuery)
                                            }
                                        }
                                    ),
                                    textStyle = TextStyle(
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    cursorBrush = SolidColor(FigmaCrimsonEnd),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear",
                                    tint = palette.textMuted,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { searchQuery = "" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                                    .clickable { showFilterSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.filter_and_sort),
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Location & Smart List quick status strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(50))
                            .clickable { onNavigateToLocation() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = FigmaCrimsonLink, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(localizedEmirate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (listItems.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.crimsonPillBg)
                                .border(1.dp, palette.crimsonPillBorder, RoundedCornerShape(50))
                                .clickable { onNavigateToList() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.action_smart_list)} (${listItems.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.crimsonLink
                            )
                        }
                    }

                    if (sortBy != "Relevance" || selectedCategory != null || selectedRetailerId != null || minDiscount > 0f || maxPrice < 5000f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFDCFCE7))
                                .clickable { showFilterSheet = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = sortBy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FigmaDiscountGreen
                            )
                        }
                    }
                }
            }
        }

        // Recent & Trending Searches when search bar is empty
        if (searchQuery.isBlank() && (recentSearches.isNotEmpty() || suggestedSearches.isNotEmpty())) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (recentSearches.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(recentSearches.take(6), key = { "recent_${it.query}" }) { hist ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(50))
                                        .clickable {
                                            searchQuery = hist.query
                                            viewModel.insertSearchHistory(hist.query)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.History, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(hist.query, fontSize = 11.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = palette.textMuted,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.deleteSearchHistory(hist.query) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (suggestedSearches.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(suggestedSearches.take(8), key = { "sug_$it" }) { term ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.subtleSurface)
                                        .border(0.8.dp, palette.cardBorder, RoundedCornerShape(50))
                                        .clickable {
                                            searchQuery = term
                                            viewModel.insertSearchHistory(term)
                                        }
                                        .padding(horizontal = 11.dp, vertical = 5.dp)
                                ) {
                                    Text(term, fontSize = 10.5.sp, color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Results Header + Horizontal Filter Chips Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(2.2.dp)
                                .height(15.dp)
                                .background(FigmaAccentBorder)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.search_results_title),
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                letterSpacing = (-0.3).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                    Text(
                        text = stringResource(R.string.results_count, totalResultsCount),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textMuted,
                            fontFeatureSettings = "tnum",
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterTabs.size) { index ->
                        val tab = filterTabs[index]
                        val selected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .shadow(
                                    elevation = if (selected) 6.dp else 4.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = if (selected) Color(0x40ED0032) else Color(0x1824203C),
                                    spotColor = if (selected) Color(0x40ED0032) else Color(0x1824203C)
                                )
                                .clip(RoundedCornerShape(50))
                                .then(
                                    if (selected) {
                                        Modifier.background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                                    } else {
                                        Modifier
                                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                                            .border(1.dp, palette.cardBorder, RoundedCornerShape(50))
                                    }
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (tab.icon != null) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = if (selected) Color.White else palette.textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = tab.label,
                                    style = TextStyle(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) Color.White else palette.textPrimary,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (syncState.isLoading && products.isEmpty() && flyers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FigmaCrimsonLink)
                }
            }
        }

        if (syncState.errorMessage != null && products.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = syncState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.refreshCatalog() }) {
                            Text(stringResource(R.string.retry), color = FigmaCrimsonLink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Best Deals Highlight + Product Comparison Items (All, Deals, Products tabs)
        if (selectedTab == 0 || selectedTab == 1 || selectedTab == 3) {
            item {
                val sectionTitle = if (searchQuery.isBlank()) {
                    stringResource(R.string.best_deals)
                } else {
                    stringResource(R.string.best_deals_for, searchQuery.trim())
                }
                SectionTitleRow(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(FigmaCrimsonLink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    },
                    title = sectionTitle,
                    actionText = if (selectedTab == 0) stringResource(R.string.view_all) else "",
                    onAction = { selectedTab = 3 }
                )
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(34.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.no_search_results), fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedCategory = null
                                selectedRetailerId = null
                                maxPrice = 5000f
                                minDiscount = 0f
                                selectedTab = 0
                            }) {
                                Text(stringResource(R.string.reset_filters), color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                val shownProducts = if (selectedTab == 0) filteredProducts.take(4) else filteredProducts
                items(shownProducts, key = { "prod_${it.id}" }) { product ->
                    val similarGroup = products.filter { it.id == product.id || product.isIdenticalTo(it) }
                    val minPriceInGroup = similarGroup.minOfOrNull { it.currentPrice } ?: product.currentPrice
                    val isCheapest = product.currentPrice <= minPriceInGroup && similarGroup.size > 1
                    val (nearestBranch, distKm) = viewModel.getNearestBranchAndDistance(product.retailerId)
                    val store = viewModel.getStore(product.retailerId)
                    SearchResultItem(
                        product = product,
                        store = store,
                        isCheapest = isCheapest,
                        distanceKm = distKm,
                        nearestBranchName = nearestBranch?.name,
                        onClick = { onNavigateToProduct(product.id) },
                        onStoreClick = {
                            if (store != null) onNavigateToRetailer(store.id)
                        },
                        onAddToList = {
                            onAddToList(product)
                            Toast.makeText(
                                context,
                                context.getString(R.string.added_to_list, product.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                if (selectedTab == 3 && syncState.hasMoreProducts) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = { viewModel.loadMoreProducts() }) {
                                Text("Load more deals", color = palette.crimsonLink)
                            }
                        }
                    }
                }
            }
        }

        // 4. Related Flyers Section (All & Flyers tabs)
        if (selectedTab == 0 || selectedTab == 2) {
            item {
                SectionTitleRow(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(FigmaCrimsonLink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    },
                    title = stringResource(R.string.related_flyers),
                    actionText = if (selectedTab == 0) stringResource(R.string.view_all) else "",
                    onAction = { selectedTab = 2 }
                )
            }

            if (filteredFlyers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_search_results), color = palette.textSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                val shownFlyers = if (selectedTab == 0) filteredFlyers.take(2) else filteredFlyers
                items(shownFlyers, key = { "flyer_${it.id}" }) { flyer ->
                    SearchLiveFlyerRowCard(
                        flyer = flyer,
                        store = viewModel.getStore(flyer.retailerId),
                        onClick = { onNavigateToFlyer(flyer.id) }
                    )
                }
            }
        }

        // 5. Popular Stores Section (All & Stores tabs)
        if (selectedTab == 0 || selectedTab == 4) {
            item {
                SectionTitleRow(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(FigmaCrimsonLink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    },
                    title = stringResource(R.string.popular_stores),
                    actionText = if (selectedTab == 0) stringResource(R.string.view_all) else "",
                    onAction = { selectedTab = 4 }
                )
            }

            if (stores.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_search_results), color = palette.textSecondary, fontSize = 12.sp)
                    }
                }
            } else if (selectedTab == 0) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stores, key = { "tile_${it.id}" }) { store ->
                            StoreSquareTile(
                                store = store,
                                onClick = { onNavigateToRetailer(store.id) }
                            )
                        }
                    }
                }
            } else {
                items(stores, key = { "store_row_${it.id}" }) { store ->
                    val dist = viewModel.getDistanceToStore(store.id)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                            .clickable { onNavigateToRetailer(store.id) }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (store.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = store.logoUrl,
                                        contentDescription = store.name,
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(store.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = FigmaTextPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(store.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.textPrimary)
                                val distTxt = if (dist != null) " • %.1f km".format(dist) else ""
                                Text(
                                    text = "${store.emirates.ifBlank { localizedEmirate }}$distTxt",
                                    fontSize = 10.5.sp,
                                    color = palette.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.crimsonLink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Get Notified Banner (All tab — routes directly to NotificationSettings!)
        if (selectedTab == 0) {
            item {
                GetNotifiedBannerCard(
                    onEnableClick = onNavigateToNotificationSettings
                )
            }
        }
    }
}

@Composable
private fun SectionTitleRow(
    icon: @Composable () -> Unit,
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            icon()
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.3).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (actionText.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAction() }
            ) {
                Text(
                    text = actionText,
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.crimsonLink,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.crimsonLink,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchLiveFlyerRowCard(
    flyer: FlyerEntity,
    store: RetailerEntity?,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val validText = if (flyer.validTo > 0) {
        val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
        stringResource(R.string.valid_till, fmt.format(Date(flyer.validTo)))
    } else "${flyer.pages} pgs"

    val daysLeft = if (flyer.validTo > System.currentTimeMillis()) {
        val d = TimeUnit.MILLISECONDS.toDays(flyer.validTo - System.currentTimeMillis()).toInt().coerceAtLeast(1)
        if (d == 1) stringResource(R.string.day_left, 1) else stringResource(R.string.days_left, d)
    } else "ACTIVE"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = flyer.imageUrl.ifBlank { store?.logoUrl ?: "" },
                    contentDescription = flyer.title,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = store?.name ?: flyer.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(validText, fontSize = 10.sp, color = palette.textMuted)
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.crimsonPillBg)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(daysLeft, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = palette.crimsonLink)
                }
            }
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(palette.crimsonPillBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.crimsonLink,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StoreSquareTile(
    store: RetailerEntity,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier.width(68.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (store.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = store.logoUrl,
                    contentDescription = store.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = store.name.take(3).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FigmaTextPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = store.name.substringBefore(" Hypermarket").substringBefore(" Supermarket"),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GetNotifiedBannerCard(onEnableClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1624203C), spotColor = Color(0x1624203C))
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (palette.isDark) {
                    Brush.horizontalGradient(listOf(Color(0xFF281722), Color(0xFF331B29)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFFEF4F8), Color(0xFFFDE6EF), Color(0xFFFBD5E3)))
                }
            )
            .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = FigmaCrimsonLink,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.get_notified),
                    style = TextStyle(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.get_notified_desc),
                    style = TextStyle(
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = palette.textSecondary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .shadow(6.dp, RoundedCornerShape(50), ambientColor = Color(0x45ED0032), spotColor = Color(0x45ED0032))
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                    .clickable { onEnableClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.enable_notifications),
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1
                )
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
    val palette = LocalAppPalette.current
    val currency = stringResource(R.string.currency_aed)
    val comparisonOldPrice = product.oldPrice
    val hasValidComparisonPrice = comparisonOldPrice != null && comparisonOldPrice > product.currentPrice
    val savedPercentage = if (comparisonOldPrice != null && comparisonOldPrice > product.currentPrice && comparisonOldPrice > 0.0) {
        (((comparisonOldPrice - product.currentPrice) / comparisonOldPrice) * 100).toInt()
    } else 0
    val unitPriceText = OfferEligibility.computeUnitPriceText(product)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.subtleSurface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = product.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                    error = coil.compose.rememberAsyncImagePainter("file:///android_asset/products/default_deal.jpg"),
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
                if (savedPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(50))
                            .background(FigmaDiscountGreen)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "-$savedPercentage%",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.textPrimary
                )
                Text(
                    text = buildString {
                        append(product.category.ifBlank { "Groceries" })
                        if (unitPriceText != null) append(" • $unitPriceText")
                    },
                    fontSize = 10.sp,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$currency %.2f".format(product.currentPrice),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = palette.crimsonLink
                    )
                    if (hasValidComparisonPrice) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$currency %.2f".format(product.oldPrice),
                            fontSize = 10.sp,
                            color = palette.textMuted,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    if (isCheapest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.sort_cheapest),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = FigmaDiscountGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (palette.isDark) Color(0xFF173521) else Color(0xFFE8F5EA))
                        .clickable { onStoreClick() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val branchPart = if (!nearestBranchName.isNullOrBlank()) " (${nearestBranchName.substringAfter("-").trim()})" else ""
                    val distLabel = if (distanceKm != null) " • %.1f km".format(distanceKm) else ""
                    Text(
                        text = "${store?.name ?: "Verified Store"}$branchPart$distLabel",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaDiscountGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.crimsonPillBg)
                    .clickable { onAddToList() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = stringResource(R.string.add_btn),
                    tint = palette.crimsonLink,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
