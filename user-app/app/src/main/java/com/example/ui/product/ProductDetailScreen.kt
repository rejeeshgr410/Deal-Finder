package com.example.ui.product

import com.example.data.isIdenticalTo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import com.example.data.isEligibleForDistance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.example.ui.home.SectionHeader
import com.example.ui.compare.SearchResultItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: com.example.ui.DealFinderViewModel,
    onBack: () -> Unit,
    onAddToList: () -> Unit,
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {}
) {
    val isLoaded by viewModel.isLoaded.collectAsState()
    val products by viewModel.products.collectAsState()
    val savedDeals by viewModel.savedDeals.collectAsState()
    val product = products.find { it.id == productId }
    val store = product?.let { viewModel.getStore(it.retailerId) }
    val isSaved = product?.let { p -> savedDeals.any { it.id == p.id } } ?: false

    if (!isLoaded) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Loading Deal...") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (product == null || store == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Product not found or unavailable.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The deal may have expired or is not currently active in your selected Emirate.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = { viewModel.toggleSavedDeal(product) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isSaved) "Remove from Saved" else "Save Deal",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { 
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out this deal: ${product.name} at AED ${product.currentPrice}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share deal"))
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val fallbackUrl = "file:///android_asset/products/default_deal.jpg"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                AsyncImage(
                    model = product.imageUrl.ifBlank { fallbackUrl },
                    error = coil.compose.rememberAsyncImagePainter(fallbackUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                if (product.brand.isNotBlank()) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "AED ${"%.2f".format(product.currentPrice)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.oldPrice != null && product.oldPrice > product.currentPrice) {
                        Text(
                            text = "AED ${"%.2f".format(product.oldPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                        val discountPct = ((product.oldPrice - product.currentPrice) / product.oldPrice * 100).toInt()
                        if (discountPct > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFE4E6)
                            ) {
                                Text(
                                    text = "-$discountPct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE11D48),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Source, verification, unit/pack, and promo terms disclosure (S5.2)
                val dateFmt = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                val sourceDisplay = product.sourceLabel.ifBlank {
                    when (product.sourceType.lowercase()) {
                        "merchant_portal" -> "Merchant portal feed"
                        "retailer_flyer" -> "Retailer flyer"
                        else -> "Retailer flyer / authorized feed"
                    }
                }
                val lastConfirmedDisplay = product.lastVerifiedAt?.takeIf { it > 0L }?.let {
                    dateFmt.format(java.util.Date(it))
                } ?: if (product.verificationLabel.equals("verified", ignoreCase = true)) {
                    "Verified by reviewer"
                } else {
                    "Not independently confirmed — check in-store"
                }
                val unitPriceText = com.example.domain.basket.OfferEligibility.computeUnitPriceText(product)
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

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Source: $sourceDisplay",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Last confirmed: $lastConfirmedDisplay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (packSummary.isNotBlank()) {
                            Text(
                                text = "Pack / Unit: $packSummary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (product.validTo > 0L) {
                            Text(
                                text = "Valid until: ${dateFmt.format(java.util.Date(product.validTo))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (product.loyaltyRequired) {
                            Text(
                                text = "Requires retailer loyalty card",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (product.promoConditions.isNotBlank()) {
                            Text(
                                text = "Terms: ${product.promoConditions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = { onNavigateToRetailer(store.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val storeFallbackUrl = "file:///android_asset/logos/default_logo.png"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            AsyncImage(
                                model = store.logoUrl.ifBlank { storeFallbackUrl },
                                error = coil.compose.rememberAsyncImagePainter(storeFallbackUrl),
                                contentDescription = store.name,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Available at", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                val (nearestBranch, nearestDistanceKm) = viewModel.getNearestBranchAndDistance(store.id)
                val detailContext = androidx.compose.ui.platform.LocalContext.current
                if (nearestBranch != null && nearestBranch.isEligibleForDistance()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val label = android.net.Uri.encode("${store.name} - ${nearestBranch.name}")
                            val uri = android.net.Uri.parse("geo:${nearestBranch.latitude},${nearestBranch.longitude}?q=${nearestBranch.latitude},${nearestBranch.longitude}($label)")
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            try {
                                detailContext.startActivity(mapIntent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(detailContext, "No maps app installed", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val distText = nearestDistanceKm?.let { " (${"%.1f".format(it)} km straight-line)" } ?: ""
                        Text("Directions to ${nearestBranch.name}$distText")
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Branch directions unavailable (verified branch coordinates not yet available).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddToList,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Shopping List", fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader(title = "Price Comparison", action = "", onAction = {})
                
                val similarProducts = products.filter {
                    it.isIdenticalTo(product)
                }.sortedBy { it.currentPrice }
                val minPrice = similarProducts.firstOrNull()?.currentPrice ?: Double.MAX_VALUE
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
                        onAddToList = onAddToList
                    )
                }
            }
        }
    }
}
