package com.example.ui.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BranchEntity
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.data.ShoppingListItem
import com.example.domain.basket.*
import com.example.ui.theme.*

@Composable
fun BasketOptimizationEngine(
    items: List<ShoppingListItem>,
    products: List<ProductEntity>,
    stores: List<RetailerEntity>,
    getNearestBranchAndDistance: (String) -> Pair<BranchEntity?, Double?>,
    selectedEmirate: String = "Dubai",
    includeDemoOffers: Boolean = false,
    onOpenProduct: (String) -> Unit = {}
) {
    if (items.isEmpty()) return
    val selectedItems = items.filter { it.isSelected }

    if (selectedItems.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingBasket,
                        contentDescription = "Select basket items",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Select items in your list to compare 1-store vs 2-store UAE prices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    var objective by remember { mutableStateOf(Objective.LOWEST_PRICE) }
    var preferredStoreId by remember(stores) { mutableStateOf(stores.firstOrNull()?.id) }
    var hasLoyaltyCard by remember { mutableStateOf(false) }
    var showWhyPanel by remember { mutableStateOf(false) }

    val requestedItems = remember(selectedItems, products) {
        val byId = products.associateBy { it.id }
        selectedItems.map { item ->
            val ref = byId[item.productId]
            val unit = ref?.let { ProductIdentityMatcher.extractCanonicalUnit(it) }
            RequestedItem(
                lineId = item.id, // Unique line ID; never keyed by name
                name = item.name,
                identity = ProductIdentity(
                    masterProductId = ref?.masterProductId ?: "",
                    barcodeGtin = ref?.barcode ?: "",
                    brand = ref?.brand ?: "",
                    name = ref?.name ?: item.name,
                    baseQuantity = unit?.baseQuantity,
                    baseUnit = unit?.baseUnit,
                    packCount = unit?.packCount ?: 1
                ),
                requestedQuantity = item.qty.coerceAtLeast(1),
                referenceProductId = item.productId
            )
        }
    }

    val result = remember(requestedItems, products, stores, selectedEmirate, objective, preferredStoreId, hasLoyaltyCard, includeDemoOffers) {
        BasketOptimizer.optimize(
            request = BasketRequest(
                items = requestedItems,
                selectedEmirate = selectedEmirate,
                objective = objective,
                maxStores = 2,
                preferredRetailerId = if (objective == Objective.PREFERRED_RETAILER) preferredStoreId else null,
                hasUserLoyaltyCard = hasLoyaltyCard,
                includeDemoOffers = includeDemoOffers
            ),
            products = products,
            stores = stores
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Preferences Row
        Text("Compare Basket Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Objective.LOWEST_PRICE to "Lowest Listed Cost",
                Objective.FEWEST_STORES to "Fewest Stops",
                Objective.PREFERRED_RETAILER to "Prefer Retailer"
            ).forEach { (obj, label) ->
                FilterChip(
                    selected = objective == obj,
                    onClick = { objective = obj },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
            FilterChip(
                selected = hasLoyaltyCard,
                onClick = { hasLoyaltyCard = !hasLoyaltyCard },
                label = { Text("Loyalty Cards", style = MaterialTheme.typography.labelMedium) }
            )
        }

        if (objective == Objective.PREFERRED_RETAILER && stores.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stores.take(8).forEach { s ->
                    FilterChip(
                        selected = preferredStoreId == s.id,
                        onClick = { preferredStoreId = s.id },
                        label = { Text(s.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Coverage & Recommendation Disclosure
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    result.recommendationSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                )
                Text(
                    result.coverageDisclosure,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }
        }

        // One-Store Option Card
        result.bestOneStore?.let { oneOpt ->
            BasketOptionCard(
                title = "1-Store Option",
                option = oneOpt,
                getNearestBranchAndDistance = getNearestBranchAndDistance,
                onOpenProduct = onOpenProduct
            )
        }

        // Two-Store Option Card
        result.bestTwoStore?.let { twoOpt ->
            BasketOptionCard(
                title = "2-Store Split Option",
                option = twoOpt,
                getNearestBranchAndDistance = getNearestBranchAndDistance,
                onOpenProduct = onOpenProduct
            )
        }

        // Preferred Store Card (if selected)
        if (objective == Objective.PREFERRED_RETAILER) {
            result.preferredStoreOption?.let { prefOpt ->
                BasketOptionCard(
                    title = "Preferred Retailer",
                    option = prefOpt,
                    getNearestBranchAndDistance = getNearestBranchAndDistance,
                    onOpenProduct = onOpenProduct
                )
            }
        }

        // "Why this option?" expandable breakdown
        val drivers = remember(result) {
            BasketExplanations.explainDrivers(result.bestOneStore, result.bestTwoStore)
        }
        if (drivers.isNotEmpty()) {
            TextButton(onClick = { showWhyPanel = !showWhyPanel }) {
                Text(if (showWhyPanel) "Hide price difference details" else "Why this option? (${drivers.size} item difference(s))")
            }
            if (showWhyPanel) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        drivers.forEach { d ->
                            Text("• $d", style = MaterialTheme.typography.bodySmall, color = Slate700)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BasketOptionCard(
    title: String,
    option: BasketOption,
    getNearestBranchAndDistance: (String) -> Pair<BranchEntity?, Double?>,
    onOpenProduct: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (option.complete) Color(0xFFBBF7D0) else Color(0xFFFDE68A))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelMedium, color = Slate500, fontWeight = FontWeight.Bold)
                    Text(
                        option.storeNames.joinToString(" + "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = option.totalFils?.let { Money.formatAed(it) }
                            ?: "Subtotal ${Money.formatAed(option.verifiedItemsSubtotalFils)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (option.complete) Color(0xFF15803D) else Color(0xFFB45309)
                    )
                    Text(
                        "${option.itemCoverage} of ${option.itemCount} items priced",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                }
            }

            // Honest Distance Disclosure (no fabricated 1.2 km / 2.5 km fallback)
            val distSummary = option.storeIds.mapIndexed { idx, sid ->
                val (_, dist) = getNearestBranchAndDistance(sid)
                val sName = option.storeNames.getOrElse(idx) { sid }
                if (dist != null) "$sName: ${"%.1f".format(dist)} km straight-line" else "$sName: Distance unavailable"
            }.joinToString(" • ")
            Text(distSummary, style = MaterialTheme.typography.labelSmall, color = Slate500)

            // Savings / Fee disclosure
            option.unknownFeeDisclosure?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
            if (option.comparisonDeltaVsBestSingleStoreFils != null) {
                Text(
                    option.savingsBaselineDisclosure,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF15803D)
                )
            }

            // Priced lines with tap-to-source
            option.pricedLines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProduct(line.product.id) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${line.requestedQuantity}× ${line.product.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate900
                        )
                        Text(
                            "${line.storeName} • ${line.verificationLabel}${line.unitPriceText?.let { " • $it" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Slate400
                        )
                    }
                    Text(
                        Money.formatAed(line.lineSubtotalFils),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            // Unpriced warnings
            if (option.unpricedLines.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "Unpriced / Missing Offers (${option.unpricedLines.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            "Full basket total unavailable. Missing: ${option.unpricedLines.joinToString(", ") { it.itemName }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            option.explanatoryWarnings.firstOrNull()?.let { warn ->
                Text(warn, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Slate400)
            }
        }
    }
}
