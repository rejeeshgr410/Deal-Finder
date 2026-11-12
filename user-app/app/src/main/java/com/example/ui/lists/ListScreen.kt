package com.example.ui.lists

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.BasketIllustration
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
    
    var currentListId by remember { mutableStateOf("My Lists") }
    val defaultTabs = listOf("My Lists", "Grocery", "Ramadan", "Party")
    var userLists by remember { mutableStateOf(defaultTabs) }
    
    var showNewListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    
    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("New Shopping List", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List Name") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListName.isNotBlank() && !userLists.contains(newListName)) {
                            userLists = userLists + newListName
                            currentListId = newListName
                        }
                        showNewListDialog = false
                        newListName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MockupYellowPill)
                ) { Text("Create", color = Slate900, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNewListDialog = false }) { Text("Cancel", color = Slate500) }
            }
        )
    }

    val displayItems = uiState.filter { it.listId == currentListId || (currentListId == "My Lists" && (it.listId.isBlank() || it.listId == "My Lists")) }
    val isLoaded by viewModel.isLoaded.collectAsState()
    
    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3E8FF))
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share List", color = Color(0xFF6D28D9), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToCompare,
                        modifier = Modifier.weight(1.3f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MockupYellowPill),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Outlined.BarChart, contentDescription = null, tint = Slate900, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compare Basket Prices", color = Slate900, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
            item { ListTabs(userLists, currentListId, onTabSelected = { currentListId = it }, onAddNew = { showNewListDialog = true }) }
            item { BudgetCard(displayItems = displayItems, products = products) }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${if (displayItems.isNotEmpty()) displayItems.size else 8} items", 
                            style = MaterialTheme.typography.labelMedium, 
                            color = Slate400
                        )
                        var optionsExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { optionsExpanded = true }) { 
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Slate400) 
                            }
                            DropdownMenu(
                                expanded = optionsExpanded,
                                onDismissRequest = { optionsExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear Completed") },
                                    onClick = {
                                        optionsExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete List") },
                                    onClick = {
                                        viewModel.removeList(currentListId)
                                        userLists = userLists.filter { it != currentListId }
                                        currentListId = userLists.firstOrNull() ?: "My Lists"
                                        optionsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (displayItems.isEmpty()) {
                val demoItems = listOf(
                    DemoListItem("Basmati Rice 5kg", "1 x 5kg", "Nesto", Color(0xFFDCFCE7), Color(0xFF15803D), 33.00, 42.00, true, "file:///android_asset/products/default_deal.jpg"),
                    DemoListItem("Fresh Milk 1L", "2 x 1L", "Carrefour", Color(0xFFE0F2FE), Color(0xFF0284C7), 7.90, 9.60, true, "file:///android_asset/products/default_deal.jpg"),
                    DemoListItem("Sunflower Oil 1.8L", "1 x 1.8L", "Lulu", Color(0xFFDCFCE7), Color(0xFF15803D), 16.90, 19.90, false, "file:///android_asset/products/default_deal.jpg"),
                    DemoListItem("Banana", "1 x 1kg", "Viva", Color(0xFFFFE4E6), Color(0xFFE11D48), 4.75, 5.95, true, "file:///android_asset/products/default_deal.jpg"),
                    DemoListItem("Eggs (30 pcs)", "1 x Pack", "Union Coop", Color(0xFFE0F2FE), Color(0xFF0284C7), 12.50, 14.75, false, "file:///android_asset/products/default_deal.jpg"),
                    DemoListItem("Chicken 1kg", "1 x 1kg", "Lulu", Color(0xFFDCFCE7), Color(0xFF15803D), 11.90, 14.50, false, "file:///android_asset/products/default_deal.jpg")
                )
                items(demoItems) { item ->
                    DemoListItemRow(item)
                }
            } else {
                items(displayItems) { shopItem ->
                    val product = products.find { it.id == shopItem.productId }
                    val store = product?.let { dealViewModel.getStore(it.retailerId) }
                    ListItemView(
                        shopItem = shopItem,
                        product = product,
                        store = store,
                        onToggleSelection = { viewModel.updateItem(shopItem.copy(isSelected = !shopItem.isSelected)) },
                        onRemove = { viewModel.removeItem(shopItem.id) },
                        onQtyChange = { newQty -> viewModel.updateItem(shopItem.copy(qty = newQty)) }
                    )
                }
            }

            item {
                BestBasketPriceCard()
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

data class DemoListItem(
    val name: String,
    val size: String,
    val storeName: String,
    val storeBg: Color,
    val storeTextColor: Color,
    val price: Double,
    val oldPrice: Double,
    val isChecked: Boolean,
    val image: String
)

@Composable
fun DemoListItemRow(item: DemoListItem) {
    var checked by remember { mutableStateOf(item.isChecked) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (checked) Color(0xFF8B5CF6) else Color.Transparent,
                border = if (!checked) BorderStroke(1.5.dp, Color(0xFFCBD5E1)) else null,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { checked = !checked }
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                modifier = Modifier.size(52.dp)
            ) {
                AsyncImage(
                    model = item.image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate900
                )
                Text(
                    item.size, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Slate400
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = item.storeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        item.storeName,
                        color = item.storeTextColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AED %.2f".format(item.price), 
                        fontWeight = FontWeight.ExtraBold, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "AED %.2f".format(item.oldPrice), 
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate400,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ListHeader(
    locationName: String,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Lists",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Plan, track and save on your shopping",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    onClick = onNavigateToLocation
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(locationName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Slate800)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate500)
                    }
                }
                BadgedBox(
                    badge = { 
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.size(40.dp),
                        onClick = onNavigateToNotifications
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.padding(10.dp), tint = Slate800)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { tabName ->
            val isSelected = tabName == currentListId
            val (icon, iconColor) = when (tabName) {
                "Grocery" -> Icons.Outlined.ShoppingCart to Color(0xFF059669)
                "Ramadan" -> Icons.Outlined.Nightlight to Color(0xFF6D28D9)
                "Party" -> Icons.Outlined.Celebration to Color(0xFFEA580C)
                else -> Icons.Outlined.ShoppingBasket to Color(0xFFB45309)
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MockupYellowPill else Color.White,
                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFCD34D) else Color(0xFFE2E8F0)),
                onClick = { onTabSelected(tabName) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Slate900 else iconColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        tabName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Slate900 else Slate700
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF3E8FF),
                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
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
                        tint = Color(0xFF6D28D9)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "New List",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D28D9)
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
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF3E8FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Monthly Budget", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AED 500.00", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Edit, 
                        contentDescription = "Edit Budget", 
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF6D28D9)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { 0.53f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color(0xFF818CF8),
                    trackColor = Color(0xFFDDD6FE)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AED 265.50 spent", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Slate500)
                    Text("AED 234.50 left", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Slate500)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You'll Save ⓘ", style = MaterialTheme.typography.labelSmall, color = Slate500)
                Text(
                    "AED 42.30", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = Color(0xFF16A34A)
                )
                Text("with best prices", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                BasketIllustration(size = 54.dp)
            }
        }
    }
}

@Composable
fun BestBasketPriceCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE8F8F0),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    Icons.Outlined.LocalOffer,
                    contentDescription = null,
                    tint = Color(0xFF15803D),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Best Basket Price",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ⓘ", color = Slate400, fontSize = 11.sp)
                }
                Text(
                    "At Nesto Hypermarket",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "8 items",
                        color = Slate600,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "AED 87.40",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF15803D)
                )
                Text(
                    "Original: AED 129.70",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Slate400,
                    textDecoration = TextDecoration.LineThrough
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "You save AED 42.30",
                        color = Color(0xFF15803D),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
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
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (shopItem.isSelected) Color(0xFF8B5CF6) else Color.Transparent,
                border = if (!shopItem.isSelected) BorderStroke(1.5.dp, Color(0xFFCBD5E1)) else null,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onToggleSelection() }
            ) {
                if (shopItem.isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                modifier = Modifier.size(52.dp)
            ) {
                AsyncImage(
                    model = product?.imageUrl ?: "file:///android_asset/products/default_deal.jpg",
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    shopItem.name, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate900
                )
                Text(
                    "${shopItem.qty} x 1 unit", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Slate400
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        store?.name ?: "Nesto",
                        color = Color(0xFF15803D),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AED %.2f".format(shopItem.targetPrice * shopItem.qty), 
                        fontWeight = FontWeight.ExtraBold, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "AED %.2f".format(shopItem.targetPrice * shopItem.qty * 1.25), 
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate400,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
        }
    }
}
