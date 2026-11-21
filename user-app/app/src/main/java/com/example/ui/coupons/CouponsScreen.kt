package com.example.ui.coupons

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CouponEntity
import com.example.data.DemoCouponStyle
import com.example.data.DemoRegistry
import com.example.data.LocationManager
import com.example.ui.DealFinderViewModel
import com.example.ui.theme.*
import java.util.concurrent.TimeUnit

private val couponFilterKeys = listOf(
    "All", "Saved", "Supermarkets", "Fashion", "Electronics", "Beauty"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponsScreen(
    viewModel: DealFinderViewModel,
    onBack: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current

    val allCoupons by viewModel.coupons.collectAsState()
    val savedCoupons by viewModel.savedCoupons.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()
    val demoEnabled by DemoRegistry.isDemoEnabled.collectAsState()

    val demoStyles = remember(demoEnabled) { DemoRegistry.currentSnapshot().couponStylesByCode }
    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortMode by remember { mutableStateOf("Featured") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var copiedCode by remember { mutableStateOf<String?>(null) }

    val savedIds = remember(savedCoupons) { savedCoupons.map { it.id }.toSet() }
    val savedCodes = remember(savedCoupons) { savedCoupons.map { it.code.uppercase() }.toSet() }

    val filteredCoupons = remember(allCoupons, savedIds, savedCodes, searchQuery, selectedCategory, sortMode) {
        val q = searchQuery.trim().lowercase()
        allCoupons.filter { item ->
            val isSaved = item.id in savedIds || item.code.uppercase() in savedCodes
            val matchesCat = when (selectedCategory) {
                "All" -> true
                "Saved" -> isSaved
                else -> item.category.contains(selectedCategory, ignoreCase = true) ||
                    item.description.contains(selectedCategory, ignoreCase = true) ||
                    item.title.contains(selectedCategory, ignoreCase = true)
            }
            val matchesQuery = q.isEmpty() ||
                item.storeName.lowercase().contains(q) ||
                item.title.lowercase().contains(q) ||
                item.code.lowercase().contains(q) ||
                item.discountInfo.lowercase().contains(q) ||
                item.description.lowercase().contains(q)
            matchesCat && matchesQuery
        }.let { list ->
            when (sortMode) {
                "Expiring Soon" -> list.sortedBy { if (it.expiry > 0L) it.expiry else Long.MAX_VALUE }
                "Highest Discount" -> list.sortedByDescending {
                    Regex("(\\d+)").find(it.discountInfo)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                }
                else -> list.sortedByDescending { it.isFeatured }
            }
        }
    }

    val featuredList = remember(filteredCoupons) {
        val featured = filteredCoupons.filter { it.isFeatured }
        if (featured.isNotEmpty()) featured else filteredCoupons
    }
    val extraPartnerList = remember(filteredCoupons, featuredList) {
        val featIds = featuredList.map { it.id }.toSet()
        filteredCoupons.filter { it.id !in featIds }
    }

    fun copyCouponCode(coupon: CouponEntity) {
        val code = coupon.code.trim()
        if (code.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Coupon Code", code))
        copiedCode = code
        val label = coupon.storeName.ifBlank { coupon.title }
        Toast.makeText(context, "$label: $code (${context.getString(R.string.copied)})", Toast.LENGTH_SHORT).show()
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
                        selectedCategory = "All"
                        sortMode = "Featured"
                        searchQuery = ""
                    }) {
                        Text(stringResource(R.string.reset_filters), color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                    }
                }

                Text(stringResource(R.string.sort_by), fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Featured", "Highest Discount", "Expiring Soon")) { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                            label = { Text(mode, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.crimsonPillBg,
                                selectedLabelColor = palette.crimsonLink
                            )
                        )
                    }
                }

                Text(stringResource(R.string.filter_category), fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(couponFilterKeys) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.crimsonPillBg,
                                selectedLabelColor = palette.crimsonLink
                            )
                        )
                    }
                }

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
                ) {
                    Text(stringResource(R.string.apply_filters), fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header Row
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
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
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.coupons_title),
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                letterSpacing = (-0.5).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.coupons_subtitle),
                            style = TextStyle(
                                fontSize = 10.5.sp,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.textPrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(localizedEmirate, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary, maxLines = 1)
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
                        if (unreadNotifCount > 0) {
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

        // 2. Search Bar with Crimson Filter Pill
        item(span = { GridItemSpan(2) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .shadow(7.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x2024203C), spotColor = Color(0x2024203C))
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp))
                    .padding(start = 15.dp, end = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_coupons_placeholder),
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
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = palette.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(35.dp)
                            .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x40ED0032), spotColor = Color(0x40ED0032))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                            .border(0.8.dp, Color(0x66FF5575), RoundedCornerShape(20.dp))
                            .clickable { showFilterSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.filter_and_sort), tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 3. Category Pills Row
        item(span = { GridItemSpan(2) }) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(couponFilterKeys) { category ->
                    val selected = selectedCategory == category
                    val label = when (category) {
                        "All" -> stringResource(R.string.tab_all)
                        "Saved" -> stringResource(R.string.saved_coupons)
                        else -> category
                    }
                    Box(
                        modifier = Modifier
                            .height(31.dp)
                            .shadow(
                                elevation = if (selected) 6.dp else 4.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = if (selected) Color(0x40ED0032) else Color(0x1624203C),
                                spotColor = if (selected) Color(0x40ED0032) else Color(0x1624203C)
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
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
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

        // 4. Featured Coupons Section Header
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RedCouponTagIcon(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.featured_coupons),
                        style = TextStyle(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            letterSpacing = (-0.3).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        selectedCategory = "All"
                        searchQuery = ""
                    }
                ) {
                    Text(
                        text = stringResource(R.string.view_all),
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

        if (syncState.isLoading && allCoupons.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FigmaCrimsonLink)
                }
            }
        } else if (featuredList.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.no_coupons_match), color = palette.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = {
                            searchQuery = ""
                            selectedCategory = "All"
                            sortMode = "Featured"
                        }) {
                            Text(stringResource(R.string.reset_filters), color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(featuredList, key = { it.id }) { coupon ->
                val style = demoStyles[coupon.code.trim().uppercase()]
                val isSaved = coupon.id in savedIds || coupon.code.uppercase() in savedCodes
                BentoCouponGridCard(
                    coupon = coupon,
                    demoStyle = style,
                    isCopied = copiedCode == coupon.code,
                    isSaved = isSaved,
                    onToggleSave = { viewModel.toggleSavedCoupon(coupon) },
                    onCopy = { copyCouponCode(coupon) },
                    onCardClick = {
                        if (coupon.retailerId.isNotBlank()) onNavigateToRetailer(coupon.retailerId)
                    }
                )
            }
        }

        // 5. Promo Banner
        item(span = { GridItemSpan(2) }) {
            CouponBottomPromoBanner()
        }

        // 6. Additional Partner & Store Codes
        if (extraPartnerList.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = stringResource(R.string.more_store_coupons),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
            items(extraPartnerList, key = { "extra_${it.id}" }) { coupon ->
                val style = demoStyles[coupon.code.trim().uppercase()]
                val isSaved = coupon.id in savedIds || coupon.code.uppercase() in savedCodes
                BentoCouponGridCard(
                    coupon = coupon,
                    demoStyle = style,
                    isCopied = copiedCode == coupon.code,
                    isSaved = isSaved,
                    onToggleSave = { viewModel.toggleSavedCoupon(coupon) },
                    onCopy = { copyCouponCode(coupon) },
                    onCardClick = {
                        if (coupon.retailerId.isNotBlank()) onNavigateToRetailer(coupon.retailerId)
                    }
                )
            }
        }
    }
}

@Composable
private fun BentoCouponGridCard(
    coupon: CouponEntity,
    demoStyle: DemoCouponStyle?,
    isCopied: Boolean,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onCopy: () -> Unit,
    onCardClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val bgTop = if (palette.isDark) {
        palette.surfaceGradStart
    } else {
        demoStyle?.let { Color(it.bgTopHex) } ?: palette.surfaceGradStart
    }
    val bgBottom = if (palette.isDark) {
        palette.surfaceGradEnd
    } else {
        demoStyle?.let { Color(it.bgBottomHex) } ?: palette.surfaceGradEnd
    }
    val brandDisplayName = coupon.storeName.ifBlank { coupon.title }
    val subtitleText = buildString {
        append(coupon.description.ifBlank { demoStyle?.secondaryLabel?.takeIf { it.isNotBlank() } ?: "Online & app" })
        if (coupon.expiry > System.currentTimeMillis()) {
            val days = TimeUnit.MILLISECONDS.toDays(coupon.expiry - System.currentTimeMillis()).toInt().coerceAtLeast(1)
            append(" • ${days}d left")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x1624203C), spotColor = Color(0x1624203C))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Box(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicBrandHeader(coupon = coupon, style = demoStyle)
                }
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save coupon",
                    tint = if (isSaved) FigmaCrimsonLink else palette.textMuted,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clickable { onToggleSave() }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = coupon.discountInfo.ifBlank { "SPECIAL DEAL" },
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.3).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitleText,
                style = TextStyle(
                    fontSize = 9.sp,
                    color = palette.textMuted,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.08f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (palette.isDark) palette.crimsonPillBg else Color(0xFFFCE2E8))
                        .border(0.6.dp, palette.cardBorder.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = coupon.code,
                        style = TextStyle(
                            fontSize = 9.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (palette.isDark) palette.textPrimary else Color(0xFF3E1822),
                            letterSpacing = 0.1.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.92f)
                        .height(24.dp)
                        .shadow(4.dp, RoundedCornerShape(50), ambientColor = Color(0x38ED0032), spotColor = Color(0x38ED0032))
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isCopied) Brush.verticalGradient(listOf(Color(0xFF05A724), FigmaDiscountGreen))
                            else Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd))
                        )
                        .clickable { onCopy() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCopied) stringResource(R.string.copied_upper) else stringResource(R.string.copy),
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.3.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicBrandHeader(
    coupon: CouponEntity,
    style: DemoCouponStyle?
) {
    val palette = LocalAppPalette.current
    val name = coupon.storeName.ifBlank { coupon.title }
    val logoUri = style?.logoUri.orEmpty()

    when (style?.styleKey) {
        "noon" -> Row(verticalAlignment = Alignment.CenterVertically) {
            if (logoUri.isNotBlank()) {
                AsyncImage(model = logoUri, contentDescription = name, modifier = Modifier.size(25.dp), contentScale = ContentScale.Fit)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text("noon", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
        }
        "amazon" -> Row(verticalAlignment = Alignment.Top) {
            if (logoUri.isNotBlank()) {
                AsyncImage(model = logoUri, contentDescription = name, modifier = Modifier.width(74.dp).height(24.dp), contentScale = ContentScale.Fit)
            } else {
                Text("amazon", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary)
            }
            Text(".ae", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
        "namshi" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("نمشي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
            Text("NAMSHI", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp, color = palette.textPrimary, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
        }
        "talabat" -> Text(
            text = "talabat",
            style = TextStyle(
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF5A00),
                letterSpacing = (-0.7).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
        "nesto" -> Row(verticalAlignment = Alignment.CenterVertically) {
            if (logoUri.isNotBlank()) {
                AsyncImage(model = logoUri, contentDescription = name, modifier = Modifier.height(25.dp), contentScale = ContentScale.Fit)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("نستو", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE00024), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
                Text("NESTO", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFE00024), style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
            }
        }
        else -> {
            if (logoUri.isNotBlank()) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = name,
                    modifier = Modifier.width(96.dp).height(24.dp),
                    contentScale = ContentScale.Fit
                )
            } else if (coupon.domain.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://www.google.com/s2/favicons?domain=${coupon.domain}&sz=128",
                        contentDescription = name,
                        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CouponBottomPromoBanner() {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1624203C), spotColor = Color(0x1624203C))
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (palette.isDark) {
                    Brush.horizontalGradient(listOf(Color(0xFF281722), Color(0xFF331B29)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFFEF0F5), Color(0xFFFDE4ED), Color(0xFFFCD6E3)))
                }
            )
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TiltedCouponTicketGraphic(modifier = Modifier.size(width = 64.dp, height = 40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.coupon_banner_title),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.3).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.coupon_banner_subtitle),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = palette.textSecondary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun TiltedCouponTicketGraphic(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawLine(Color(0xFFFF7A93), Offset(w * 0.04f, h * 0.28f), Offset(w * 0.16f, h * 0.34f), 2.5f, StrokeCap.Round)
            drawLine(Color(0xFFFF7A93), Offset(0f, h * 0.50f), Offset(w * 0.14f, h * 0.50f), 2.5f, StrokeCap.Round)
            drawLine(Color(0xFFFF7A93), Offset(w * 0.04f, h * 0.72f), Offset(w * 0.16f, h * 0.66f), 2.5f, StrokeCap.Round)

            rotate(degrees = -12f, pivot = Offset(w * 0.60f, h * 0.52f)) {
                val tx = w * 0.24f
                val ty = h * 0.12f
                val tw = w * 0.72f
                val th = h * 0.78f
                val tPath = Path().apply {
                    moveTo(tx + 8f, ty)
                    lineTo(tx + tw - 8f, ty)
                    lineTo(tx + tw, ty + th * 0.32f)
                    cubicTo(tx + tw * 0.85f, ty + th * 0.44f, tx + tw * 0.85f, ty + th * 0.56f, tx + tw, ty + th * 0.68f)
                    lineTo(tx + tw - 8f, ty + th)
                    lineTo(tx + 8f, ty + th)
                    lineTo(tx, ty + th * 0.68f)
                    cubicTo(tx + tw * 0.15f, ty + th * 0.56f, tx + tw * 0.15f, ty + th * 0.44f, tx, ty + th * 0.32f)
                    close()
                }
                drawPath(
                    tPath,
                    brush = Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd))
                )
            }
        }
        Text(
            text = "%",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .padding(start = 14.dp)
                .rotate(-12f)
        )
    }
}

@Composable
private fun RedCouponTagIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.08f, h * 0.56f)
            lineTo(w * 0.56f, h * 0.08f)
            lineTo(w * 0.88f, h * 0.08f)
            lineTo(w * 0.92f, h * 0.44f)
            lineTo(w * 0.44f, h * 0.92f)
            close()
        }
        drawPath(p, color = FigmaCrimsonLink, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p, color = FigmaCrimsonLink)
        drawCircle(color = Color.White, radius = w * 0.10f, center = Offset(w * 0.72f, h * 0.28f))
    }
}
