package com.example.ui.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BranchEntity
import com.example.data.ProductEntity
import com.example.data.RetailerEntity
import com.example.data.ShoppingListItem
import com.example.domain.basket.*
import com.example.ui.theme.*

@Composable
fun rememberComputedBasketComparisonResult(
    selectedItems: List<ShoppingListItem>,
    products: List<ProductEntity>,
    stores: List<RetailerEntity>,
    selectedEmirate: String,
    objective: Objective,
    preferredStoreId: String?,
    hasLoyaltyCard: Boolean,
    includeDemoOffers: Boolean
): BasketComparisonResult? {
    val requestedItems = remember(selectedItems, products) {
        val byId = products.associateBy { it.id }
        selectedItems.map { item ->
            val ref = byId[item.productId]
            val unit = ref?.let { ProductIdentityMatcher.extractCanonicalUnit(it) }
            RequestedItem(
                lineId = item.id,
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

    return remember(requestedItems, products, stores, selectedEmirate, objective, preferredStoreId, hasLoyaltyCard, includeDemoOffers) {
        if (requestedItems.isEmpty()) null
        else BasketOptimizer.optimize(
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
}

@Composable
fun BasketOptimizationEngine(
    items: List<ShoppingListItem>,
    stores: List<RetailerEntity>,
    result: BasketComparisonResult?,
    objective: Objective,
    onObjectiveChange: (Objective) -> Unit,
    preferredStoreId: String?,
    onPreferredStoreChange: (String) -> Unit,
    hasLoyaltyCard: Boolean,
    onToggleLoyaltyCard: () -> Unit,
    getNearestBranchAndDistance: (String) -> Pair<BranchEntity?, Double?>,
    onOpenProduct: (String) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    if (items.isEmpty()) return
    val selectedItems = items.filter { it.isSelected }

    if (selectedItems.isEmpty() || result == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ShoppingBasket,
                    contentDescription = null,
                    tint = palette.crimsonLink.copy(alpha = 0.8f),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.basket_optimizer_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var showWhyPanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.basket_optimizer_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Objective.LOWEST_PRICE to stringResource(R.string.sort_cheapest),
                Objective.FEWEST_STORES to "Fewest Stops",
                Objective.PREFERRED_RETAILER to "Prefer Retailer"
            ).forEach { (obj, label) ->
                FilterChip(
                    selected = objective == obj,
                    onClick = { onObjectiveChange(obj) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.crimsonPillBg,
                        selectedLabelColor = palette.crimsonLink
                    )
                )
            }
            FilterChip(
                selected = hasLoyaltyCard,
                onClick = onToggleLoyaltyCard,
                label = { Text(stringResource(R.string.nav_loyalty), style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.crimsonPillBg,
                    selectedLabelColor = palette.crimsonLink
                )
            )
        }

        if (objective == Objective.PREFERRED_RETAILER && stores.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stores.forEach { s ->
                    FilterChip(
                        selected = preferredStoreId == s.id,
                        onClick = { onPreferredStoreChange(s.id) },
                        label = { Text(s.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.crimsonPillBg,
                            selectedLabelColor = palette.crimsonLink
                        )
                    )
                }
            }
        }

        // Coverage & Recommendation Disclosure
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.subtleSurface)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = result.recommendationSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
                Text(
                    text = result.coverageDisclosure,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }

        result.bestOneStore?.let { oneOpt ->
            BasketOptionCard(
                title = stringResource(R.string.cheapest_single_store),
                option = oneOpt,
                getNearestBranchAndDistance = getNearestBranchAndDistance,
                onOpenProduct = onOpenProduct
            )
        }

        result.bestTwoStore?.let { twoOpt ->
            BasketOptionCard(
                title = stringResource(R.string.split_store_savings),
                option = twoOpt,
                getNearestBranchAndDistance = getNearestBranchAndDistance,
                onOpenProduct = onOpenProduct
            )
        }

        if (objective == Objective.PREFERRED_RETAILER) {
            result.preferredStoreOption?.let { prefOpt ->
                BasketOptionCard(
                    title = "Preferred Store",
                    option = prefOpt,
                    getNearestBranchAndDistance = getNearestBranchAndDistance,
                    onOpenProduct = onOpenProduct
                )
            }
        }

        val drivers = remember(result) {
            BasketExplanations.explainDrivers(result.bestOneStore, result.bestTwoStore)
        }
        if (drivers.isNotEmpty()) {
            TextButton(onClick = { showWhyPanel = !showWhyPanel }) {
                Text(
                    text = if (showWhyPanel) "Hide price difference details" else "Why this option? (${drivers.size} item difference(s))",
                    color = palette.crimsonLink,
                    fontWeight = FontWeight.Bold
                )
            }
            if (showWhyPanel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        drivers.forEach { d ->
                            Text("• $d", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
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
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = Color(0x1624203C), spotColor = Color(0x1624203C))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, if (option.complete) Color(0xFF4ADE80) else Color(0xFFFBBF24), shape)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelMedium, color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                    Text(
                        text = option.storeNames.joinToString(" + "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = option.totalFils?.let { Money.formatAed(it) }
                            ?: "Subtotal ${Money.formatAed(option.verifiedItemsSubtotalFils)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (option.complete) FigmaDiscountGreen else Color(0xFFD97706)
                    )
                    Text(
                        text = "${option.itemCoverage} of ${option.itemCount} items priced",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                }
            }

            val distSummary = option.storeIds.mapIndexed { idx, sid ->
                val (_, dist) = getNearestBranchAndDistance(sid)
                val sName = option.storeNames.getOrElse(idx) { sid }
                if (dist != null) "$sName: ${"%.1f".format(dist)} km" else "$sName: UAE"
            }.joinToString(" • ")
            Text(distSummary, style = MaterialTheme.typography.labelSmall, color = palette.textMuted)

            option.unknownFeeDisclosure?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
            }
            if (option.comparisonDeltaVsBestSingleStoreFils != null) {
                Text(
                    text = option.savingsBaselineDisclosure,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = FigmaDiscountGreen
                )
            }

            option.pricedLines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenProduct(line.product.id) }
                        .padding(vertical = 3.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${line.requestedQuantity}× ${line.product.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "${line.storeName}${line.unitPriceText?.let { " • $it" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = palette.textMuted
                        )
                    }
                    Text(
                        text = Money.formatAed(line.lineSubtotalFils),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
            }

            if (option.unpricedLines.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${stringResource(R.string.missing_items)} (${option.unpricedLines.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = option.unpricedLines.joinToString(", ") { it.itemName },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            option.explanatoryWarnings.forEach { warn ->
                Text(warn, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = palette.textMuted)
            }
        }
    }
}
