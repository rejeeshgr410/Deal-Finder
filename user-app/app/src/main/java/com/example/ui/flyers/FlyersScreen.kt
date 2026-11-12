package com.example.ui.flyers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.ui.home.ProductCard
import com.example.ui.home.SectionHeader
import com.example.ui.theme.GoldBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyersScreen(
    viewModel: com.example.ui.DealFinderViewModel,
    onNavigateToFlyer: (String) -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {}
) {
    val products by viewModel.products.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { DealsHeader() }
            item { DealsCategories() }
            
            item {
                val flyers by viewModel.flyers.collectAsState()
                val stores by viewModel.stores.collectAsState()
                val isLoaded by viewModel.isLoaded.collectAsState()
                
                SectionHeader(title = "Promotional Flyers", action = "", onAction = { })
                if (!isLoaded) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator()
                    }
                } else if (flyers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("No flyers available") }
                } else {
                    PromotionalFlyersGrid(
                        flyers = flyers,
                        stores = stores,
                        onFlyerClick = { onNavigateToFlyer(it.id) }
                    )
                }
            }
            
            item {
                val isLoaded by viewModel.isLoaded.collectAsState()
                SectionHeader(title = "Top Deals Today", action = "", onAction = { })
                
                if (!isLoaded) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator()
                    }
                } else if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("No deals available") }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(products) { product -> ProductCard(product, viewModel.getStore(product.retailerId), onClick = { onNavigateToProduct(product.id) }) }
                    }
                }
            }
            
            item { FlashDealsBanner() }
            
            item {
                SectionHeader(title = "Deals Near You", subtitle = "View on map", action = "", onAction = {})
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    products.take(3).forEach { product ->
                        DealListItem(product, viewModel.getStore(product.retailerId), onClick = { onNavigateToProduct(product.id) })
                    }
                }
            }
            
            item { NeverMissDealBanner() }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DealsHeader() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Flyers & Catalogues",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Browse active promotions across UAE hypermarkets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class DealCategory(val name: String, val icon: ImageVector, val selected: Boolean = false)

@Composable
fun DealsCategories() {
    var selectedCat by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All Deals") }
    val cats = listOf(
        DealCategory("All Deals", Icons.Outlined.LocalOffer),
        DealCategory("Supermarkets", Icons.Outlined.ShoppingCart),
        DealCategory("Electronics", Icons.Outlined.Computer),
        DealCategory("Pharmacy", Icons.Outlined.LocalHospital),
        DealCategory("Home & Living", Icons.Outlined.Home),
        DealCategory("Personal Care", Icons.Outlined.Face)
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cats) { cat ->
            val isSelected = cat.name == selectedCat
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { selectedCat = cat.name }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        cat.icon,
                        contentDescription = cat.name,
                        modifier = Modifier.padding(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    cat.name, 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FlashDealsBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(12.dp), 
                    color = MaterialTheme.colorScheme.primaryContainer, 
                    modifier = Modifier.size(44.dp)
                ) {
                     Icon(
                         Icons.Outlined.Timer, 
                         contentDescription = null, 
                         modifier = Modifier.padding(10.dp), 
                         tint = MaterialTheme.colorScheme.primary
                     )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text("Limited-Time Offers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                         Spacer(modifier = Modifier.width(4.dp))
                         Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    }
                    Text("Daily deals ending tonight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TimeBox("02", "Hours")
                TimeBox("45", "Mins")
                TimeBox("18", "Secs")
            }
        }
    }
}

@Composable
fun TimeBox(time: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(time, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealListItem(product: ProductEntity, store: RetailerEntity? = null, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box {
            AsyncImage(
                model = product.imageUrl.ifBlank { "file:///android_asset/products/default_deal.jpg" },
                error = coil.compose.rememberAsyncImagePainter("file:///android_asset/products/default_deal.jpg"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp)
            ) {
                Text(
                    "-20%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(product.brand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AED ${product.currentPrice}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                if (product.oldPrice != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "AED ${product.oldPrice}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(store?.name ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("500 m away", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
}

@Composable
fun NeverMissDealBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.NotificationsActive, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Deal & Catalogue Alerts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Get notified when new weekly flyers drop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            var alertsEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            Switch(
                checked = alertsEnabled,
                onCheckedChange = { alertsEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
