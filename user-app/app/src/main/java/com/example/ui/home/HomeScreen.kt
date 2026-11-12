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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Find the best deals around you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            locationName, 
                            style = MaterialTheme.typography.labelMedium, 
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BadgedBox(
                    badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp),
                        onClick = onNavigateToNotifications
                    ) {
                        Icon(
                            Icons.Outlined.Notifications, 
                            contentDescription = "Notifications", 
                            modifier = Modifier.padding(10.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = onNavigateToCompare
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search products, brands, or hypermarkets…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList, 
                        contentDescription = "Filter", 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.padding(8.dp)
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
        QuickAction("Loyalty Cards", Icons.Outlined.Payment, CatBlue, CatBlueIcon, onNavigateToLoyaltyCards),
        QuickAction("Nearby Deals", Icons.Outlined.LocationOn, CatGreen, CatGreenIcon, onNavigateToFlyers),
        QuickAction("All Flyers", Icons.Outlined.LocalOffer, CatRed, CatRedIcon, onNavigateToFlyers),
        QuickAction("Price Compare", Icons.Outlined.CompareArrows, CatPurple, CatPurpleIcon, onNavigateToCompare),
        QuickAction("Smart List", Icons.AutoMirrored.Outlined.List, CatYellow, CatYellowIcon, onNavigateToList),
        QuickAction("Coupons", Icons.Outlined.ConfirmationNumber, CatPink, CatPinkIcon, onNavigateToCoupons)
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = item.color,
                    border = BorderStroke(1.dp, item.iconColor.copy(alpha = 0.15f)),
                    modifier = Modifier.size(56.dp),
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
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
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
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(210.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(68.dp)
            ) {
                AsyncImage(
                    model = store?.logoUrl,
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
            val formattedDate = if (flyer.validTo > 0) "Until ${sdf.format(java.util.Date(flyer.validTo))}" else "Active"
            Text(
                formattedDate, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            val daysLeft = if (flyer.validTo > 0) java.util.concurrent.TimeUnit.MILLISECONDS.toDays(flyer.validTo - System.currentTimeMillis()).toInt() else null
            val isUrgent = daysLeft != null && daysLeft in 0..2
            Surface(
                color = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                val badgeText = when {
                    daysLeft != null && daysLeft > 1 -> "$daysLeft DAYS LEFT"
                    daysLeft != null && daysLeft == 1 -> "ENDS TOMORROW"
                    daysLeft != null && daysLeft == 0 -> "ENDS TODAY"
                    else -> "VERIFIED FLYER"
                }
                Text(
                    text = badgeText,
                    color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
    } else null

    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(6.dp)
                    )
                }
                if (discountPercent != null && discountPercent > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
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
                fontWeight = FontWeight.SemiBold, 
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = product.brand, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AED ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.2f".format(product.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (product.oldPrice != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "%.2f".format(product.oldPrice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = store?.name ?: "", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            
            if (onAddToList != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onAddToList,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to List", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SmartSavingsBanner(onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        "UAE PRICE OPTIMIZER",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Multi-Store Grocery Comparison", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Compare items across Carrefour, Lulu, Nesto & Viva to find the cheapest supermarket.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Compare Prices", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                shape = CircleShape, 
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(64.dp)
            ) {
                 Icon(
                     Icons.Default.ShoppingBasket, 
                     contentDescription = null, 
                     modifier = Modifier.fillMaxSize().padding(16.dp), 
                     tint = MaterialTheme.colorScheme.primary
                 )
            }
        }
    }
}
