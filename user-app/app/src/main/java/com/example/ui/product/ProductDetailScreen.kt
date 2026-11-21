package com.example.ui.product

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.domain.basket.OfferEligibility
import com.example.ui.DealFinderViewModel
import com.example.ui.compare.SearchResultItem
import com.example.ui.home.SectionHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: DealFinderViewModel,
    isSignedIn: Boolean = false,
    onBack: () -> Unit,
    onAddToList: (ProductEntity) -> Unit,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current

    val isLoaded by viewModel.isLoaded.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val products by viewModel.products.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val savedDeals by viewModel.savedDeals.collectAsState()
    val product = products.find { it.id == productId }
    val store = product?.let { viewModel.getStore(it.retailerId) }
    val isSaved = product?.let { p -> savedDeals.any { it.id == p.id } } ?: false

    var priceAlertEnabled by remember(productId) { mutableStateOf(false) }

    if (!isLoaded && product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FigmaCrimsonLink)
        }
        return
    }

    if (product == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.textPrimary)
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Product not found or unavailable.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        syncState.errorMessage ?: "The deal may have expired or is not currently active in your selected Emirate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)) {
                        Text("Go Back", color = Color.White)
                    }
                }
            }
        }
        return
    }

    val effectivePriceAed = product.effectiveAmountMinor() / 100.0
    val effectiveCompAed = product.effectiveComparisonPriceMinor()?.let { it / 100.0 }
    val currency = stringResource(R.string.currency_aed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Unified Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.textPrimary, modifier = Modifier.size(21.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable {
                            priceAlertEnabled = !priceAlertEnabled
                            Toast.makeText(
                                context,
                                if (priceAlertEnabled) "Price drop alert enabled for ${product.name}" else "Price drop alert removed",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (priceAlertEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                        contentDescription = "Price Alert",
                        tint = if (priceAlertEnabled) palette.crimsonLink else palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable {
                            if (!isSignedIn) {
                                Toast.makeText(context, "Sign in to save deals", Toast.LENGTH_SHORT).show()
                                onNavigateToAuth()
                            } else {
                                viewModel.toggleSavedDeal(product)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isSaved) "Remove from Saved" else "Save Deal",
                        tint = if (isSaved) palette.crimsonLink else palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, "Check out this deal: ${product.name} at $currency ${"%.2f".format(effectivePriceAed)}")
                                type = "text/plain"
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, "Share deal"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Unable to open share sheet", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Product Showcase Image Frame
        val fallbackUrl = "file:///android_asset/products/default_deal.jpg"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(250.dp)
                .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = product.imageUrl.ifBlank { fallbackUrl },
                error = coil.compose.rememberAsyncImagePainter(fallbackUrl),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize().padding(18.dp),
                contentScale = ContentScale.Fit
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (product.brand.isNotBlank()) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.crimsonLink
                )
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$currency ${"%.2f".format(effectivePriceAed)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.crimsonLink
                )
                if (effectiveCompAed != null && effectiveCompAed > effectivePriceAed) {
                    Text(
                        text = "$currency ${"%.2f".format(effectiveCompAed)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                    val discountPct = ((effectiveCompAed - effectivePriceAed) / effectiveCompAed * 100).toInt()
                    if (discountPct > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.crimsonPillBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "-$discountPct%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.crimsonLink
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
            val sourceDisplay = product.sourceLabel.ifBlank {
                when (product.sourceType.lowercase()) {
                    "merchant_portal" -> "Merchant portal feed"
                    "retailer_flyer" -> "Retailer flyer"
                    "demo" -> "UAE Demo Catalog"
                    else -> "Retailer flyer / authorized feed"
                }
            }
            val lastConfirmedDisplay = product.lastVerifiedAt?.takeIf { it > 0L }?.let {
                dateFmt.format(Date(it))
            } ?: if (product.verificationLabel.equals("verified", ignoreCase = true)) {
                "Verified by reviewer"
            } else {
                "Check in-store"
            }
            val unitPriceText = OfferEligibility.computeUnitPriceText(product)
            val packSummary = buildString {
                if (product.unitSize > 0 && product.unitType.isNotBlank()) {
                    val sizeStr = if (product.unitSize % 1.0 == 0.0) product.unitSize.toInt().toString() else product.unitSize.toString()
                    if (product.packCount > 1) append("${product.packCount} x $sizeStr ${product.unitType}")
                    else append("$sizeStr ${product.unitType}")
                }
                if (unitPriceText != null) {
                    if (isNotEmpty()) append(" ($unitPriceText)") else append(unitPriceText)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Source: $sourceDisplay", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    Text("Last confirmed: $lastConfirmedDisplay", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    if (packSummary.isNotBlank()) {
                        Text("Pack / Unit: $packSummary", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    if (product.validTo > 0L) {
                        Text(stringResource(R.string.valid_till, dateFmt.format(Date(product.validTo))), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    if (product.loyaltyRequired) {
                        Text("Requires retailer loyalty card", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.crimsonLink)
                    }
                    if (product.promoConditions.isNotBlank()) {
                        Text("Terms: ${product.promoConditions}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
            }

            if (store != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        .clickable { onNavigateToRetailer(store.id) }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val storeFallbackUrl = "file:///android_asset/logos/default_logo.png"
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = store.logoUrl.ifBlank { storeFallbackUrl },
                                error = coil.compose.rememberAsyncImagePainter(storeFallbackUrl),
                                contentDescription = store.name,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Available at", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            Text(store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.textMuted)
                    }
                }

                val (nearestBranch, nearestDistanceKm) = branches.let { viewModel.getNearestBranchAndDistance(store.id) }
                if (nearestBranch != null && nearestBranch.isEligibleForDistance()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val label = Uri.encode("${store.name} - ${nearestBranch.name}")
                            val uri = Uri.parse("geo:${nearestBranch.latitude},${nearestBranch.longitude}?q=${nearestBranch.latitude},${nearestBranch.longitude}($label)")
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No maps app installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, palette.crimsonLink),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.crimsonLink)
                        Spacer(modifier = Modifier.width(8.dp))
                        val distText = nearestDistanceKm?.let { " (${"%.1f".format(it)} km)" } ?: ""
                        Text("Directions to ${nearestBranch.name}$distText", color = palette.crimsonLink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0x40ED0032), spotColor = Color(0x40ED0032))
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                    .clickable {
                        onAddToList(product)
                        Toast.makeText(context, context.getString(R.string.added_to_list, product.name), Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_btn) + " • " + stringResource(R.string.smart_list_title), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            SectionHeader(title = stringResource(R.string.action_price_compare).replace("\n", " "), action = "", onAction = {})
        }

        val similarProducts = remember(products, product) {
            products.filter { it.isIdenticalTo(product) }.sortedBy { it.currentPrice }
        }
        val minPrice = similarProducts.firstOrNull()?.currentPrice ?: Double.MAX_VALUE
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            similarProducts.forEach { simProduct ->
                val (simBranch, distanceKm) = viewModel.getNearestBranchAndDistance(simProduct.retailerId)
                SearchResultItem(
                    product = simProduct,
                    store = viewModel.getStore(simProduct.retailerId),
                    isCheapest = simProduct.currentPrice <= minPrice,
                    distanceKm = distanceKm,
                    nearestBranchName = simBranch?.name,
                    onClick = { onNavigateToProduct(simProduct.id) },
                    onStoreClick = { onNavigateToRetailer(simProduct.retailerId) },
                    onAddToList = {
                        onAddToList(simProduct)
                        Toast.makeText(context, context.getString(R.string.added_to_list, simProduct.name), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
