package com.example.ui.flyers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
                    text = "Deals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Explore the best offers near you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dubai", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                BadgedBox(
                    badge = { Badge(modifier = Modifier.offset(x = (-6).dp, y = 6.dp), containerColor = Color(0xFFE53935)) { } }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

data class DealCategory(val name: String, val icon: ImageVector, val selected: Boolean = false)

@Composable
fun DealsCategories() {
    val cats = listOf(
        DealCategory("All Deals", Icons.Outlined.LocalOffer, true),
        DealCategory("Supermarkets", Icons.Outlined.ShoppingCart),
        DealCategory("Electronics", Icons.Outlined.Computer),
        DealCategory("Pharmacy", Icons.Outlined.LocalHospital),
        DealCategory("Home & Kitchen", Icons.Outlined.Home),
        DealCategory("Beauty", Icons.Outlined.Face) // added purely as extra content
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(cats) { cat ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (cat.selected) GoldBackground else MaterialTheme.colorScheme.surface,
                    border = if (!cat.selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        cat.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = if (cat.selected) Color.Black else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(cat.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun FlashDealsBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF8E1)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(48.dp)) {
                     Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color(0xFF5C6BC0))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text("Flash Deals", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                         Spacer(modifier = Modifier.width(4.dp))
                         Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    }
                    Text("Limited time offers on", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("your favorite products", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TimeBox("02", "Hours")
                TimeBox("45", "Mins")
                TimeBox("18", "Secs")
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = GoldBackground,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(8.dp))
                }
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
                model = product.imageUrl,
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
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = Color(0xFFF3E5F5)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF7E57C2), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Never Miss a Deal!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Turn on alerts and get notified about the best offers & price drops.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            var alertsEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            Switch(
                checked = alertsEnabled,
                onCheckedChange = { alertsEnabled = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF7E57C2))
            )
        }
    }
}
