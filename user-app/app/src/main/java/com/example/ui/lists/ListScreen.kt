package com.example.ui.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ShoppingListItem
import com.example.data.isIdenticalTo
import com.example.data.matchesSearchName
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: ListViewModel,
    dealViewModel: com.example.ui.DealFinderViewModel,
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCompare: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val products by dealViewModel.products.collectAsState()
    val selectedEmirate by dealViewModel.selectedEmirate.collectAsState()
    
    var currentListId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("My Lists") }
    var userLists by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(listOf("My Lists")) }
    
    var showNewListDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newListName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    
    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("New List") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newListName.isNotBlank() && !userLists.contains(newListName)) {
                        userLists = userLists + newListName
                        currentListId = newListName
                    }
                    showNewListDialog = false
                    newListName = ""
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Also pick up any implicit lists from database that aren't in userLists
    val availableLists = (userLists + uiState.map { it.listId }).distinct()
    val displayItems = uiState.filter { it.listId == currentListId }
    val isLoaded by viewModel.isLoaded.collectAsState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val shareText = "My Shopping List ($currentListId):\n" + displayItems.joinToString("\n") { "• ${it.name} (x${it.qty}) - AED ${it.targetPrice}" }
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onNavigateToCompare,
                        modifier = Modifier.weight(1.4f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.CompareArrows, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compare Stores", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ListHeader(locationName = selectedEmirate, onNavigateToLocation = onNavigateToLocation, onNavigateToNotifications = onNavigateToNotifications) }
            item { ListTabs(availableLists, currentListId, onTabSelected = { currentListId = it }, onAddNew = { showNewListDialog = true }) }
            item { BudgetCard(displayItems = displayItems, products = products) }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentListId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${uiState.filter { it.listId == currentListId }.size} items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        var optionsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { optionsExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                            DropdownMenu(
                                expanded = optionsExpanded,
                                onDismissRequest = { optionsExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete List") },
                                    onClick = {
                                        viewModel.removeList(currentListId)
                                        userLists = userLists.filter { it != currentListId }
                                        currentListId = availableLists.firstOrNull { it != currentListId } ?: "My Lists"
                                        optionsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!isLoaded) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator()
                    }
                }
            } else if (displayItems.isEmpty()) {
                item {
                    Text(
                        "Your list is empty. Start adding items!",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(displayItems) { shopItem ->
                    val product = products.find { it.id == shopItem.productId }
                    ListItemView(
                        shopItem = shopItem,
                        product = product,
                        store = dealViewModel.getStore(shopItem.productId)?.let { null } ?: product?.let { dealViewModel.getStore(it.retailerId) },
                        onToggleSelection = { viewModel.updateItem(shopItem.copy(isSelected = !shopItem.isSelected)) },
                        onRemove = { viewModel.removeItem(shopItem.id) },
                        onQtyChange = { newQty -> viewModel.updateItem(shopItem.copy(qty = newQty)) }
                    )
                }
            }

            item {         BasketOptimizationEngine(
            items = displayItems, 
            products = products, 
            stores = dealViewModel.stores.collectAsState().value,
            getNearestBranchAndDistance = { dealViewModel.getNearestBranchAndDistance(it) }
        ) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun ListHeader(
    locationName: String,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Lists",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Plan, track and save on your shopping",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = onNavigateToLocation
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(locationName, style = MaterialTheme.typography.labelMedium)
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
                        modifier = Modifier.size(40.dp),
                        onClick = onNavigateToNotifications
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ListTabs(
    tabs: List<String>,
    currentListId: String,
    onTabSelected: (String) -> Unit,
    onAddNew: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tabs.size) { i ->
            val tabName = tabs[i]
            val isSelected = tabName == currentListId
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant),
                onClick = { onTabSelected(tabName) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ShoppingBasket,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        tabName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                onClick = onAddNew
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "New List",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetCard(
    displayItems: List<com.example.data.ShoppingListItem>,
    products: List<com.example.data.ProductEntity>
) {
    val originalCost = displayItems.sumOf { it.targetPrice * it.qty * 1.0 }

    val matchingProductsByName = displayItems.associate { item ->
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

    var optimalCost = 0.0
    displayItems.forEach { item ->
        val matches = matchingProductsByName[item.name] ?: emptyList()
        val optimalProduct = matches.minByOrNull { it.currentPrice }
        if (optimalProduct != null) {
            optimalCost += optimalProduct.currentPrice * item.qty
        } else {
            optimalCost += item.targetPrice * item.qty
        }
    }

    val savings = maxOf(0.0, originalCost - optimalCost)
    val budgetLimit = 500.00
    val progressRatio = if (budgetLimit > 0) (optimalCost / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
    val budgetLeft = maxOf(0.0, budgetLimit - optimalCost)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Monthly Budget Tracker", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AED ", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "500.00", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AED ${String.format("%.2f", optimalCost)} spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("AED ${String.format("%.2f", budgetLeft)} left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            VerticalDivider(modifier = Modifier.height(80.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                Text("Potential Savings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Text("AED ${String.format("%.2f", savings)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("with best store", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ListItemView(
    shopItem: ShoppingListItem,
    product: com.example.data.ProductEntity?,
    store: com.example.data.RetailerEntity?,
    onToggleSelection: () -> Unit,
    onRemove: () -> Unit,
    onQtyChange: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (shopItem.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                border = if (!shopItem.isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                modifier = Modifier.size(24.dp),
                onClick = onToggleSelection
            ) {
                if (shopItem.isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(3.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(56.dp)
            ) {
                AsyncImage(
                    model = product?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    shopItem.name, 
                    fontWeight = FontWeight.SemiBold, 
                    style = MaterialTheme.typography.bodyMedium, 
                    textDecoration = if (!shopItem.isSelected) TextDecoration.LineThrough else null,
                    color = if (shopItem.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Surface(
                        shape = CircleShape, 
                        color = MaterialTheme.colorScheme.surfaceVariant, 
                        modifier = Modifier.size(26.dp),
                        onClick = { if (shopItem.qty > 1) onQtyChange(shopItem.qty - 1) else onRemove() }
                    ) {
                        Icon(
                            if (shopItem.qty > 1) Icons.Default.Remove else Icons.Default.Delete, 
                            contentDescription = "Decrease", 
                            tint = MaterialTheme.colorScheme.onSurface, 
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                    Text(
                        "${shopItem.qty}", 
                        modifier = Modifier.padding(horizontal = 10.dp), 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = CircleShape, 
                        color = MaterialTheme.colorScheme.surfaceVariant, 
                        modifier = Modifier.size(26.dp),
                        onClick = { onQtyChange(shopItem.qty + 1) }
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Increase", 
                            tint = MaterialTheme.colorScheme.onSurface, 
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (store != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            store.name, 
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AED ", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        String.format("%.2f", shopItem.targetPrice * shopItem.qty), 
                        fontWeight = FontWeight.Bold, 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


