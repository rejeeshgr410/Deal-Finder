package com.example.ui.flyers

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import coil.compose.AsyncImage
import com.example.data.FlyerEntity
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.ui.components.BellsIllustration
import com.example.ui.home.ProductCard
import com.example.ui.home.SectionHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyersScreen(
    viewModel: com.example.ui.DealFinderViewModel,
    onNavigateToFlyer: (String) -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val flyers by viewModel.flyers.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    var selectedCategory by remember { mutableStateOf("All Deals") }

    val filteredProducts = if (selectedCategory == "All Deals") {
        products
    } else {
        products.filter {
            it.category.contains(selectedCategory, ignoreCase = true) ||
                it.name.contains(selectedCategory, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                DealsHeader(
                    userLocation = selectedEmirate,
                    onNavigateToLocation = onNavigateToLocation,
                    onNavigateToNotifications = onNavigateToNotifications
                )
            }
            item {
                DealsCategories(selectedCategory = selectedCategory, onSelect = { selectedCategory = it })
            }

            item {
                SectionHeader(title = "Top Deals Today", action = "View all", onAction = onNavigateToSearch)
                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (products.isEmpty()) "No deals available" else "No deals in $selectedCategory",
                            color = Slate400
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredProducts.take(6)) { product ->
                            ProductCard(product, viewModel.getStore(product.retailerId), onClick = { onNavigateToProduct(product.id) })
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Active Flyers", action = "Search", onAction = onNavigateToSearch)
                if (flyers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active flyers in $selectedEmirate", color = Slate400)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(flyers.take(10), key = { it.id }) { flyer ->
                            FlyerSummaryCard(
                                flyer = flyer,
                                store = viewModel.getStore(flyer.retailerId),
                                onClick = { onNavigateToFlyer(flyer.id) }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Deals Near You",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onNavigateToLocation)
                    ) {
                        Text(
                            "Change area",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF6D28D9),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF6D28D9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val dealsToShow = filteredProducts.take(4)
                    if (dealsToShow.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No nearby deals found for $selectedEmirate", color = Slate400)
                        }
                    } else {
                        dealsToShow.forEachIndexed { index, product ->
                            val (_, distanceKm) = viewModel.getNearestBranchAndDistance(product.retailerId)
                            val distanceLabel = when {
                                distanceKm == null -> "Distance unavailable"
                                distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m away"
                                else -> "%.1f km away".format(distanceKm)
                            }
                            DealListItem(
                                product = product,
                                store = viewModel.getStore(product.retailerId),
                                distanceLabel = distanceLabel,
                                itemIndex = index,
                                onClick = { onNavigateToProduct(product.id) }
                            )
                        }
                    }
                }
            }

            item { NeverMissDealBanner(onManageAlerts = onNavigateToNotificationSettings) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DealsHeader(
    userLocation: String,
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Deals",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Explore the best offers near you",
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
}

data class DealCategory(val name: String, val icon: ImageVector, val iconColor: Color)

@Composable
fun DealsCategories(selectedCategory: String, onSelect: (String) -> Unit) {
    val cats = listOf(
        DealCategory("All Deals", Icons.Outlined.LocalOffer, Color(0xFFB45309)),
        DealCategory("Supermarkets", Icons.Outlined.ShoppingCart, Color(0xFF059669)),
        DealCategory("Electronics", Icons.Outlined.Tv, Color(0xFF7C3AED)),
        DealCategory("Pharmacy", Icons.Outlined.LocalHospital, Color(0xFFE11D48)),
        DealCategory("Home & Kitchen", Icons.Outlined.Home, Color(0xFFEA580C)),
        DealCategory("Beauty", Icons.Outlined.Spa, Color(0xFF0284C7))
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cats) { cat ->
            val isSelected = cat.name == selectedCategory
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(78.dp)
                    .clickable { onSelect(cat.name) }
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) MockupYellowPill else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFCD34D) else Color(0xFFE2E8F0)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        cat.icon,
                        contentDescription = cat.name,
                        modifier = Modifier.padding(14.dp),
                        tint = if (isSelected) Slate900 else cat.iconColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    cat.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Slate900 else Slate600,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun FlyerSummaryCard(
    flyer: FlyerEntity,
    store: RetailerEntity?,
    onClick: () -> Unit
) {
    val expiryLabel = if (flyer.validTo > 0L) {
        "Until " + SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(flyer.validTo))
    } else {
        "Active now"
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        onClick = onClick,
        modifier = Modifier.width(220.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = store?.name ?: "Verified Store",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6D28D9),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = flyer.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = expiryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealListItem(
    product: ProductEntity,
    store: RetailerEntity? = null,
    distanceLabel: String = "Distance unavailable",
    itemIndex: Int = 0,
    onClick: () -> Unit = {}
) {
    val arrowColors = listOf(
        Pair(Color(0xFFDCFCE7), Color(0xFF15803D)),
        Pair(Color(0xFFFFE4E6), Color(0xFFE11D48)),
        Pair(Color(0xFFE0F2FE), Color(0xFF0284C7))
    )
    val (arrowBg, arrowTint) = arrowColors[itemIndex % arrowColors.size]

    val discountPercent = if (product.oldPrice != null && product.oldPrice > product.currentPrice && product.oldPrice > 0.0) {
        (((product.oldPrice - product.currentPrice) / product.oldPrice) * 100).toInt().takeIf { it > 0 }
    } else null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(68.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AsyncImage(
                        model = product.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                        error = coil.compose.rememberAsyncImagePainter("file:///android_asset/products/default_deal.jpg"),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
                if (discountPercent != null) {
                    Surface(
                        color = arrowBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                    ) {
                        Text(
                            "-$discountPercent%",
                            color = arrowTint,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = product.brand.ifBlank { product.category }
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AED %.2f".format(product.currentPrice),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900
                    )
                    if (product.oldPrice != null && product.oldPrice > product.currentPrice) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "AED %.2f".format(product.oldPrice),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Slate400,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    store?.name ?: "Verified Partner",
                    style = MaterialTheme.typography.labelSmall,
                    color = arrowTint,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    distanceLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = arrowBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("↗", color = arrowTint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NeverMissDealBanner(onManageAlerts: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF3E8FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BellsIllustration(size = 56.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Never Miss a Deal!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Turn on alerts and get notified about the best offers & price drops.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onManageAlerts,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Manage Alerts", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
