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
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.CompareArrows
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
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val isNetworkAvailable = androidx.compose.runtime.remember(context) {
        try {
            val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        } catch (e: Exception) {
            true // Fallback to assuming online to avoid false warnings
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
            
            if (!isNetworkAvailable) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudOff, 
                                contentDescription = "Offline Cache Identifier", 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Offline Mode Active", 
                                    style = MaterialTheme.typography.titleSmall, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Using local device cache. You can still access saved shopping lists and browsing features offline.", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
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
                SectionHeader(title = "Nearby Flyers", subtitle = "Within 10 km", action = "View all", onAction = onNavigateToFlyers)
                if (!isLoaded) {
                    Box(modifier = Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator()
                    }
                } else if (flyers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) { Text("No flyers available") }
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
                SectionHeader(title = "Top Deals Today", action = "View all", onAction = onNavigateToCompare)
                if (!isLoaded) {
                     Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator()
                     }
                } else if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No deals available offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { /* Force refresh is handled by Firebase automatically, could show toast */ }) {
                                Text("Retry")
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
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${promotion.discount} OFF",
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
        QuickAction("Nearby Deals", Icons.Outlined.LocationOn, MockupMintBg, MockupMintGreen, onNavigateToFlyers),
        QuickAction("All Flyers", Icons.Outlined.LocalOffer, MockupPeachBg, MockupOrange, onNavigateToFlyers),
        QuickAction("Price Compare", Icons.Outlined.CompareArrows, MockupPurpleBg, MockupPurpleText, onNavigateToCompare),
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
                modifier = Modifier.width(64.dp)
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
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), 
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
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
        modifier = Modifier.width(164.dp).height(218.dp),
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
                modifier = Modifier.fillMaxWidth().height(72.dp)
            ) {
                AsyncImage(
                    model = store?.logoUrl ?: "file:///android_asset/logos/default_logo.png",
                    error = coil.compose.rememberAsyncImagePainter("file:///android_asset/logos/default_logo.png"),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                store?.name ?: flyer.title, 
                style = MaterialTheme.typography.titleSmall, 
                fontWeight = FontWeight.Bold, 
                maxLines = 1,
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
                else -> "2 DAYS LEFT"
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
    val discountPercent = if (product.oldPrice != null && product.oldPrice > product.currentPrice) {
        (((product.oldPrice - product.currentPrice) / product.oldPrice) * 100).toInt()
    } else 20

    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
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
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.name, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold, 
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Slate900
            )
            Text(
                text = product.brand.ifBlank { "Philippines" }, 
                style = MaterialTheme.typography.labelSmall, 
                color = Slate400,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AED ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.2f".format(product.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                if (product.oldPrice != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AED %.2f".format(product.oldPrice),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate400,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = store?.name ?: "Lulu Hypermarket", 
                style = MaterialTheme.typography.labelSmall, 
                color = Slate500,
                maxLines = 1
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
