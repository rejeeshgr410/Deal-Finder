package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CatalogQueryPolicy
import com.example.data.FlyerEntity
import com.example.data.LocationManager
import com.example.data.ProductEntity
import com.example.data.PromotionEntity
import com.example.data.RetailerEntity
import com.example.ui.DealFinderViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: DealFinderViewModel,
    userName: String?,
    onNavigateToCompare: (Boolean) -> Unit,
    onSearchBrand: (String) -> Unit = { onNavigateToCompare(false) },
    onNavigateToFlyers: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFlyer: (String) -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToRetailer: (String) -> Unit = {},
    onNavigateToLoyaltyCards: () -> Unit = {},
    onAddToList: (ProductEntity) -> Unit
) {
    val palette = LocalAppPalette.current
    val flyers by viewModel.flyers.collectAsState()
    val products by viewModel.products.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val promotions by viewModel.promotions.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    val hasGps by viewModel.hasActiveGpsLocation.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()

    val context = LocalContext.current
    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }

    val isNetworkAvailable by produceState(initialValue = true, key1 = context) {
        val cm = try {
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        } catch (_: Exception) {
            null
        }
        fun checkOnline(): Boolean {
            return try {
                val activeNetwork = cm?.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } catch (_: Exception) {
                true
            }
        }
        value = checkOnline()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
                viewModel.refreshCatalog()
            }

            override fun onLost(network: Network) {
                value = checkOnline()
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
        awaitDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    val lastUpdatedLabel = remember(syncState.lastServerSyncAtMs) {
        syncState.lastServerSyncAtMs?.let { ts ->
            val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            fmt.format(Date(ts))
        }
    }

    val storesById = remember(stores) { stores.associateBy { it.id } }

    // Showcase featured Nestlé demo products first, then highest-discount / flagship deal per product group
    val topDeals = remember(products) {
        val sorted = products.sortedWith(
            compareByDescending<ProductEntity> { CatalogQueryPolicy.isNestleShowcaseProduct(it) }
                .thenByDescending {
                    val oldP = it.oldPrice
                    if (oldP != null && oldP > it.currentPrice && oldP > 0.0) {
                        (oldP - it.currentPrice) / oldP
                    } else 0.0
                }
                .thenBy { it.currentPrice }
        )
        sorted.distinctBy { it.masterProductId.ifBlank { it.normalizedName.ifBlank { it.id } } }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            HomeHeader(
                userName = userName,
                locationName = localizedEmirate,
                hasUnreadNotifications = unreadNotifCount > 0,
                onNavigateToCompare = { openFilters -> onNavigateToCompare(openFilters) },
                onNavigateToLocation = onNavigateToLocation,
                onNavigateToLoyaltyCards = onNavigateToLoyaltyCards,
                onNavigateToNotifications = onNavigateToNotifications
            )
        }

        item {
            QuickActionRow(
                onScrollToTopDeals = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(4)
                    }
                },
                onNavigateToFlyers = onNavigateToFlyers,
                onNavigateToCompare = { onNavigateToCompare(false) },
                onNavigateToSmartList = onNavigateToList,
                onNavigateToCoupons = onNavigateToCoupons
            )
        }

        if (syncState.isLoading && flyers.isEmpty() && products.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = FigmaCrimsonLink,
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }
        } else {
            item {
                NearbyFlyersCardSection(
                    flyers = flyers,
                    storesById = storesById,
                    locationLabel = if (hasGps) stringResource(R.string.within_10km) else localizedEmirate,
                    onLocationFilterClick = onNavigateToLocation,
                    onViewAll = onNavigateToFlyers,
                    onFlyerCardClick = { flyerId -> onNavigateToFlyer(flyerId) }
                )
            }

            item {
                TrendingProductsSection(
                    onViewAll = { onSearchBrand("Nestlé") },
                    onBannerClick = { url ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                TopDealsTodaySection(
                    deals = topDeals,
                    getStore = { id -> storesById[id] ?: viewModel.getStore(id) },
                    onViewAll = { onNavigateToCompare(false) },
                    onProductClick = onNavigateToProduct,
                    onAddToList = { product ->
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

        item {
            SmartSavingsCarouselSection(
                onCreateSmartList = onNavigateToList,
                onOptimizeBasket = onNavigateToList,
                onOpenWallet = onNavigateToLoyaltyCards
            )
        }

        if (promotions.isNotEmpty()) {
            item {
                UpcomingPromotionsHeader(
                    onViewAll = onNavigateToFlyers
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(promotions, key = { it.id }) { promo ->
                        val store = storesById[promo.retailerId] ?: viewModel.getStore(promo.retailerId)
                        PromotionCard(
                            promotion = promo,
                            store = store,
                            onClick = {
                                if (promo.retailerId.isNotBlank()) {
                                    onNavigateToRetailer(promo.retailerId)
                                } else {
                                    onNavigateToFlyers()
                                }
                            }
                        )
                    }
                }
            }
        }

        if (!isNetworkAvailable || syncState.isCachePotentiallyStale() || syncState.errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    !isNetworkAvailable -> stringResource(R.string.offline_cached_title)
                                    syncState.errorMessage != null -> stringResource(R.string.sync_error_title)
                                    else -> stringResource(R.string.stale_cached_title)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            val detail = syncState.errorMessage
                                ?: lastUpdatedLabel
                                ?: stringResource(R.string.no_catalog_data)
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.refreshCatalog() }) {
                            Text(
                                text = stringResource(R.string.retry),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
    }
}

@Composable
fun HomeHeader(
    userName: String?,
    locationName: String,
    hasUnreadNotifications: Boolean = false,
    onNavigateToCompare: (Boolean) -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToLoyaltyCards: () -> Unit = {},
    onNavigateToNotifications: () -> Unit
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (!userName.isNullOrBlank()) {
                            stringResource(R.string.hi_user, userName) + " "
                        } else {
                            stringResource(R.string.hi_guest) + " "
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Image(
                        painter = painterResource(id = R.drawable.fig_waving_hand),
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = palette.textSecondary,
                    letterSpacing = (-0.25).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(35.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                        .clickable { onNavigateToLocation() }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = stringResource(R.string.location),
                            modifier = Modifier.size(15.dp),
                            tint = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = locationName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = palette.textPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable { onNavigateToLoyaltyCards() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = stringResource(R.string.nav_wallet),
                        modifier = Modifier.size(16.dp),
                        tint = palette.textPrimary
                    )
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
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            modifier = Modifier.size(17.dp),
                            tint = palette.textPrimary
                        )
                    }
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 1.dp)
                                .clip(CircleShape)
                                .background(FigmaCrimsonDot)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(9.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .shadow(7.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp))
                .clickable { onNavigateToCompare(false) }
                .padding(start = 16.dp, end = 5.dp),
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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.search_placeholder),
                    color = palette.textMuted,
                    fontSize = 11.5.sp,
                    letterSpacing = (-0.25).sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(35.dp)
                        .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x40ED0032), spotColor = Color(0x40ED0032))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        .border(0.8.dp, Color(0x66FF5575), RoundedCornerShape(20.dp))
                        .clickable { onNavigateToCompare(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.filter_and_sort),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private enum class QuickShortcutKind {
    NEARBY_DEALS, ALL_FLYERS, PRICE_COMPARE, SMART_LIST, COUPONS
}

private data class FigmaQuickShortcut(
    val label: String,
    val bgColor: Color,
    val kind: QuickShortcutKind,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionRow(
    onScrollToTopDeals: () -> Unit,
    onNavigateToFlyers: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToSmartList: () -> Unit,
    onNavigateToCoupons: () -> Unit
) {
    val palette = LocalAppPalette.current
    val items = listOf(
        FigmaQuickShortcut(stringResource(R.string.action_nearby_deals), QuickActionDealsBg, QuickShortcutKind.NEARBY_DEALS, onScrollToTopDeals),
        FigmaQuickShortcut(stringResource(R.string.action_all_flyers), QuickActionFlyersBg, QuickShortcutKind.ALL_FLYERS, onNavigateToFlyers),
        FigmaQuickShortcut(stringResource(R.string.action_price_compare), QuickActionCompareBg, QuickShortcutKind.PRICE_COMPARE, onNavigateToCompare),
        FigmaQuickShortcut(stringResource(R.string.action_smart_list), QuickActionListBg, QuickShortcutKind.SMART_LIST, onNavigateToSmartList),
        FigmaQuickShortcut(stringResource(R.string.action_coupons), QuickActionCouponsBg, QuickShortcutKind.COUPONS, onNavigateToCoupons)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(6.dp, RoundedCornerShape(15.dp), ambientColor = Color(0x1F28233C), spotColor = Color(0x1F28233C))
                        .clip(RoundedCornerShape(15.dp))
                        .background(item.bgColor)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.56f), Color.Transparent)))
                        .border(0.8.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(15.dp))
                        .clickable { item.onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    QuickShortcutCanvasIcon(kind = item.kind, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    style = TextStyle(
                        fontSize = 10.sp,
                        lineHeight = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.25).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun QuickShortcutCanvasIcon(kind: QuickShortcutKind, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (kind) {
            QuickShortcutKind.NEARBY_DEALS -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.56f)
                    lineTo(w * 0.56f, h * 0.08f)
                    lineTo(w * 0.85f, h * 0.08f)
                    quadraticTo(w * 0.92f, h * 0.08f, w * 0.92f, h * 0.16f)
                    lineTo(w * 0.92f, h * 0.44f)
                    lineTo(w * 0.44f, h * 0.92f)
                    quadraticTo(w * 0.39f, h * 0.96f, w * 0.34f, h * 0.92f)
                    lineTo(w * 0.08f, h * 0.66f)
                    quadraticTo(w * 0.04f, h * 0.61f, w * 0.08f, h * 0.56f)
                    close()
                }
                drawPath(p, color = QuickActionDealsIcon)
                drawCircle(color = QuickActionDealsHole, radius = w * 0.095f, center = Offset(w * 0.70f, h * 0.30f))
            }
            QuickShortcutKind.ALL_FLYERS -> {
                val p = Path().apply {
                    moveTo(w * 0.16f, h * 0.64f)
                    lineTo(w * 0.08f, h * 0.50f)
                    lineTo(w * 0.22f, h * 0.33f)
                    lineTo(w * 0.38f, h * 0.29f)
                    cubicTo(w * 0.54f, h * 0.06f, w * 0.71f, h * 0.06f, w * 0.91f, h * 0.06f)
                    cubicTo(w * 0.95f, h * 0.30f, w * 0.86f, h * 0.50f, w * 0.63f, h * 0.68f)
                    lineTo(w * 0.58f, h * 0.84f)
                    lineTo(w * 0.42f, h * 0.92f)
                    lineTo(w * 0.29f, h * 0.75f)
                    lineTo(w * 0.10f, h * 0.88f)
                    close()
                }
                drawPath(p, color = QuickActionFlyersIcon)
                drawCircle(color = QuickActionFlyersHole, radius = w * 0.095f, center = Offset(w * 0.68f, h * 0.32f))
            }
            QuickShortcutKind.PRICE_COMPARE -> {
                val sw = w * 0.085f
                val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawLine(QuickActionCompareIcon, Offset(w * 0.50f, h * 0.10f), Offset(w * 0.50f, h * 0.86f), sw, StrokeCap.Round)
                drawLine(QuickActionCompareIcon, Offset(w * 0.28f, h * 0.86f), Offset(w * 0.72f, h * 0.86f), sw, StrokeCap.Round)
                drawLine(QuickActionCompareIcon, Offset(w * 0.14f, h * 0.22f), Offset(w * 0.86f, h * 0.22f), sw, StrokeCap.Round)
                val leftPan = Path().apply {
                    moveTo(w * 0.20f, h * 0.24f)
                    lineTo(w * 0.06f, h * 0.56f)
                    lineTo(w * 0.34f, h * 0.56f)
                    close()
                }
                val leftBowl = Path().apply {
                    moveTo(w * 0.06f, h * 0.56f)
                    quadraticTo(w * 0.20f, h * 0.72f, w * 0.34f, h * 0.56f)
                }
                val rightPan = Path().apply {
                    moveTo(w * 0.80f, h * 0.24f)
                    lineTo(w * 0.66f, h * 0.56f)
                    lineTo(w * 0.94f, h * 0.56f)
                    close()
                }
                val rightBowl = Path().apply {
                    moveTo(w * 0.66f, h * 0.56f)
                    quadraticTo(w * 0.80f, h * 0.72f, w * 0.94f, h * 0.56f)
                }
                drawPath(leftPan, color = QuickActionCompareIcon, style = stroke)
                drawPath(leftBowl, color = QuickActionCompareIcon, style = stroke)
                drawPath(rightPan, color = QuickActionCompareIcon, style = stroke)
                drawPath(rightBowl, color = QuickActionCompareIcon, style = stroke)
            }
            QuickShortcutKind.SMART_LIST -> {
                val barH = h * 0.13f
                val r = barH / 2f
                listOf(0.24f, 0.50f, 0.76f).forEach { cy ->
                    drawCircle(color = QuickActionListIcon, radius = barH * 0.55f, center = Offset(w * 0.18f, h * cy))
                    drawRoundRect(
                        color = QuickActionListIcon,
                        topLeft = Offset(w * 0.34f, h * cy - barH * 0.45f),
                        size = Size(w * 0.56f, barH * 0.90f),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
            QuickShortcutKind.COUPONS -> {
                val tPath = Path().apply {
                    moveTo(w * 0.10f, h * 0.16f)
                    lineTo(w * 0.90f, h * 0.16f)
                    lineTo(w * 0.94f, h * 0.36f)
                    cubicTo(w * 0.78f, h * 0.44f, w * 0.78f, h * 0.56f, w * 0.94f, h * 0.64f)
                    lineTo(w * 0.94f, h * 0.84f)
                    lineTo(w * 0.06f, h * 0.84f)
                    lineTo(w * 0.06f, h * 0.64f)
                    cubicTo(w * 0.22f, h * 0.56f, w * 0.22f, h * 0.44f, w * 0.06f, h * 0.36f)
                    lineTo(w * 0.06f, h * 0.16f)
                    close()
                }
                drawPath(tPath, color = QuickActionCouponsIcon)
                val sw = w * 0.075f
                val star = Path().apply {
                    moveTo(w * 0.50f, h * 0.36f)
                    lineTo(w * 0.60f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.64f)
                    lineTo(w * 0.40f, h * 0.50f)
                    close()
                }
                drawPath(star, color = Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val dotR = w * 0.032f
                drawCircle(Color.White, dotR, Offset(w * 0.33f, h * 0.34f))
                drawCircle(Color.White, dotR, Offset(w * 0.67f, h * 0.34f))
                drawCircle(Color.White, dotR, Offset(w * 0.33f, h * 0.66f))
                drawCircle(Color.White, dotR, Offset(w * 0.67f, h * 0.66f))
            }
        }
    }
}

@Composable
private fun NearbyFlyersCardSection(
    flyers: List<FlyerEntity>,
    storesById: Map<String, RetailerEntity>,
    locationLabel: String,
    onLocationFilterClick: () -> Unit,
    onViewAll: () -> Unit,
    onFlyerCardClick: (String) -> Unit
) {
    val palette = LocalAppPalette.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val visibleFlyers = remember(flyers) { flyers.take(4) }
    val pastelCycle = listOf(FlyerMintCard, FlyerBlueCard, FlyerCreamCard, FlyerPinkCard)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RedPinIcon(modifier = Modifier.size(width = 14.dp, height = 17.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.nearby_flyers),
                    style = TextStyle(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.4).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(FigmaPinkPillStart, FigmaPinkPillSolidEnd.copy(alpha = 0.55f))))
                        .clickable { onLocationFilterClick() }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = locationLabel,
                            style = TextStyle(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FigmaCrimsonText,
                                letterSpacing = (-0.2).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = FigmaCrimsonText,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onViewAll() }
                ) {
                    Text(
                        text = stringResource(R.string.view_all),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.crimsonLink,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.crimsonLink,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            if (visibleFlyers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.subtleSurface)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_catalog_data),
                        fontSize = 11.5.sp,
                        color = palette.textSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    visibleFlyers.forEachIndexed { idx, flyer ->
                        val store = storesById[flyer.retailerId]
                        val storeTitle = store?.name ?: flyer.title
                        val bgColor = if (palette.isDark) palette.subtleSurface else pastelCycle[idx % pastelCycle.size]
                        val validText = if (flyer.validTo > 0L) {
                            stringResource(R.string.valid_till, dateFormatter.format(Date(flyer.validTo)))
                        } else {
                            "${flyer.pages} pgs"
                        }
                        val daysRemaining = if (flyer.validTo > now) {
                            ((flyer.validTo - now) / 86_400_000L).toInt().coerceAtLeast(1)
                        } else 1
                        val badgeText = if (daysRemaining == 1) {
                            stringResource(R.string.day_left, 1)
                        } else {
                            stringResource(R.string.days_left, daysRemaining)
                        }
                        val logoUri = store?.logoUrl?.ifBlank { flyer.imageUrl } ?: flyer.imageUrl

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .clickable { onFlyerCardClick(flyer.id) }
                                .padding(horizontal = 5.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = Color(0x14262147), spotColor = Color(0x14262147))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.88f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 5.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (logoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = logoUri,
                                            contentDescription = storeTitle,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text(
                                            text = storeTitle.take(4).uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FigmaTextPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = storeTitle,
                                    style = TextStyle(
                                        fontSize = 8.3.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        letterSpacing = (-0.25).sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = validText,
                                    style = TextStyle(
                                        fontSize = 7.7.sp,
                                        color = palette.textSecondary,
                                        letterSpacing = (-0.2).sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.verticalGradient(listOf(FigmaPinkPillStart, FigmaPinkPillSolidEnd.copy(alpha = 0.55f))))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = TextStyle(
                                            fontSize = 7.2.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FigmaCrimsonText,
                                            letterSpacing = (-0.2).sp,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RedPinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pinPath = Path().apply {
            moveTo(w * 0.50f, h * 0.96f)
            cubicTo(w * 0.14f, h * 0.64f, 0f, h * 0.47f, 0f, h * 0.34f)
            cubicTo(0f, h * 0.12f, w * 0.22f, 0f, w * 0.50f, 0f)
            cubicTo(w * 0.78f, 0f, w, h * 0.12f, w, h * 0.34f)
            cubicTo(w, h * 0.47f, w * 0.86f, h * 0.64f, w * 0.50f, h * 0.96f)
            close()
        }
        drawPath(pinPath, color = FigmaPinRed)
        drawCircle(color = Color.White, radius = w * 0.20f, center = Offset(w * 0.50f, h * 0.33f))
    }
}

private data class TrendingBannerItem(
    val drawableRes: Int,
    val contentDescription: String,
    val targetUrl: String
)

@Composable
private fun TrendingProductsSection(
    onViewAll: () -> Unit,
    onBannerClick: (String) -> Unit
) {
    val palette = LocalAppPalette.current
    val coroutineScope = rememberCoroutineScope()
    val banners = remember {
        listOf(
            TrendingBannerItem(
                drawableRes = R.drawable.banner_trending_nescafe,
                contentDescription = "Discover the world of Nescafé",
                targetUrl = "https://gcc.luluhypermarket.com/en-ae/list/?search_text=nescafe"
            ),
            TrendingBannerItem(
                drawableRes = R.drawable.banner_trending_nestle_range,
                contentDescription = "Discover the Nestlé range",
                targetUrl = "https://gcc.luluhypermarket.com/en-ae/list/?search_text=nestle"
            ),
            TrendingBannerItem(
                drawableRes = R.drawable.banner_trending_nestle_explore,
                contentDescription = "Explore more of Nestlé",
                targetUrl = "https://gcc.luluhypermarket.com/en-ae/list/?search_text=nestle+products"
            )
        )
    }
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && banners.size > 1) {
            delay(3500L)
            val nextPage = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.trending_products),
                style = TextStyle(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.4).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onViewAll() }
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.crimsonLink,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.crimsonLink,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val banner = banners[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .aspectRatio(960f / 504f)
                    .shadow(5.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .clickable { onBannerClick(banner.targetUrl) }
            ) {
                Image(
                    painter = painterResource(id = banner.drawableRes),
                    contentDescription = banner.contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(banners.size) { index ->
                if (index > 0) Spacer(modifier = Modifier.width(7.dp))
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 7.dp else 5.5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) FigmaCarouselActiveDot else FigmaCarouselInactiveDot)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun TopDealsTodaySection(
    deals: List<ProductEntity>,
    getStore: (String) -> RetailerEntity?,
    onViewAll: () -> Unit,
    onProductClick: (String) -> Unit,
    onAddToList: (ProductEntity) -> Unit
) {
    val palette = LocalAppPalette.current
    val currency = stringResource(R.string.currency_aed)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_fig_deals_flame),
                contentDescription = null,
                modifier = Modifier.size(width = 15.dp, height = 18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.top_deals_today),
                style = TextStyle(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.4).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.view_all),
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.crimsonLink,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.clickable { onViewAll() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (deals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_catalog_data),
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(deals, key = { it.id }) { product ->
                    val oldPrice = product.oldPrice
                    val discountPct = if (oldPrice != null && oldPrice > product.currentPrice && oldPrice > 0.0) {
                        (((oldPrice - product.currentPrice) / oldPrice) * 100).toInt().takeIf { it > 0 }
                    } else null
                    val storeDisplayName = getStore(product.retailerId)?.name
                        ?: product.sourceLabel.ifBlank { "UAE Store" }
                    val storeLower = storeDisplayName.lowercase()
                    val storeColor = when {
                        storeLower.contains("carrefour") -> FigmaRetailerBlue
                        storeLower.contains("nesto") -> FigmaRetailerNestoRed
                        else -> FigmaDiscountGreen
                    }
                    val badgeColor = when {
                        discountPct != null && discountPct >= 20 -> FigmaDiscountGreen
                        discountPct != null && discountPct >= 15 -> FigmaDiscountRed
                        else -> FigmaDiscountPurple
                    }
                    val subtitleMeta = buildString {
                        if (product.brand.isNotBlank() && !product.brand.equals("Fresh Produce", ignoreCase = true)) {
                            append(product.brand)
                        }
                        if (product.unitSize > 0 && product.unitType.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            val s = if (product.unitSize % 1.0 == 0.0) product.unitSize.toInt().toString() else product.unitSize.toString()
                            append("$s ${product.unitType}")
                        } else if (isEmpty() && product.category.isNotBlank()) {
                            append(product.category)
                        }
                    }
                    CompactDealCardFrame(
                        title = product.name,
                        subtitle = subtitleMeta.ifBlank { storeDisplayName },
                        currentPrice = "$currency %.2f".format(product.currentPrice),
                        oldPrice = if (oldPrice != null && oldPrice > product.currentPrice) "$currency %.2f".format(oldPrice) else null,
                        storeName = storeDisplayName,
                        storeColor = storeColor,
                        badgeText = discountPct?.let { "-$it%" },
                        badgeColor = badgeColor,
                        onClick = { onProductClick(product.id) },
                        onAddClick = { onAddToList(product) }
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = product.name,
                                tint = palette.textMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDealCardFrame(
    title: String,
    subtitle: String,
    currentPrice: String,
    oldPrice: String?,
    storeName: String,
    storeColor: Color,
    badgeText: String?,
    badgeColor: Color,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    imageContent: @Composable BoxScope.() -> Unit
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .width(102.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(0.8.dp, palette.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                imageContent()
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(50))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 7.2.sp,
                                fontWeight = FontWeight.Medium,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = title,
                style = TextStyle(
                    fontSize = 8.6.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.25).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    fontSize = 7.6.sp,
                    color = palette.textSecondary,
                    letterSpacing = (-0.2).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = currentPrice,
                    style = TextStyle(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.crimsonLink,
                        letterSpacing = (-0.25).sp,
                        fontFeatureSettings = "tnum",
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1
                )
                if (oldPrice != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = oldPrice,
                        style = TextStyle(
                            fontSize = 6.3.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textMuted,
                            textDecoration = TextDecoration.LineThrough,
                            letterSpacing = (-0.2).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = storeName,
                    style = TextStyle(
                        fontSize = 7.6.sp,
                        fontWeight = FontWeight.Bold,
                        color = storeColor,
                        letterSpacing = (-0.2).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(palette.crimsonPillBg)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_btn),
                        tint = FigmaCrimsonLink,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private data class SavingsBannerSlide(
    val title: String,
    val desc: String,
    val cta: String,
    val onClick: () -> Unit
)

@Composable
private fun SmartSavingsCarouselSection(
    onCreateSmartList: () -> Unit,
    onOptimizeBasket: () -> Unit,
    onOpenWallet: () -> Unit
) {
    val slides = listOf(
        SavingsBannerSlide(
            title = stringResource(R.string.smart_savings_title),
            desc = stringResource(R.string.smart_savings_desc),
            cta = stringResource(R.string.create_smart_list),
            onClick = onCreateSmartList
        ),
        SavingsBannerSlide(
            title = stringResource(R.string.basket_optimizer_title),
            desc = stringResource(R.string.basket_optimizer_subtitle),
            cta = stringResource(R.string.open_basket_optimizer),
            onClick = onOptimizeBasket
        ),
        SavingsBannerSlide(
            title = stringResource(R.string.loyalty_wallet_title),
            desc = stringResource(R.string.loyalty_wallet_subtitle),
            cta = stringResource(R.string.open_loyalty_wallet),
            onClick = onOpenWallet
        )
    )
    val pagerState = rememberPagerState(pageCount = { slides.size })

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIdx ->
            val slide = slides[pageIdx]
            SmartSavingsBanner(
                title = slide.title,
                description = slide.desc,
                ctaLabel = slide.cta,
                onAction = slide.onClick
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 7.dp else 5.5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) FigmaCarouselActiveDot else FigmaCarouselInactiveDot)
                )
            }
        }
    }
}

@Composable
fun SmartSavingsBanner(
    title: String = stringResource(R.string.smart_savings_title),
    description: String = stringResource(R.string.smart_savings_desc),
    ctaLabel: String = stringResource(R.string.create_smart_list),
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(98.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x14252039), spotColor = Color(0x14252039))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(FigmaBannerGradStart, FigmaBannerGradMid, FigmaBannerGradEnd)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55FFBEC9), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.50f),
                    radius = size.height * 0.9f
                ),
                radius = size.height * 0.9f,
                center = Offset(size.width * 0.78f, size.height * 0.50f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.fig_smart_bag),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 40.dp, height = 44.dp)
                    .offset(y = (-4).dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaTextPrimary,
                        letterSpacing = (-0.4).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = TextStyle(
                        fontSize = 9.sp,
                        lineHeight = 11.5.sp,
                        color = FigmaTextPrimary,
                        letterSpacing = (-0.2).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(23.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x30272445), spotColor = Color(0x30272445))
                        .clip(RoundedCornerShape(16.dp))
                        .background(FigmaDarkButton)
                        .clickable { onAction() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ctaLabel,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.2).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(114.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fig_smart_basket),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 102.dp, height = 96.dp)
                        .offset(x = (-8).dp),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.fig_smart_discount),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-2).dp, y = (-10).dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun UpcomingPromotionsHeader(
    onViewAll: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(1.8.dp)
                .height(14.dp)
                .background(FigmaAccentBorder)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = stringResource(R.string.upcoming_promotions),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.view_all),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.crimsonLink,
            modifier = Modifier.clickable { onViewAll() }
        )
    }
}

@Composable
fun PromotionCard(
    promotion: PromotionEntity,
    store: RetailerEntity?,
    onClick: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, palette.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .width(276.dp)
            .background(
                Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                store?.name ?: "Verified Partner",
                style = MaterialTheme.typography.labelSmall,
                color = palette.crimsonLink,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                promotion.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                promotion.description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            val discountBadgeText = promotion.discountInfo.ifBlank {
                if (promotion.discount > 0.0) {
                    val pct = if (promotion.discount % 1.0 == 0.0) promotion.discount.toInt().toString() else promotion.discount.toString()
                    "$pct% OFF"
                } else {
                    "SPECIAL OFFER"
                }
            }
            Surface(
                color = palette.crimsonPillBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = discountBadgeText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.crimsonLink,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: String, onAction: () -> Unit) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = palette.crimsonPillBg
                ) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.crimsonLink,
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
                    color = palette.crimsonLink,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: ProductEntity,
    store: RetailerEntity? = null,
    onClick: () -> Unit = {},
    onAddToList: (() -> Unit)? = null
) {
    val palette = LocalAppPalette.current
    val currency = stringResource(R.string.currency_aed)
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, palette.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                .padding(12.dp)
        ) {
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.subtleSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
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
                if (discountPercent != null) {
                    Surface(
                        color = FigmaDiscountRed,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
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
                color = palette.textPrimary
            )
            if (subtitleMeta.isNotBlank()) {
                Text(
                    text = subtitleMeta,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currency %.2f".format(product.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.crimsonLink
                )
                if (oldPrice != null && oldPrice > product.currentPrice) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$currency %.2f".format(oldPrice),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            if (unitPriceText != null) {
                Text(
                    text = unitPriceText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = palette.textMuted,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = store?.name ?: "Verified Retailer",
                style = MaterialTheme.typography.labelSmall,
                color = FigmaDiscountGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (onAddToList != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onAddToList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.crimsonPillBg,
                        contentColor = palette.crimsonLink
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.add_btn),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
