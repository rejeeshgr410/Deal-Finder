package com.example.ui.flyers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FlyerEntity
import com.example.data.RetailerEntity
import com.example.ui.theme.LocalAppPalette
import java.util.concurrent.TimeUnit

@Composable
fun PromotionalFlyersGrid(
    flyers: List<FlyerEntity>,
    stores: List<RetailerEntity>,
    onFlyerClick: (FlyerEntity) -> Unit
) {
    val palette = LocalAppPalette.current
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
            placeholder = { Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = palette.textMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.crimsonLink,
                unfocusedBorderColor = palette.cardBorder,
                focusedContainerColor = palette.surfaceGradStart,
                unfocusedContainerColor = palette.surfaceGradStart
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredFlyers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_search_results), color = palette.textMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.heightIn(max = 600.dp)
            ) {
                items(filteredFlyers, key = { it.id }) { flyer ->
                    val store = stores.find { it.id == flyer.retailerId }
                    FlyerGridCard(flyer = flyer, store = store, onClick = { onFlyerClick(flyer) })
                }
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
    val palette = LocalAppPalette.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, palette.cardBorder),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceGradStart)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = flyer.imageUrl.ifEmpty { store?.logoUrl ?: "file:///android_asset/flyers/default_flyer.jpg" },
                    error = coil.compose.rememberAsyncImagePainter("file:///android_asset/flyers/default_flyer.jpg"),
                    contentDescription = flyer.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                if (flyer.pages > 0) {
                    Surface(
                        color = palette.crimsonPillBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    ) {
                        Text(
                            "${flyer.pages} pgs",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = palette.crimsonLink,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = flyer.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = store?.name ?: "Verified Retailer",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.crimsonLink,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                val daysLeft = if (flyer.validTo > 0) {
                    TimeUnit.MILLISECONDS.toDays(flyer.validTo - System.currentTimeMillis()).toInt()
                } else null
                val validityText = when {
                    daysLeft == null -> "Active promotion"
                    daysLeft < 0 -> "Expired"
                    daysLeft == 0 -> "Ends today"
                    daysLeft == 1 -> stringResource(R.string.day_left, 1)
                    else -> stringResource(R.string.days_left, daysLeft)
                }
                Text(
                    text = validityText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (daysLeft != null && daysLeft <= 1) palette.crimsonLink else palette.textMuted,
                    fontWeight = if (daysLeft != null && daysLeft <= 1) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
