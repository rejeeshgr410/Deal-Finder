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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
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
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightPeriwinkle),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, tint = Purple40)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share", color = Purple40, style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = onNavigateToCompare,
                    modifier = Modifier.weight(1.3f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.CompareArrows, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compare Stores", color = Color.White, style = MaterialTheme.typography.titleMedium)
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
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) GoldBackground else MaterialTheme.colorScheme.surface,
                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                onClick = { onTabSelected(tabName) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ShoppingBasket,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color.Black else Teal500
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tabName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LightPeriwinkle,
                onClick = onAddNew
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Purple40
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "New List",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Purple40
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
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF3E5F5)
    ) {
        Row(
            modifier = Modifier.padding(24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Monthly Budget limit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("AED", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("500.00", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp).padding(bottom = 4.dp), tint = Color(0xFF7E57C2))
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF7E57C2),
                    trackColor = Color(0xFFD1C4E9)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AED ${String.format("%.2f", optimalCost)} spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("AED ${String.format("%.2f", budgetLeft)} left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Divider(modifier = Modifier.height(100.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("You'll Save", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = "Savings info badge", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                }
                Text("AED ${String.format("%.2f", savings)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Teal500)
                Text("with best prices", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                // Box acting as an illustration placeholder
                Box(modifier = Modifier.size(60.dp).background(Color(0xFFB0A4F6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ShoppingBasket, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (shopItem.isSelected) Purple40 else Color.Transparent,
            border = if (!shopItem.isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            modifier = Modifier.size(28.dp),
            onClick = onToggleSelection
        ) {
            if (shopItem.isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        AsyncImage(
            model = product?.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(shopItem.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, textDecoration = if (!shopItem.isSelected) TextDecoration.LineThrough else null)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Surface(
                    shape = CircleShape, color = LightPeriwinkle, modifier = Modifier.size(24.dp),
                    onClick = { if (shopItem.qty > 1) onQtyChange(shopItem.qty - 1) else onRemove() }
                ) {
                    Icon(if (shopItem.qty > 1) Icons.Default.Remove else Icons.Default.Delete, contentDescription = null, tint = Purple40, modifier = Modifier.padding(4.dp))
                }
                Text("${shopItem.qty}", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
                Surface(
                    shape = CircleShape, color = LightPeriwinkle, modifier = Modifier.size(24.dp),
                    onClick = { onQtyChange(shopItem.qty + 1) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Purple40, modifier = Modifier.padding(4.dp))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                color = CatGreen,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(store?.name ?: "", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Teal500, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AED ${String.format("%.2f", shopItem.targetPrice * shopItem.qty)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


