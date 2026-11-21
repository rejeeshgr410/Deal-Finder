package com.example.ui.retailer

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ProductEntity
import com.example.data.isEligibleForDistance
import com.example.ui.DealFinderViewModel
import com.example.ui.compare.SearchResultItem
import com.example.ui.flyers.FlyerSummaryCard
import com.example.ui.home.SectionHeader
import com.example.ui.theme.*

@Composable
fun RetailerScreen(
    retailerId: String,
    viewModel: DealFinderViewModel,
    isSignedIn: Boolean = false,
    onNavigateToAuth: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToFlyer: (String) -> Unit = {},
    onAddToList: (ProductEntity) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val isLoaded by viewModel.isLoaded.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val flyers by viewModel.flyers.collectAsState()
    val products by viewModel.products.collectAsState()
    val followedRetailers by viewModel.followedRetailers.collectAsState()

    val store = viewModel.getStore(retailerId)

    if (!isLoaded && store == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FigmaCrimsonLink)
        }
        return
    }

    if (store == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Retailer not found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This retailer is currently offline or unavailable in your chosen Emirate.", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onBack, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)) {
                        Text("Go back", color = Color.White)
                    }
                }
            }
        }
        return
    }

    val storeProducts = remember(products, store.id) { products.filter { it.retailerId == store.id } }
    val storeFlyers = remember(flyers, store.id) { flyers.filter { it.retailerId == store.id } }
    val storeBranches = remember(branches, store.id) { viewModel.getBranchesForStoreInEmirate(store.id) }
    val isFollowing = followedRetailers.any { it.id == store.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
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
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(store.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
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
                                Toast.makeText(context, "Sign in to follow retailers", Toast.LENGTH_SHORT).show()
                                onNavigateToAuth()
                            } else {
                                viewModel.toggleFollowedRetailer(store)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFollowing) "Unfollow" else "Follow",
                        tint = if (isFollowing) palette.crimsonLink else palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Store Hero Banner Card
        item {
            val storeFallbackUrl = "file:///android_asset/logos/default_logo.png"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .shadow(7.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = store.logoUrl.ifBlank { storeFallbackUrl },
                            error = coil.compose.rememberAsyncImagePainter(storeFallbackUrl),
                            contentDescription = store.name,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(store.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val branchDisplayCount = if (storeBranches.isNotEmpty()) storeBranches.size else store.branchesCount
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.crimsonLink)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$branchDisplayCount Branches • ${storeProducts.size} Deals",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Active Store Flyers
        if (storeFlyers.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.featured_flyers), action = "", onAction = {})
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(storeFlyers, key = { it.id }) { flyer ->
                        FlyerSummaryCard(
                            flyer = flyer,
                            store = store,
                            onClick = { onNavigateToFlyer(flyer.id) }
                        )
                    }
                }
            }
        }

        // 4. Branches
        if (storeBranches.isNotEmpty()) {
            item { SectionHeader(title = "Branches", action = "", onAction = {}) }
            items(storeBranches, key = { it.id }) { branch ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(branch.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            if (branch.address.isNotBlank()) {
                                Text(branch.address, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            }
                            if (!branch.isEligibleForDistance()) {
                                Text("Coordinates pending verification", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            }
                        }
                        if (branch.isEligibleForDistance()) {
                            OutlinedButton(
                                onClick = {
                                    val label = Uri.encode("${store.name} - ${branch.name}")
                                    val uri = Uri.parse("geo:${branch.latitude},${branch.longitude}?q=${branch.latitude},${branch.longitude}($label)")
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No maps app installed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                border = BorderStroke(1.dp, palette.crimsonLink),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = "Directions", modifier = Modifier.size(16.dp), tint = palette.crimsonLink)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions", style = MaterialTheme.typography.labelSmall, color = palette.crimsonLink)
                            }
                        }
                    }
                }
            }
        }

        // 5. Store Products
        item { SectionHeader(title = "${stringResource(R.string.nav_deals)} • ${store.name}", action = "", onAction = {}) }

        if (storeProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.subtleSurface)
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_catalog_data), color = palette.textSecondary)
                }
            }
        } else {
            items(storeProducts, key = { it.id }) { product ->
                val (nearestBranch, distanceKm) = viewModel.getNearestBranchAndDistance(store.id)
                SearchResultItem(
                    product = product,
                    store = store,
                    isCheapest = true,
                    distanceKm = distanceKm,
                    nearestBranchName = nearestBranch?.name,
                    onClick = { onNavigateToProduct(product.id) },
                    onStoreClick = {},
                    onAddToList = {
                        onAddToList(product)
                        Toast.makeText(context, context.getString(R.string.added_to_list, product.name), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
