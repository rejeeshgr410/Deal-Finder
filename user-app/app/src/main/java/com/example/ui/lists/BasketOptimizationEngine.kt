package com.example.ui.lists

import com.example.data.matchesSearchName
import com.example.data.isIdenticalTo

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.data.ShoppingListItem
import com.example.data.RetailerEntity
import com.example.data.BranchEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BasketOptimizationEngine(
    items: List<ShoppingListItem>,
    products: List<ProductEntity>,
    stores: List<RetailerEntity>,
    getNearestBranchAndDistance: (String) -> Pair<BranchEntity?, Double?>
) {
    if (items.isEmpty()) return
    val selectedItems = items.filter { it.isSelected }
    if (selectedItems.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingBasket, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please select items in your list to optimize and find the best UAE deals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val originalCost = selectedItems.sumOf { it.targetPrice * it.qty * 1.0 }

    // Grouping by item.productId master match, fallback to matchesSearchName
    val matchingProductsByName = selectedItems.associate { item ->
        val refProduct = products.find { it.id == item.productId }
        val matches = if (refProduct != null) {
            products.filter { it.isIdenticalTo(refProduct) }
        } else {
            products.filter {
                (it.masterProductId.isNotBlank() && it.masterProductId == item.productId) ||
                it.matchesSearchName(item.name)
            }
        }
        item.name to matches
    }

    // Single store cost calculation
    var singleStoreMinCost = Double.MAX_VALUE
    var bestSingleStoreId = ""
    var bestSingleStoreMissingCount = Int.MAX_VALUE
    val bestSingleStoreMissingItems = mutableListOf<String>()
    
    // Multi store optimal cost calculation
    var multiStoreOptimalCost = 0.0
    val multiStorePlan = mutableMapOf<String, MutableList<ShoppingListItem>>() // retailerId to items
    val multiStoreMissingItems = mutableListOf<String>()

    // Resolve optimal multi-store
    selectedItems.forEach { item ->
        val matches = matchingProductsByName[item.name] ?: emptyList()
        val optimalProduct = matches.minByOrNull { it.currentPrice }
        
        if (optimalProduct != null) {
            multiStoreOptimalCost += optimalProduct.currentPrice * item.qty
            val list = multiStorePlan.getOrPut(optimalProduct.retailerId) { mutableListOf() }
            list.add(item)
        } else {
            multiStoreOptimalCost += item.targetPrice * item.qty
            multiStoreMissingItems.add(item.name)
        }
    }

    stores.forEach { store ->
        var storeCost = 0.0
        var missingCount = 0
        val currentMissingTemp = mutableListOf<String>()
        selectedItems.forEach { item ->
            val matches = matchingProductsByName[item.name] ?: emptyList()
            val productAtStore = matches.find { it.retailerId == store.id }
            if (productAtStore != null) {
                storeCost += productAtStore.currentPrice * item.qty
            } else {
                storeCost += item.targetPrice * item.qty
                missingCount++
                currentMissingTemp.add(item.name)
            }
        }
        
        // Prioritize: 1) Minimum unavailable count, 2) Minimum cost
        if (missingCount < bestSingleStoreMissingCount || (missingCount == bestSingleStoreMissingCount && storeCost < singleStoreMinCost)) {
            singleStoreMinCost = storeCost
            bestSingleStoreId = store.id
            bestSingleStoreMissingCount = missingCount
            bestSingleStoreMissingItems.clear()
            bestSingleStoreMissingItems.addAll(currentMissingTemp)
        }
    }

    val singleStoreEntity = stores.find { it.id == bestSingleStoreId }
    val bestSingleStoreBranchAndDist = getNearestBranchAndDistance(bestSingleStoreId)
    val singleStoreBranch = bestSingleStoreBranchAndDist.first
    val singleStoreDistance = bestSingleStoreBranchAndDist.second ?: 2.5

    val validMultiStores = multiStorePlan.keys.toList()
    val multiStoreBranches = validMultiStores.map { getNearestBranchAndDistance(it) }
    val multiStoreTotalDistance = if (multiStoreBranches.isNotEmpty() && multiStoreBranches.all { it.second != null }) {
        multiStoreBranches.sumOf { it.second!! }
    } else {
        singleStoreDistance + (validMultiStores.size * 1.5)
    }

    val singleStoreSavings = originalCost - singleStoreMinCost
    val multiStoreSavings = originalCost - multiStoreOptimalCost
    
    val extraMultiSavings = multiStoreSavings - singleStoreSavings
    
    // Under material 3 guidelines, recommendation mode compares driving/effort to absolute extra savings
    // If saving more than AED 15 by splitting, recommend Multi-Store, otherwise advise Single-Store convenience.
    val isMultiBetterRecommendation = extraMultiSavings >= 15.0 && validMultiStores.size > 1

    var selectedMode by remember { mutableStateOf("recommended") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = "Insights",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Basket Optimization Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OptimizationTab("Recommended", selectedMode == "recommended") { selectedMode = "recommended" }
                OptimizationTab("Single Store", selectedMode == "single") { selectedMode = "single" }
                OptimizationTab("Multi Store", selectedMode == "multi") { selectedMode = "multi" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(targetState = selectedMode, label = "modeAnimation") { mode ->
                when (mode) {
                    "single" -> {
                        val cheapestStoreName = singleStoreEntity?.name ?: "Unknown Retailer"
                        val branchString = if (singleStoreBranch != null) {
                            "${singleStoreBranch.name} (${String.format("%.1f", singleStoreDistance)} km away)"
                        } else {
                            "$cheapestStoreName (~${String.format("%.1f", singleStoreDistance)} km)"
                        }
                        
                        OptimizationResultView(
                            title = "Cheapest Single Store",
                            subtitle = "Shop everything at $cheapestStoreName for ultimate simplicity.",
                            totalCost = singleStoreMinCost,
                            originalCost = originalCost,
                            savings = singleStoreSavings,
                            savingsPct = if (originalCost > 0) ((singleStoreSavings / originalCost) * 100).roundToInt() else 0,
                            distanceStr = "Drive to nearest branch: $branchString",
                            planDetails = "All grocery list items routed to $cheapestStoreName. No multiple trips or extra parking charges.",
                            shoppingRoute = listOf(singleStoreBranch?.name ?: cheapestStoreName),
                            missingItems = bestSingleStoreMissingItems
                        )
                    }
                    "multi" -> {
                        val routeNames = multiStoreBranches.mapIndexed { index, pair -> 
                            pair.first?.name ?: (stores.find { it.id == validMultiStores.getOrNull(index) }?.name ?: "Store")
                        }
                        val storeSummary = multiStorePlan.entries.joinToString(", ") { (storeId, items) ->
                            val sName = stores.find { it.id == storeId }?.name ?: "Store"
                            "$sName (${items.size} items)"
                        }

                        OptimizationResultView(
                            title = "Optimal Multi-Store Basket",
                            subtitle = "Split items across ${validMultiStores.size} retailers for lowest price.",
                            totalCost = multiStoreOptimalCost,
                            originalCost = originalCost,
                            savings = multiStoreSavings,
                            savingsPct = if (originalCost > 0) ((multiStoreSavings / originalCost) * 100).roundToInt() else 0,
                            distanceStr = "Split route total: ~${String.format("%.1f", multiStoreTotalDistance)} km away",
                            planDetails = "Optimal split mapping: $storeSummary. Gives you the rock-bottom prices on each individual brand.",
                            shoppingRoute = routeNames,
                            missingItems = multiStoreMissingItems
                        )
                    }
                    "recommended" -> {
                        if (isMultiBetterRecommendation) {
                            val routeNames = multiStoreBranches.mapIndexed { index, pair -> 
                                pair.first?.name ?: (stores.find { it.id == validMultiStores.getOrNull(index) }?.name ?: "Store")
                            }
                            OptimizationResultView(
                                title = "Smart Recommendation",
                                subtitle = "Go with Multi-Store Split",
                                totalCost = multiStoreOptimalCost,
                                originalCost = originalCost,
                                savings = multiStoreSavings,
                                savingsPct = if (originalCost > 0) ((multiStoreSavings / originalCost) * 100).roundToInt() else 0,
                                distanceStr = "Splitting across ${validMultiStores.size} stores covers ${String.format("%.1f", multiStoreTotalDistance)} km",
                                planDetails = "Worth the drive! You save an extra AED ${String.format("%.2f", extraMultiSavings)} compared to shopping at a single retailer. Highly recommended for family baskets.",
                                shoppingRoute = routeNames,
                                missingItems = multiStoreMissingItems,
                                isRecommended = true
                            )
                        } else {
                            val cheapestStoreName = singleStoreEntity?.name ?: "Unknown Retailer"
                            val branchString = if (singleStoreBranch != null) {
                                "${singleStoreBranch.name} (~${String.format("%.1f", singleStoreDistance)} km)"
                            } else {
                                "$cheapestStoreName (~${String.format("%.1f", singleStoreDistance)} km)"
                            }
                            OptimizationResultView(
                                title = "Smart Recommendation",
                                subtitle = "Stick to $cheapestStoreName (Single Store)",
                                totalCost = singleStoreMinCost,
                                originalCost = originalCost,
                                savings = singleStoreSavings,
                                savingsPct = if (originalCost > 0) ((singleStoreSavings / originalCost) * 100).roundToInt() else 0,
                                distanceStr = "Nearest branch: $branchString",
                                planDetails = if (extraMultiSavings > 0) {
                                    "Highly Recommended. Splitting only saves an extra AED ${String.format("%.2f", extraMultiSavings)} which doesn't compensate for the extra petrol and driving time. Stay convenient!"
                                } else {
                                    "Save maximum time and money! Shopping entirely at $cheapestStoreName guarantees full convenience and the highest discount rate."
                                },
                                shoppingRoute = listOf(singleStoreBranch?.name ?: cheapestStoreName),
                                missingItems = bestSingleStoreMissingItems,
                                isRecommended = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.OptimizationTab(title: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OptimizationResultView(
    title: String,
    subtitle: String,
    totalCost: Double,
    originalCost: Double,
    savings: Double,
    savingsPct: Int,
    distanceStr: String,
    planDetails: String,
    shoppingRoute: List<String>,
    missingItems: List<String>,
    isRecommended: Boolean = false
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (isRecommended) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        "BEST VALUE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text("Optimized Basket Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "AED ${String.format("%.2f", totalCost)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Original: AED ${String.format("%.2f", originalCost)}",
                    style = MaterialTheme.typography.labelSmall,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            if (savings > 0) {
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Save ${savingsPct}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF235E25),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AED ${String.format("%.2f", savings)} Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF235E25),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Real branch and distance information
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Distance",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Real Branch & Distance Estimates",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        distanceStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (shoppingRoute.isNotEmpty()) {
            Text("Suggested Mall / Branch Route", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)
            ) {
                shoppingRoute.forEachIndexed { index, storeName ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (index < shoppingRoute.size - 1) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp).size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Convenience recommendation content text
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Recommend,
                        contentDescription = "Recommendation icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Convenience vs Savings Recommendation",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = planDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Unavailable items indicator
        if (missingItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Unpublished/Unavailable Here (${missingItems.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "These items are fallback/unmatched and estimated at target list price: ${missingItems.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
