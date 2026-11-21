package com.example.ui.clearance

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ClearanceBundle
import com.example.data.LocationManager
import com.example.data.RetailerEntity
import com.example.ui.DealFinderViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val clearanceFilterChips = listOf(
    "All",
    "80-90% Off",
    "Expiring Soon",
    "Coffee & Beverages",
    "Snacks & Sweets",
    "Pantry & Meals",
    "Pet & Home"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearanceScreen(
    viewModel: DealFinderViewModel,
    onBack: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {},
    onAddBundleToList: (ClearanceBundle) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current

    val bundles by viewModel.clearanceBundles.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()

    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }
    val currency = stringResource(R.string.currency_aed)
    val storesById = remember(stores) { stores.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf("All") }
    var selectedRetailerId by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf("Highest Discount") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedBundle by remember { mutableStateOf<ClearanceBundle?>(null) }

    val now = System.currentTimeMillis()
    val filteredBundles = remember(bundles, searchQuery, selectedChip, selectedRetailerId, sortMode, storesById) {
        val q = searchQuery.trim().lowercase()
        bundles.filter { bundle ->
            val storeName = storesById[bundle.retailerId]?.name.orEmpty().lowercase()
            val discount = bundle.discountPercentage()
            val daysLeft = if (bundle.expiryDateMillis > now) {
                TimeUnit.MILLISECONDS.toDays(bundle.expiryDateMillis - now)
            } else 0L

            val matchesChip = when (selectedChip) {
                "All" -> true
                "80-90% Off" -> discount >= 80
                "Expiring Soon" -> daysLeft <= 2L
                else -> bundle.category.equals(selectedChip, ignoreCase = true)
            }
            val matchesStore = selectedRetailerId == null || bundle.retailerId == selectedRetailerId
            val matchesQuery = q.isEmpty() ||
                bundle.title.lowercase().contains(q) ||
                bundle.category.lowercase().contains(q) ||
                storeName.contains(q) ||
                bundle.items.any { it.name.lowercase().contains(q) }
            matchesChip && matchesStore && matchesQuery
        }.let { list ->
            when (sortMode) {
                "Expiring Soon" -> list.sortedBy { it.expiryDateMillis }
                "Lowest Price" -> list.sortedBy { it.bundleOfferPrice }
                else -> list.sortedByDescending { it.discountPercentage() }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = palette.surfaceGradStart,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
                        selectedChip = "All"
                        selectedRetailerId = null
                        sortMode = "Highest Discount"
                        searchQuery = ""
                    }) {
                        Text(stringResource(R.string.reset_filters), color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.sort_by), fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                    val sortOptions = listOf("Highest Discount", "Expiring Soon", "Lowest Price")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sortOptions) { option ->
                            FilterChip(
                                selected = sortMode == option,
                                onClick = { sortMode = option },
                                label = { Text(option, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.crimsonPillBg,
                                    selectedLabelColor = palette.crimsonLink
                                )
                            )
                        }
                    }
                }

                if (stores.isNotEmpty()) {
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
                            items(stores, key = { it.id }) { store ->
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

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
                ) {
                    Text(stringResource(R.string.apply_filters), fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    selectedBundle?.let { bundle ->
        ClearanceBundleDetailSheet(
            bundle = bundle,
            store = storesById[bundle.retailerId],
            currency = currency,
            onDismiss = { selectedBundle = null },
            onStoreClick = {
                selectedBundle = null
                if (bundle.retailerId.isNotBlank()) onNavigateToRetailer(bundle.retailerId)
            },
            onAddBundle = {
                onAddBundleToList(bundle)
                Toast.makeText(context, context.getString(R.string.added_to_list, bundle.title), Toast.LENGTH_SHORT).show()
                selectedBundle = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // 1. Top Header
        item {
            ClearanceHeaderRow(
                localizedEmirate = localizedEmirate,
                hasUnreadNotifications = unreadNotifCount > 0,
                onBack = onBack,
                onNavigateToLocation = onNavigateToLocation,
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToNotifications = onNavigateToNotifications
            )
        }

        // 2. Search Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(44.dp)
                    .shadow(7.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x2024203C), spotColor = Color(0x2024203C))
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
                    Icon(Icons.Default.Search, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.clearance_search_placeholder),
                                style = TextStyle(
                                    color = palette.textMuted,
                                    fontSize = 11.5.sp,
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
                            textStyle = TextStyle(
                                color = palette.textPrimary,
                                fontSize = 12.5.sp,
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
                                .size(19.dp)
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

        // 3. Clearance Hero Banner
        item {
            ClearanceHeroBanner()
        }

        // 4. Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clearanceFilterChips) { chip ->
                    val selected = selectedChip == chip
                    Box(
                        modifier = Modifier
                            .height(33.dp)
                            .shadow(
                                elevation = if (selected) 6.dp else 3.dp,
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
                            .clickable { selectedChip = chip }
                            .padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip,
                            style = TextStyle(
                                fontSize = 11.sp,
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

        // 5. Bundle Cards
        if (filteredBundles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.no_clearance_match), fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            searchQuery = ""
                            selectedChip = "All"
                            selectedRetailerId = null
                        }) {
                            Text(stringResource(R.string.reset_filters), color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredBundles, key = { it.id }) { bundle ->
                ClearanceBundleCard(
                    bundle = bundle,
                    store = storesById[bundle.retailerId],
                    currency = currency,
                    onClick = { selectedBundle = bundle },
                    onStoreClick = {
                        if (bundle.retailerId.isNotBlank()) onNavigateToRetailer(bundle.retailerId)
                    },
                    onAddCombo = {
                        onAddBundleToList(bundle)
                        Toast.makeText(
                            context,
                            context.getString(R.string.added_to_list, bundle.title),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun ClearanceHeaderRow(
    localizedEmirate: String,
    hasUnreadNotifications: Boolean,
    onBack: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.clearance_title),
                    style = TextStyle(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.5).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.clearance_subtitle),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = palette.textSecondary,
                        letterSpacing = (-0.25).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToLocation() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(localizedEmirate, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary, maxLines = 1)
                    Spacer(modifier = Modifier.width(1.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(13.dp), tint = palette.textPrimary)
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, CircleShape)
                    .clickable { onNavigateToWallet() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = stringResource(R.string.nav_wallet),
                    modifier = Modifier.size(16.dp),
                    tint = palette.textPrimary
                )
            }

            Box(modifier = Modifier.size(34.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable { onNavigateToNotifications() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications), modifier = Modifier.size(16.dp), tint = palette.textPrimary)
                }
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .size(6.5.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 1.dp)
                            .clip(CircleShape)
                            .background(FigmaCrimsonDot)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClearanceHeroBanner() {
    val palette = LocalAppPalette.current
    val gradientColors = if (palette.isDark) {
        listOf(Color(0xFF331521), Color(0xFF28121B), Color(0xFF3E1828))
    } else {
        listOf(FigmaBannerGradStart, FigmaBannerGradMid, FigmaBannerGradEnd)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1A252039), spotColor = Color(0x1A252039))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "UP TO 90% OFF",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.clearance_banner_title),
                    style = TextStyle(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (palette.isDark) palette.textPrimary else FigmaTextPrimary,
                        letterSpacing = (-0.3).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.clearance_banner_subtitle),
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = if (palette.isDark) palette.textSecondary else FigmaTextSecondary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = if (palette.isDark) 0.12f else 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = FigmaCrimsonEnd,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
private fun ClearanceBundleCard(
    bundle: ClearanceBundle,
    store: RetailerEntity?,
    currency: String,
    onClick: () -> Unit,
    onStoreClick: () -> Unit,
    onAddCombo: () -> Unit
) {
    val palette = LocalAppPalette.current
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val expiryFormatted = remember(bundle.expiryDateMillis) {
        if (bundle.expiryDateMillis > 0L) dateFmt.format(Date(bundle.expiryDateMillis)) else "Limited time"
    }
    val daysRemaining = remember(bundle.expiryDateMillis) {
        if (bundle.expiryDateMillis > System.currentTimeMillis()) {
            TimeUnit.MILLISECONDS.toDays(bundle.expiryDateMillis - System.currentTimeMillis()).toInt().coerceAtLeast(1)
        } else 1
    }
    val daysLeftLabel = if (daysRemaining == 1) {
        stringResource(R.string.day_left, 1)
    } else {
        stringResource(R.string.days_left, daysRemaining)
    }
    val discountPct = bundle.discountPercentage()
    val savings = bundle.savingsAmount()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1624203C), spotColor = Color(0x1624203C))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(13.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Store info + Discount % badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onStoreClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(0.8.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!store?.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = store.logoUrl,
                                contentDescription = store.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = FigmaTextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = store?.name ?: "UAE Supermarket",
                            style = TextStyle(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bundle.branchName.isNotBlank()) {
                            Text(
                                text = bundle.branchName,
                                style = TextStyle(
                                    fontSize = 9.5.sp,
                                    color = palette.textMuted,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-$discountPct% OFF",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = bundle.title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.3).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Expiry Date + Days Left + Stock Urgency Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(FigmaPinkPillStart, FigmaPinkPillSolidEnd.copy(alpha = 0.55f))))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = FigmaCrimsonText,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.expires_on, expiryFormatted)} • $daysLeftLabel",
                            style = TextStyle(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FigmaCrimsonText,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1
                        )
                    }
                }
                if (bundle.stockRemaining > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.subtleSurface)
                            .border(0.8.dp, palette.cardBorder, RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${bundle.stockRemaining} left",
                            style = TextStyle(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Combo Products Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bundle.items) { item ->
                    Box(
                        modifier = Modifier
                            .width(126.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.subtleSurface)
                            .border(0.8.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            .padding(7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = item.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                                    contentDescription = item.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.quantity}x",
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.crimsonLink,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                                Text(
                                    text = item.name,
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary,
                                        lineHeight = 11.sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$currency %.2f".format(item.retailPrice * item.quantity),
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        color = palette.textMuted,
                                        textDecoration = TextDecoration.LineThrough,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(9.dp))

            // Footer: Slashed Original Total Price + Offer Price + Save Badge + CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$currency %.2f".format(bundle.bundleOfferPrice),
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.crimsonLink,
                                fontFeatureSettings = "tnum",
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "$currency %.2f".format(bundle.originalTotalPrice),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textMuted,
                                textDecoration = TextDecoration.LineThrough,
                                fontFeatureSettings = "tnum",
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.you_save_amount, "$currency %.2f".format(savings)) +
                            " • " + stringResource(R.string.bundle_items_count, bundle.items.sumOf { it.quantity }),
                        style = TextStyle(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = FigmaDiscountGreen,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .height(35.dp)
                        .shadow(5.dp, RoundedCornerShape(50), ambientColor = Color(0x40ED0032), spotColor = Color(0x40ED0032))
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        .clickable { onAddCombo() }
                        .padding(horizontal = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.add_combo),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearanceBundleDetailSheet(
    bundle: ClearanceBundle,
    store: RetailerEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onStoreClick: () -> Unit,
    onAddBundle: () -> Unit
) {
    val palette = LocalAppPalette.current
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val expiryText = if (bundle.expiryDateMillis > 0L) dateFmt.format(Date(bundle.expiryDateMillis)) else "Limited time"
    val discountPct = bundle.discountPercentage()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceGradStart,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = store?.name ?: "UAE Supermarket",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.crimsonLink,
                    modifier = Modifier.clickable { onStoreClick() }
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "-$discountPct% OFF",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Text(
                text = bundle.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary
            )

            Text(
                text = "${stringResource(R.string.expires_on, expiryText)}${if (bundle.branchName.isNotBlank()) " • ${bundle.branchName}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.crimsonLink
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.subtleSurface)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bundle.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = item.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.quantity}x ${item.name}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Retail: $currency %.2f each".format(item.retailPrice),
                                fontSize = 10.5.sp,
                                color = palette.textSecondary
                            )
                        }
                        Text(
                            text = "$currency %.2f".format(item.retailPrice * item.quantity),
                            fontSize = 12.sp,
                            color = palette.textMuted,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.retail_total_label),
                        fontSize = 11.sp,
                        color = palette.textMuted
                    )
                    Text(
                        text = "$currency %.2f".format(bundle.originalTotalPrice),
                        fontSize = 15.sp,
                        color = palette.textMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.combo_offer_label),
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$currency %.2f".format(bundle.bundleOfferPrice),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.crimsonLink
                    )
                }
            }

            Button(
                onClick = onAddBundle,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add_combo), fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
