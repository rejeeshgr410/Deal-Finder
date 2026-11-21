package com.example.ui.flyers

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.FlyerEntity
import com.example.data.LocationManager
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.ui.DealFinderViewModel
import com.example.ui.components.BellsIllustration
import com.example.ui.home.ProductCard
import com.example.ui.home.SectionHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FlyersScreen(
    viewModel: DealFinderViewModel,
    onBack: () -> Unit = {},
    onNavigateToFlyer: (String) -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onAddToList: (ProductEntity) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current

    val products by viewModel.products.collectAsState()
    val flyers by viewModel.flyers.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()

    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }

    var selectedCategory by remember { mutableStateOf("All Deals") }
    var showAllFlyersGrid by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "All Deals") products
        else products.filter {
            it.category.contains(selectedCategory, ignoreCase = true) ||
                it.name.contains(selectedCategory, ignoreCase = true)
        }
    }

    val filteredFlyers = remember(flyers, products, selectedCategory) {
        if (selectedCategory == "All Deals") flyers
        else flyers.filter { flyer ->
            flyer.title.contains(selectedCategory, ignoreCase = true) ||
                flyer.searchKeywords.any { it.contains(selectedCategory, ignoreCase = true) } ||
                products.any { it.flyerId == flyer.id && it.category.contains(selectedCategory, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DealsHeader(
                userLocation = localizedEmirate,
                hasUnreadNotifications = unreadNotifCount > 0,
                onBack = onBack,
                onNavigateToLocation = onNavigateToLocation,
                onNavigateToNotifications = onNavigateToNotifications
            )
        }

        item {
            DealsCategories(selectedCategory = selectedCategory, onSelect = { selectedCategory = it })
        }

        if (syncState.isLoading && products.isEmpty() && flyers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FigmaCrimsonLink)
                }
            }
        } else {
            item {
                SectionHeader(
                    title = stringResource(R.string.featured_flyers),
                    action = if (showAllFlyersGrid) "Compact" else stringResource(R.string.view_all),
                    onAction = { showAllFlyersGrid = !showAllFlyersGrid }
                )
                if (filteredFlyers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_catalog_data), color = palette.textMuted)
                    }
                } else if (showAllFlyersGrid) {
                    PromotionalFlyersGrid(
                        flyers = filteredFlyers,
                        stores = stores,
                        onFlyerClick = { onNavigateToFlyer(it.id) }
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFlyers, key = { it.id }) { flyer ->
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
                SectionHeader(title = stringResource(R.string.top_deals_today), action = stringResource(R.string.view_all), onAction = onNavigateToSearch)
                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_catalog_data), color = palette.textMuted)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts.take(10), key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                store = viewModel.getStore(product.retailerId),
                                onClick = { onNavigateToProduct(product.id) },
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
                        text = stringResource(R.string.nearby_stores),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onNavigateToLocation)
                    ) {
                        Text(
                            text = localizedEmirate,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.crimsonLink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.crimsonLink)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredProducts.take(8).forEachIndexed { index, product ->
                        val (_, distanceKm) = viewModel.getNearestBranchAndDistance(product.retailerId)
                        val distanceLabel = when {
                            distanceKm == null -> localizedEmirate
                            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
                            else -> "%.1f km".format(distanceKm)
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

        if (syncState.errorMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(syncState.errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.refreshCatalog() }) {
                            Text(stringResource(R.string.retry), color = FigmaCrimsonLink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { NeverMissDealBanner(onManageAlerts = onNavigateToNotificationSettings) }
    }
}

@Composable
private fun DealsHeader(
    userLocation: String,
    hasUnreadNotifications: Boolean,
    onBack: () -> Unit,
    onNavigateToLocation: () -> Unit,
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
                    .size(38.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.textPrimary, modifier = Modifier.size(21.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.nav_deals),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary, platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = TextStyle(fontSize = 10.5.sp, color = palette.textSecondary, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToLocation() }
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(userLocation, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary, maxLines = 1)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.textPrimary)
                }
            }

            Box(modifier = Modifier.size(35.dp)) {
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

private data class DealCategory(val name: String, val icon: ImageVector)

@Composable
fun DealsCategories(selectedCategory: String, onSelect: (String) -> Unit) {
    val palette = LocalAppPalette.current
    val cats = listOf(
        DealCategory("All Deals", Icons.Outlined.LocalOffer),
        DealCategory("Fresh Food", Icons.Outlined.ShoppingCart),
        DealCategory("Groceries", Icons.Outlined.ShoppingBag),
        DealCategory("Electronics", Icons.Outlined.Tv),
        DealCategory("Home & Kitchen", Icons.Outlined.Home),
        DealCategory("Beauty", Icons.Outlined.Spa)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cats) { cat ->
            val isSelected = selectedCategory == cat.name
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .shadow(if (isSelected) 6.dp else 3.dp, RoundedCornerShape(50), ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        } else {
                            Modifier
                                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(50))
                        }
                    )
                    .clickable { onSelect(cat.name) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(cat.icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else palette.textPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (cat.name == "All Deals") stringResource(R.string.tab_all) else cat.name,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun FlyerSummaryCard(flyer: FlyerEntity, store: RetailerEntity?, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val validText = if (flyer.validTo > 0) stringResource(R.string.valid_till, dateFormat.format(Date(flyer.validTo))) else "${flyer.pages} pgs"
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .width(240.dp)
            .shadow(6.dp, cardShape, ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, cardShape)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = flyer.imageUrl.ifBlank { store?.logoUrl ?: "file:///android_asset/flyers/default_flyer.jpg" },
                    error = coil.compose.rememberAsyncImagePainter("file:///android_asset/flyers/default_flyer.jpg"),
                    contentDescription = flyer.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = store?.name ?: "Verified Store",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.crimsonLink,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = flyer.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(validText, style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
            }
        }
    }
}

@Composable
fun DealListItem(
    product: ProductEntity,
    store: RetailerEntity?,
    distanceLabel: String,
    itemIndex: Int = 0,
    onClick: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val currency = stringResource(R.string.currency_aed)
    val oldPrice = product.oldPrice
    val discountPercent = if (oldPrice != null && oldPrice > product.currentPrice && oldPrice > 0.0) {
        (((oldPrice - product.currentPrice) / oldPrice) * 100).toInt().takeIf { it > 0 }
    } else null
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, cardShape, ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, cardShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(58.dp)
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
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.textPrimary
                )
                Text(
                    text = "${store?.name ?: "Verified Store"} • $distanceLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currency %.2f".format(product.currentPrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.crimsonLink
                    )
                    if (oldPrice != null && oldPrice > product.currentPrice) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$currency %.2f".format(oldPrice),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    if (discountPercent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.crimsonPillBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("-$discountPercent%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = palette.crimsonLink)
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = palette.crimsonLink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun NeverMissDealBanner(onManageAlerts: () -> Unit = {}) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(6.dp, shape, ambientColor = Color(0x1624203C), spotColor = Color(0x1624203C))
            .clip(shape)
            .background(
                if (palette.isDark) {
                    Brush.horizontalGradient(listOf(Color(0xFF281722), Color(0xFF331B29)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFFEF4F8), Color(0xFFFDE6EF), Color(0xFFFBD5E3)))
                }
            )
            .border(1.dp, palette.cardBorder, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BellsIllustration(size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.get_notified),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = stringResource(R.string.get_notified_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        .clickable { onManageAlerts() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enable_notifications),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
