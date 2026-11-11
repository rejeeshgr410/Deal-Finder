package com.example.ui.retailer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.example.ui.compare.SearchResultItem
import com.example.ui.home.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerScreen(
    retailerId: String,
    viewModel: com.example.ui.DealFinderViewModel,
    onBack: () -> Unit,
    onNavigateToProduct: (String) -> Unit
) {
    val isLoaded by viewModel.isLoaded.collectAsState()
    val store = viewModel.getStore(retailerId)

    if (!isLoaded) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Loading Retailer...") },
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

    if (store == null) {
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
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Retailer not found.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This retailer is currently ofline or unavailable in your chosen Emirate district context.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
        }
        return
    }

    val products by viewModel.products.collectAsState()
    val storeProducts = products.filter { it.retailerId == retailerId }
    val followedRetailers by viewModel.followedRetailers.collectAsState()
    val isFollowing = followedRetailers.any { it.id == store.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(store.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFollowedRetailer(store) }) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFollowing) "Unfollow Retailer" else "Follow Retailer",
                            tint = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val storeFallbackUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&w=200&q=80"
                    AsyncImage(
                        model = store.logoUrl.ifBlank { storeFallbackUrl },
                        error = coil.compose.rememberAsyncImagePainter(storeFallbackUrl),
                        contentDescription = store.name,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(store.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        val branchCount = store.branchesCount
                        Text("$branchCount Branches nearby", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                SectionHeader(title = "Top Deals at ${store.name}", action = "", onAction = {})
            }

            items(storeProducts) { product ->
                SearchResultItem(
                    product = product,
                    store = store,
                    isCheapest = true,
                    onClick = { onNavigateToProduct(product.id) },
                    onAddToList = {}
                )
            }
        }
    }
}
