package com.example.ui.product

import com.example.data.isIdenticalTo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            val fallbackUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80"
            AsyncImage(
                model = product.imageUrl.ifBlank { fallbackUrl },
                error = coil.compose.rememberAsyncImagePainter(fallbackUrl),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AED ${"%.2f".format(product.currentPrice)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.oldPrice != null) {
                        Text(
                            text = "AED ${"%.2f".format(product.oldPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    onClick = { onNavigateToRetailer(store.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val storeFallbackUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&w=200&q=80"
                        AsyncImage(
                            model = store.logoUrl.ifBlank { storeFallbackUrl },
                            error = coil.compose.rememberAsyncImagePainter(storeFallbackUrl),
                            contentDescription = store.name,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Available at", style = MaterialTheme.typography.labelMedium)
                            Text(store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddToList,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Add to List")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader(title = "Price Comparison", action = "", onAction = {})
                
                val similarProducts = products.filter {
                    it.isIdenticalTo(product)
                }.sortedBy { it.currentPrice }
                val minPrice = similarProducts.firstOrNull()?.currentPrice ?: Double.MAX_VALUE
                similarProducts.forEach { simProduct ->
                    val (nearestBranch, distanceKm) = viewModel.getNearestBranchAndDistance(simProduct.retailerId)
                    SearchResultItem(
                        product = simProduct,
                        store = viewModel.getStore(simProduct.retailerId),
                        isCheapest = simProduct.currentPrice <= minPrice,
                        distanceKm = distanceKm,
                        nearestBranchName = nearestBranch?.name,
                        onClick = { onNavigateToProduct(simProduct.id) },
                        onStoreClick = { simProduct.retailerId?.let { onNavigateToRetailer(it) } },
                        onAddToList = onAddToList
                    )
                }
            }
        }
    }
}
