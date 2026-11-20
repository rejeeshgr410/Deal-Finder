package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.example.ui.components.BasketIllustration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import com.example.data.FlyerEntity
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: com.example.ui.DealFinderViewModel,
    userName: String,
    onNavigateToCompare: () -> Unit,
    onNavigateToFlyers: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFlyer: (String) -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onAddToList: (com.example.data.ProductEntity) -> Unit,
    onNavigateToLoyaltyCards: () -> Unit = {}
) {
    val flyers by viewModel.flyers.collectAsState()
    val products by viewModel.products.collectAsState()
    val promotions by viewModel.promotions.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val isNetworkAvailable by androidx.compose.runtime.produceState(initialValue = true, key1 = context) {
        val cm = try {
            context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        } catch (_: Exception) {
            null
        }
        fun checkOnline(): Boolean {
            return try {
                val activeNetwork = cm?.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
            } catch (_: Exception) {
                true
            }
        }
        value = checkOnline()
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                value = true
                viewModel.refreshCatalog()
            }
            override fun onLost(network: android.net.Network) {
                value = checkOnline()
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            // Fall back to initial value if network callback cannot be registered
        }
        awaitDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    val lastUpdatedLabel = androidx.compose.runtime.remember(syncState.lastServerSyncAtMs) {
        syncState.lastServerSyncAtMs?.let { ts ->
            val fmt = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
            "Last confirmed ${fmt.format(java.util.Date(ts))}"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { HomeHeader(userName = userName, locationName = selectedEmirate, onNavigateToCompare = onNavigateToCompare, onNavigateToLocation = onNavigateToLocation, onNavigateToNotifications = onNavigateToNotifications) }

            if (!isNetworkAvailable || syncState.isCachePotentiallyStale() || syncState.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline or cached data warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!isNetworkAvailable) "Offline Mode — Cached Data" else "Cached Data — May Be Outdated",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = buildString {
                                        append("Prices shown are from device cache, not live retailer feeds. ")
                                        if (lastUpdatedLabel != null) append(lastUpdatedLabel) else append("Reconnect to revalidate.")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            if (isNetworkAvailable) {
                                TextButton(onClick = { viewModel.refreshCatalog() }) {
                                    Text("Refresh", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (lastUpdatedLabel != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Source: Retailer flyers & authorized feeds • $lastUpdatedLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.refreshCatalog() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Refresh", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item { 
                QuickActions(
                    onNavigateToCompare = onNavigateToCompare,
                    onNavigateToFlyers = onNavigateToFlyers,
                    onNavigateToList = onNavigateToList,
                    onNavigateToCoupons = onNavigateToCoupons,
                    onNavigateToLoyaltyCards = onNavigateToLoyaltyCards
                ) 
            }
            
            item {
                SectionHeader(title = "Active Flyers", subtitle = selectedEmirate, action = "View all", onAction = onNavigateToFlyers)
                if (!isLoaded) {
                    Box(modifier = Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator()
                    }
                } else if (flyers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No active flyers in $selectedEmirate right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onNavigateToLocation) {
                                Text("Switch Emirate")
                            }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(flyers) { flyer -> FlyerCard(flyer, viewModel.getStore(flyer.retailerId), onClick = { onNavigateToFlyer(flyer.id) }) }
                    }
                }
            }
            
            item {
                SectionHeader(title = "Top Deals Today", subtitle = selectedEmirate, action = "View all", onAction = onNavigateToCompare)
                if (!isLoaded) {
                     Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator()
                     }
                } else if (products.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (!isNetworkAvailable) "No cached deals available offline for $selectedEmirate." else "No active published deals in $selectedEmirate yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.refreshCatalog() }) {
                                    Text("Retry")
                                }
                                OutlinedButton(onClick = onNavigateToLocation) {
                                    Text("Change Emirate")
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(products) { product -> ProductCard(product, viewModel.getStore(product.retailerId), onClick = { onNavigateToProduct(product.id) }, onAddToList = { onAddToList(product) }) }
                    }
                }
            }

            if (promotions.isNotEmpty()) {
                item {
                    SectionHeader(title = "Active Promotions", action = "", onAction = {})
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(promotions) { promo ->
                            PromotionCard(promo, viewModel.getStore(promo.retailerId))
                        }
                    }
                }
            }

            item {
                SmartSavingsBanner(onAction = onNavigateToCompare)
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionCard(promotion: com.example.data.PromotionEntity, store: com.example.data.RetailerEntity?) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.width(280.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                store?.name ?: "Verified Partner", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                promotion.title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                promotion.description, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            val discountBadgeText = promotion.discountInfo.ifBlank {
                if (promotion.discount > 0.0) {
                    val pct = if (promotion.discount % 1.0 == 0.0) promotion.discount.toInt().toString() else promotion.discount.toString()
                    "$pct% OFF"
                } else {
                    "SPECIAL OFFER"
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = discountBadgeText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HomeHeader(userName: String, locationName: String, onNavigateToCompare: () -> Unit, onNavigateToLocation: () -> Unit, onNavigateToNotifications: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hi, $userName \uD83D\uDC4B",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Find the best deals around you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    onClick = onNavigateToLocation
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                            locationName, 
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
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(40.dp),
                    onClick = onNavigateToNotifications
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
        
        Spacer(modifier = Modifier.height(18.dp))
        
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            onClick = onNavigateToCompare
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search products, brands or stores",
                    color = Slate400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = MockupYellowPill,
                    modifier = Modifier.size(42.dp)
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
}

data class QuickAction(val name: String, val icon: ImageVector, val color: Color, val iconColor: Color, val onClick: () -> Unit)

@Composable
fun QuickActions(
    onNavigateToCompare: () -> Unit,
    onNavigateToFlyers: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    onNavigateToLoyaltyCards: () -> Unit = {}
) {
    val items = listOf(
        QuickAction("Nearby", Icons.Outlined.LocationOn, MockupMintBg, MockupMintGreen, onNavigateToFlyers),
        QuickAction("Flyers", Icons.Outlined.LocalOffer, MockupPeachBg, MockupOrange, onNavigateToFlyers),
        QuickAction("Compare", Icons.AutoMirrored.Outlined.CompareArrows, MockupPurpleBg, MockupPurpleText, onNavigateToCompare),
        QuickAction("Smart List", Icons.AutoMirrored.Outlined.List, MockupYellowContainer, MockupYellowDark, onNavigateToList),
        QuickAction("Coupons", Icons.Outlined.ConfirmationNumber, MockupPinkBg, MockupPink, onNavigateToCoupons)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(66.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = item.color,
                    modifier = Modifier.size(54.dp),
                    onClick = item.onClick
                ) {
                    Icon(
                        item.icon, 
                        contentDescription = item.name, 
                        modifier = Modifier.padding(14.dp), 
                        tint = item.iconColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    item.name, 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp), 
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        subtitle, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (action.isNotEmpty()) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    action, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyerCard(flyer: FlyerEntity, store: RetailerEntity? = null, onClick: () -> Unit = {}) {
    // Dynamic pastel background matching mockup 4
    val storeNameLower = (store?.name ?: flyer.title).lowercase()
    val (bgColor, badgeColor) = when {
        storeNameLower.contains("lulu") -> Pair(Color(0xFFE8F8F0), Color(0xFF15803D))
        storeNameLower.contains("carrefour") -> Pair(Color(0xFFEDF4FC), Color(0xFF0284C7))
        storeNameLower.contains("nesto") -> Pair(Color(0xFFFFF4E8), Color(0xFFEA580C))
        else -> Pair(Color(0xFFFDF0F4), Color(0xFFE11D48))
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(164.dp).height(236.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().height(98.dp)
            ) {
                AsyncImage(
                    model = flyer.imageUrl.ifBlank { store?.logoUrl ?: "file:///android_asset/logos/default_logo.png" },
                    error = coil.compose.rememberAsyncImagePainter("file:///android_asset/logos/default_logo.png"),
                    contentDescription = flyer.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (store != null) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = flyer.title.ifBlank { store?.name ?: "Offer Flyer" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(2.dp))
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
            val formattedDate = if (flyer.validTo > 0) "Valid till ${sdf.format(java.util.Date(flyer.validTo))}" else "Valid today"
            Text(
                formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
            Spacer(modifier = Modifier.weight(1f))
            val daysLeft = if (flyer.validTo > 0) java.util.concurrent.TimeUnit.MILLISECONDS.toDays(flyer.validTo - System.currentTimeMillis()).toInt() else null
            
            val badgeText = when {
                daysLeft != null && daysLeft > 1 -> "$daysLeft DAYS LEFT"
                daysLeft != null && daysLeft == 1 -> "1 DAY LEFT"
                daysLeft != null && daysLeft == 0 -> "ENDS TODAY"
                else -> "CHECK FLYER DATES"
            }
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 0.5.dp
            ) {
                Text(
                    text = badgeText,
                    color = Color(0xFFE11D48),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(product: ProductEntity, store: RetailerEntity? = null, onClick: () -> Unit = {}, onAddToList: (() -> Unit)? = null) {
    val oldPrice = product.oldPrice
    val discountPercent = if (oldPrice != null && oldPrice > product.currentPrice && oldPrice > 0.0) {
        (((oldPrice - product.currentPrice) / oldPrice) * 100).toInt().takeIf { it > 0 }
    } else null
    val unitPriceText = com.example.domain.basket.OfferEligibility.computeUnitPriceText(product)
    val subtitleMeta = buildString {
        if (product.brand.isNotBlank()) append(product.brand)
        val unitLabel = if (product.unitSize > 0 && product.unitType.isNotBlank()) {
            val sizeStr = if (product.unitSize % 1.0 == 0.0) product.unitSize.toInt().toString() else product.unitSize.toString()
            if (product.packCount > 1) "${product.packCount}x$sizeStr ${product.unitType}" else "$sizeStr ${product.unitType}"
        } else ""
        if (unitLabel.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append(unitLabel)
        }
        if (isEmpty() && product.category.isNotBlank()) append(product.category)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(168.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    AsyncImage(
                        model = product.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                        error = coil.compose.rememberAsyncImagePainter("file:///android_asset/products/default_deal.jpg"),
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }
                if (discountPercent != null) {
                    Surface(
                        color = Color(0xFF059669),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Text(
                            "-$discountPercent%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitleMeta.isNotBlank()) {
                Text(
                    text = subtitleMeta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AED ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.2f".format(product.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (oldPrice != null && oldPrice > product.currentPrice) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AED %.2f".format(oldPrice),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            if (unitPriceText != null) {
                Text(
                    text = unitPriceText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = store?.name ?: "Verified Retailer",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.sourceLabel.ifBlank { "Retailer flyer" },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (onAddToList != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onAddToList,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MockupYellowContainer,
                        contentColor = MockupYellowDark
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to List", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SmartSavingsBanner(onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF3E8FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
    ) {
        Row(
            modifier = Modifier.padding(18.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Smart Savings", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Compare prices, build your list and save more every day.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Create Smart List", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("→", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            BasketIllustration(size = 80.dp)
        }
    }
}
