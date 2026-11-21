package com.example.ui.lists

import android.content.Intent
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.domain.basket.BasketComparisonResult
import com.example.domain.basket.Money
import com.example.domain.basket.Objective
import com.example.ui.DealFinderViewModel
import com.example.ui.components.BasketIllustration
import com.example.ui.theme.*

@Composable
fun ListScreen(
    viewModel: ListViewModel,
    dealViewModel: DealFinderViewModel,
    hasLoyaltyCardInWallet: Boolean = false,
    isSignedIn: Boolean = false,
    onBack: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCompare: () -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val userLists by viewModel.listNames.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    val products by dealViewModel.products.collectAsState()
    val stores by dealViewModel.stores.collectAsState()
    val selectedEmirate by dealViewModel.selectedEmirate.collectAsState()
    val unreadNotifCount by dealViewModel.unreadNotificationCount.collectAsState()
    val demoEnabled by DemoRegistry.isDemoEnabled.collectAsState()

    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }

    // Seed clean real Room sample list on first Demo visit if empty
    LaunchedEffect(isLoaded, demoEnabled, uiState.isEmpty()) {
        if (isLoaded && demoEnabled && uiState.isEmpty()) {
            val prefs = context.getSharedPreferences("demo_list_prefs", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("seeded_smart_list_v1", false)) {
                prefs.edit().putBoolean("seeded_smart_list_v1", true).apply()
                viewModel.seedDemoItemsIfEmpty()
            }
        }
    }

    var currentListId by remember { mutableStateOf("My Lists") }
    LaunchedEffect(userLists) {
        if (currentListId !in userLists && userLists.isNotEmpty()) {
            currentListId = userLists.first()
        }
    }

    var showNewListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var customItemName by remember { mutableStateOf("") }
    var customItemQty by remember { mutableStateOf("1") }

    // Shared optimization state between BudgetCard and BasketOptimizationEngine
    var objective by remember { mutableStateOf(Objective.LOWEST_PRICE) }
    var preferredStoreId by remember(stores) { mutableStateOf(stores.firstOrNull()?.id) }
    var hasLoyaltyCard by remember(hasLoyaltyCardInWallet) { mutableStateOf(hasLoyaltyCardInWallet) }

    val displayItems = remember(uiState, currentListId) {
        uiState.filter {
            it.listId == currentListId || (currentListId == "My Lists" && (it.listId.isBlank() || it.listId == "My Lists"))
        }
    }
    val selectedItems = remember(displayItems) { displayItems.filter { it.isSelected } }

    val basketResult = rememberComputedBasketComparisonResult(
        selectedItems = selectedItems,
        products = products,
        stores = stores,
        selectedEmirate = selectedEmirate,
        objective = objective,
        preferredStoreId = preferredStoreId,
        hasLoyaltyCard = hasLoyaltyCard,
        includeDemoOffers = demoEnabled
    )

    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            containerColor = palette.surfaceGradStart,
            title = { Text(stringResource(R.string.new_list), fontWeight = FontWeight.Bold, color = palette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newListName.trim()
                        if (clean.isNotBlank()) {
                            viewModel.createList(clean)
                            currentListId = clean
                        }
                        showNewListDialog = false
                        newListName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
                ) { Text("Create", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) { Text(stringResource(R.string.cancel), color = palette.textMuted) }
            }
        )
    }

    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            containerColor = palette.surfaceGradStart,
            title = { Text("Add Item to List", fontWeight = FontWeight.Bold, color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text(stringResource(R.string.add_item_placeholder)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = customItemQty,
                        onValueChange = { customItemQty = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Quantity") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = customItemName.trim()
                        if (cleanName.isNotBlank()) {
                            val matchedCatalog = products.firstOrNull { it.matchesSearchName(cleanName) }
                            viewModel.addItem(
                                ShoppingListItem(
                                    listId = currentListId,
                                    productId = matchedCatalog?.id ?: "",
                                    name = matchedCatalog?.name ?: cleanName,
                                    targetPrice = matchedCatalog?.currentPrice ?: 0.0,
                                    qty = customItemQty.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                    isSelected = true
                                )
                            )
                        }
                        customItemName = ""
                        customItemQty = "1"
                        showAddItemDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
                ) { Text(stringResource(R.string.add_btn), color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) { Text(stringResource(R.string.cancel), color = palette.textMuted) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ListHeader(
                locationName = localizedEmirate,
                hasUnreadNotifications = unreadNotifCount > 0,
                onBack = onBack,
                onNavigateToLocation = onNavigateToLocation,
                onNavigateToNotifications = onNavigateToNotifications
            )
        }

        if (!isSignedIn) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.crimsonPillBg)
                        .border(1.dp, palette.crimsonPillBorder, RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAuth() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.guest_signin_hint),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.crimsonLink,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sign_in),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.crimsonLink
                        )
                    }
                }
            }
        }

        if (syncError != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(syncError ?: "", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearSyncError() }) {
                            Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            ListTabs(
                userLists = userLists,
                currentListId = currentListId,
                onTabSelected = { currentListId = it },
                onAddNew = { showNewListDialog = true }
            )
        }

        item {
            BudgetCard(
                selectedCount = selectedItems.size,
                basketResult = basketResult,
                objective = objective
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentListId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${displayItems.size} items",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textMuted
                    )
                    IconButton(onClick = { showAddItemDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = stringResource(R.string.add_btn), tint = palette.crimsonLink)
                    }
                    var optionsExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { optionsExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = palette.textMuted)
                        }
                        DropdownMenu(
                            expanded = optionsExpanded,
                            onDismissRequest = { optionsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Item") },
                                onClick = {
                                    optionsExpanded = false
                                    showAddItemDialog = true
                                }
                            )
                            if (demoEnabled) {
                                DropdownMenuItem(
                                    text = { Text("Load Sample UAE Basket") },
                                    onClick = {
                                        optionsExpanded = false
                                        viewModel.seedDemoItemsIfEmpty()
                                    }
                                )
                            }
                            if (userLists.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Delete List", color = FigmaCrimsonEnd) },
                                    onClick = {
                                        optionsExpanded = false
                                        viewModel.removeList(currentListId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isLoaded) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FigmaCrimsonLink)
                }
            }
        } else if (displayItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1624203C), spotColor = Color(0x1624203C))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.ShoppingBasket, contentDescription = null, modifier = Modifier.size(44.dp), tint = palette.crimsonLink)
                        Text(stringResource(R.string.empty_list), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddItemDialog = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
                            ) {
                                Text(stringResource(R.string.add_btn), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            if (demoEnabled) {
                                OutlinedButton(
                                    onClick = { viewModel.seedDemoItemsIfEmpty() },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, palette.crimsonLink)
                                ) {
                                    Text("Sample List", color = palette.crimsonLink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(displayItems, key = { it.id }) { shopItem ->
                val matchedProduct = products.find { it.id == shopItem.productId }
                    ?: products.filter { it.matchesSearchName(shopItem.name) }.minByOrNull { it.currentPrice }
                val matchedStore = matchedProduct?.let { prod ->
                    stores.find { it.id == prod.retailerId } ?: dealViewModel.getStore(prod.retailerId)
                }

                ListItemView(
                    shopItem = shopItem,
                    product = matchedProduct,
                    store = matchedStore,
                    onClick = { matchedProduct?.let { onNavigateToProduct(it.id) } },
                    onToggleSelection = { viewModel.updateItem(shopItem.copy(isSelected = !shopItem.isSelected)) },
                    onRemove = { viewModel.removeItem(shopItem.id) },
                    onQtyChange = { newQty -> viewModel.updateItem(shopItem.copy(qty = newQty)) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                BasketOptimizationEngine(
                    items = displayItems,
                    stores = stores,
                    result = basketResult,
                    objective = objective,
                    onObjectiveChange = { objective = it },
                    preferredStoreId = preferredStoreId,
                    onPreferredStoreChange = { preferredStoreId = it },
                    hasLoyaltyCard = hasLoyaltyCard,
                    onToggleLoyaltyCard = { hasLoyaltyCard = !hasLoyaltyCard },
                    getNearestBranchAndDistance = { dealViewModel.getNearestBranchAndDistance(it) },
                    onOpenProduct = onNavigateToProduct
                )
            }
        }

        // Bottom Actions Row (cleanly embedded inside scrollable page above FigmaFloatingBottomBar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.crimsonPillBg)
                        .border(1.dp, palette.crimsonPillBorder, RoundedCornerShape(50))
                        .clickable {
                            if (displayItems.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.empty_list), Toast.LENGTH_SHORT).show()
                            } else {
                                val lines = displayItems.joinToString("\n") { item ->
                                    val resolvedPrice = products.find { it.id == item.productId }?.currentPrice
                                        ?: products.filter { it.matchesSearchName(item.name) }.minOfOrNull { it.currentPrice }
                                        ?: item.targetPrice
                                    val priceStr = if (resolvedPrice > 0.0) " - AED %.2f".format(resolvedPrice * item.qty) else ""
                                    "• ${item.name} (x${item.qty})$priceStr"
                                }
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Deal Finder UAE ($currentListId):\n$lines")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Unable to open share sheet", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = palette.crimsonLink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.share_list), color = palette.crimsonLink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                        .shadow(7.dp, RoundedCornerShape(50), ambientColor = Color(0x40ED0032), spotColor = Color(0x40ED0032))
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        .clickable { onNavigateToCompare() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BarChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.nav_search), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(
    locationName: String,
    hasUnreadNotifications: Boolean,
    onBack: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
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
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = palette.textPrimary, modifier = Modifier.size(21.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.smart_list_title),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary, platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.smart_list_subtitle),
                    style = TextStyle(fontSize = 10.5.sp, color = palette.textSecondary, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToLocation() }
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(locationName, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = palette.textPrimary, maxLines = 1)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.textPrimary)
                }
            }

            Box(modifier = Modifier.size(35.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable { onNavigateToNotifications() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications), modifier = Modifier.size(16.dp), tint = palette.textPrimary)
                }
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .size(6.5.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 1.dp)
                            .clip(CircleShape)
                            .background(FigmaCrimsonDot)
                    )
                }
            }
        }
    }
}

@Composable
fun ListTabs(userLists: List<String>, currentListId: String, onTabSelected: (String) -> Unit, onAddNew: () -> Unit) {
    val palette = LocalAppPalette.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(userLists) { tabName ->
            val isSelected = tabName == currentListId
            val icon = when (tabName) {
                "Grocery" -> Icons.Outlined.ShoppingCart
                "Ramadan" -> Icons.Outlined.Nightlight
                "Party" -> Icons.Outlined.Celebration
                else -> Icons.Outlined.ShoppingBasket
            }
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .shadow(if (isSelected) 6.dp else 3.dp, RoundedCornerShape(50), ambientColor = Color(0x2024203C), spotColor = Color(0x2024203C))
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                        } else {
                            Modifier
                                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(50))
                        }
                    )
                    .clickable { onTabSelected(tabName) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else palette.textPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tabName,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else palette.textPrimary
                    )
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.crimsonPillBg)
                    .border(1.dp, palette.crimsonPillBorder, RoundedCornerShape(50))
                    .clickable { onAddNew() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.crimsonLink)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.new_list), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = palette.crimsonLink)
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(
    selectedCount: Int,
    basketResult: BasketComparisonResult?,
    objective: Objective
) {
    val palette = LocalAppPalette.current
    val bestOpt = when (objective) {
        Objective.FEWEST_STORES -> basketResult?.bestOneStore ?: basketResult?.bestTwoStore
        Objective.PREFERRED_RETAILER -> basketResult?.preferredStoreOption ?: basketResult?.bestTwoStore ?: basketResult?.bestOneStore
        Objective.LOWEST_PRICE -> basketResult?.bestTwoStore ?: basketResult?.bestOneStore
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(7.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x1E24203C), spotColor = Color(0x1E24203C))
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (palette.isDark) {
                    Brush.horizontalGradient(listOf(Color(0xFF281722), Color(0xFF331B29)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFFEF4F8), Color(0xFFFDE6EF), Color(0xFFFBD5E3)))
                }
            )
            .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.total_basket_cost),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = when {
                        selectedCount == 0 -> "No items selected"
                        bestOpt?.totalFils != null -> Money.formatAed(bestOpt.totalFils)
                        bestOpt != null -> "Subtotal ${Money.formatAed(bestOpt.verifiedItemsSubtotalFils)}"
                        else -> "No matching offers yet"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (bestOpt != null) {
                        "${bestOpt.itemCoverage}/${bestOpt.itemCount} items • ${bestOpt.storeNames.joinToString(" + ")}"
                    } else {
                        stringResource(R.string.basket_optimizer_subtitle)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.crimsonLink,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            BasketIllustration(size = 54.dp)
        }
    }
}

@Composable
fun ListItemView(
    shopItem: ShoppingListItem,
    product: ProductEntity?,
    store: RetailerEntity?,
    onClick: () -> Unit = {},
    onToggleSelection: () -> Unit,
    onRemove: () -> Unit,
    onQtyChange: (Int) -> Unit
) {
    val palette = LocalAppPalette.current
    val currency = stringResource(R.string.currency_aed)
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(4.dp, cardShape, ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, cardShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (shopItem.isSelected) FigmaCrimsonEnd else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (shopItem.isSelected) FigmaCrimsonEnd else palette.textMuted,
                        shape = RoundedCornerShape(7.dp)
                    )
                    .clickable { onToggleSelection() },
                contentAlignment = Alignment.Center
            ) {
                if (shopItem.isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.subtleSurface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = product?.imageUrl?.ifBlank { "file:///android_asset/products/default_deal.jpg" }
                        ?: "file:///android_asset/products/default_deal.jpg",
                    contentDescription = shopItem.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(3.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shopItem.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.subtleSurface)
                            .clickable { if (shopItem.qty > 1) onQtyChange(shopItem.qty - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, color = palette.textPrimary, fontSize = 13.sp)
                    }
                    Text(
                        text = "${shopItem.qty}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.crimsonPillBg)
                            .clickable { onQtyChange(shopItem.qty + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, color = palette.crimsonLink, fontSize = 13.sp)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (store != null) Color(0xFFDCFCE7) else palette.subtleSurface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = store?.name?.substringBefore(" Hypermarket")?.substringBefore(" Supermarket") ?: "Unlinked",
                        color = if (store != null) Color(0xFF15803D) else palette.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                val unitPrice = product?.currentPrice
                if (unitPrice != null && unitPrice > 0.0) {
                    Text(
                        text = "$currency %.2f".format(unitPrice * shopItem.qty),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = palette.crimsonLink
                    )
                    if (product.oldPrice != null && product.oldPrice > unitPrice) {
                        Text(
                            text = "$currency %.2f".format(product.oldPrice * shopItem.qty),
                            fontSize = 10.sp,
                            color = palette.textMuted,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                } else {
                    Text("Unpriced", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = palette.textMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
