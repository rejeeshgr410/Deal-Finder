package com.example.ui.flyers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FlyerEntity
import com.example.data.RetailerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionalFlyersGrid(
    flyers: List<FlyerEntity>,
    stores: List<RetailerEntity>,
    onFlyerClick: (FlyerEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredFlyers = flyers.filter { flyer ->
        val storeName = stores.find { it.id == flyer.retailerId }?.name ?: ""
        storeName.contains(searchQuery, ignoreCase = true) || flyer.title.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Filter flyers by hypermarket or title…", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.heightIn(max = 600.dp)
        ) {
            items(filteredFlyers) { flyer ->
                val store = stores.find { it.id == flyer.retailerId }
                FlyerGridCard(flyer = flyer, store = store, onClick = { onFlyerClick(flyer) })
            }
        }
    }
}

@Composable
fun FlyerGridCard(
    flyer: FlyerEntity,
    store: RetailerEntity?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = flyer.imageUrl.ifEmpty { "https://picsum.photos/300/400" },
                    contentDescription = flyer.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.78f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                if (flyer.pages > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    ) {
                        Text(
                            "${flyer.pages} pgs",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = flyer.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = store?.name ?: "Verified Retailer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                val daysLeft = if (flyer.validTo > 0) java.util.concurrent.TimeUnit.MILLISECONDS.toDays(flyer.validTo - System.currentTimeMillis()).toInt() else null
                val validityText = when {
                    daysLeft != null && daysLeft > 1 -> "$daysLeft days left"
                    daysLeft != null && daysLeft == 1 -> "Ends tomorrow"
                    daysLeft != null && daysLeft == 0 -> "Ends today"
                    else -> "Active promotion"
                }
                Text(
                    text = validityText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (daysLeft != null && daysLeft <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (daysLeft != null && daysLeft <= 1) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
