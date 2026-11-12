package com.example.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.ui.theme.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.example.ui.home.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: com.example.ui.DealFinderViewModel,
    listViewModel: com.example.ui.lists.ListViewModel,
    authViewModel: com.example.ui.auth.AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToLoyaltyCards: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { AccountHeader() }
            item { 
                if (currentUser != null) {
                    ProfileCard(currentUser!!.name, currentUser!!.email, viewModel, listViewModel, currentUser)
                } 
            }
            
            item {
                SectionHeader(title = "My Tools & Wallet", action = "", onAction = {})
                MyToolsGrid(onNavigateToLoyaltyCards = onNavigateToLoyaltyCards)
            }

            item {
                SectionHeader(title = "App Settings", action = "", onAction = {})
                AppSettingsList(onLogout, onNavigateToNotificationSettings)
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AccountHeader() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage your profile, lists and preferences",
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
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
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

@Composable
fun ProfileCard(
    name: String,
    email: String,
    viewModel: com.example.ui.DealFinderViewModel,
    listViewModel: com.example.ui.lists.ListViewModel,
    currentUser: com.example.data.UserEntity?
) {
    val savedDeals by viewModel.savedDeals.collectAsState()
    val followedRetailers by viewModel.followedRetailers.collectAsState()
    val lists by listViewModel.uiState.collectAsState()

    val savedDealsCount = savedDeals.size.toString()
    val favStoresCount = followedRetailers.size.toString()
    val listCount = lists.map { it.listId }.distinct().size.toString()
    val alertsCount = if (currentUser?.notifyPriceDrops == true) "Active" else "Off"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.size(64.dp)
                ) {
                    // Placeholder for Avatar
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFEDE7F6),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp), tint = Purple40)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Regular Member", style = MaterialTheme.typography.labelMedium, color = Purple40)
                        }
                    }
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(icon = Icons.Outlined.LocalOffer, count = savedDealsCount, label = "Saved Deals", iconColor = Purple40, bgColor = LightPeriwinkle)
                StatItem(icon = Icons.Default.Favorite, count = favStoresCount, label = "Favorite Stores", iconColor = CatPinkIcon, bgColor = CatPink)
                StatItem(icon = Icons.Outlined.List, count = listCount, label = "Shopping Lists", iconColor = CatYellowIcon, bgColor = CatYellow)
                StatItem(icon = Icons.Outlined.Notifications, count = alertsCount, label = "Price Alerts", iconColor = CatGreenIcon, bgColor = CatGreen)
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, count: String, label: String, iconColor: Color, bgColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(36.dp)) {
                 Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(8.dp))
             }
             Spacer(modifier = Modifier.width(8.dp))
             Text(count, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class ToolItem(val name: String, val desc: String, val icon: ImageVector, val iconColor: Color, val bgColor: Color, val onClick: () -> Unit = {})

@Composable
fun MyToolsGrid(onNavigateToLoyaltyCards: () -> Unit = {}) {
    val tools = listOf(
        ToolItem("Loyalty Cards", "Digital barcode membership cards", Icons.Outlined.CreditCard, Color(0xFF004F9F), Color(0xFFE3F2FD), onClick = onNavigateToLoyaltyCards),
        ToolItem("Smart Lists", "Create & manage smart lists", Icons.Outlined.ShoppingBasket, CatGreenIcon, CatGreen),
        ToolItem("Budget Planner", "Track spending & savings", Icons.Outlined.AccountBalanceWallet, Color(0xFF5C6BC0), Color(0xFFE8EAF6)),
        ToolItem("Price History", "Track price trends of products", Icons.Outlined.ShowChart, CatYellowIcon, CatYellow),
        ToolItem("Coupons", "View saved coupons", Icons.Outlined.ConfirmationNumber, CatPinkIcon, CatPink),
        ToolItem("Saved Flyers", "Your saved flyers", Icons.Outlined.MenuBook, CatBlueIcon, CatBlue),
        ToolItem("Deal Alerts", "Manage your alerts", Icons.Outlined.Campaign, CatOrangeIcon, CatOrange),
        ToolItem("Nearby Stores", "Find stores near you", Icons.Outlined.LocationOn, CatGreenIcon, CatGreen)
    )
    
    // Instead of LazyVerticalGrid which requires a fixed height if put inside LazyColumn directly, we use Column of Rows.
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        tools.chunked(4).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).aspectRatio(0.85f).clickable { item.onClick() },
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(16.dp), color = item.bgColor, modifier = Modifier.size(56.dp)) {
                                Icon(item.icon, contentDescription = null, tint = item.iconColor, modifier = Modifier.padding(16.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(item.desc, style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
                // Fill empty slots if less than 4
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun MyActivityList() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityItem("Recent Orders (Lists)", "View your recent shopping lists", Icons.Outlined.Assignment, Purple40, LightPeriwinkle, "3")
        ActivityItem("Recently Viewed", "Products, flyers and deals you viewed", Icons.Outlined.Visibility, CatYellowIcon, CatYellow, "23")
        ActivityItem("Browsing History", "Your recently searched items", Icons.Outlined.History, CatGreenIcon, CatGreen, "15")
    }
}

@Composable
fun ActivityItem(title: String, desc: String, icon: ImageVector, iconColor: Color, bgColor: Color, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(12.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(count, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SavedFollowedStores(viewModel: com.example.ui.DealFinderViewModel) {
    val stores by viewModel.stores.collectAsState()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(stores) { store ->
            val isFavorite = store.name != "Life Pharmacy"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.width(100.dp).height(120.dp)
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    ) {
                        AsyncImage(
                            model = store.logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(store.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
                        Text("${(5..20).random()} Deals", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFF44336) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppSettingsList(onLogout: () -> Unit, onNavigateToNotificationSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backupManager = androidx.compose.runtime.remember { com.example.data.BackupRecoveryManager(context) }
    
    var showThemeDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showBackupDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showLanguageDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val localeManager = com.example.data.LocalLocaleManager.current
    val currentLangCode by (localeManager?.currentLanguage?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("en") })
    val currentLangLabel = if (currentLangCode == "ar") "العربية" else "English"

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingItem("Settings", "Manage your app preferences", Icons.Outlined.Settings, Purple40, LightPeriwinkle)
        SettingItem("Theme", "Switch between Light & Dark mode", Icons.Outlined.DarkMode, Color(0xFF5C6BC0), Color(0xFFE8EAF6), onClick = { showThemeDialog = true })
        SettingItem("Backup & Recovery", "Save lists, settings, and stats securely", Icons.Outlined.CloudUpload, Color(0xFF00ACC1), Color(0xFFE0F7FA), onClick = { showBackupDialog = true })
        SettingItem("Notifications", "Flyers, Deals, Expiry Alerts", Icons.Outlined.Notifications, CatGreenIcon, CatGreen, onClick = onNavigateToNotificationSettings)
        SettingItem("Language / اللغة", "English or العربية (Arabic)", Icons.Outlined.Language, CatBlueIcon, CatBlue, currentLangLabel, onClick = { showLanguageDialog = true })
        SettingItem("Help & Support", "FAQs, support and feedback", Icons.Outlined.HelpOutline, CatOrangeIcon, CatOrange)
        SettingItem("Logout", "Sign out of your account", Icons.Outlined.ExitToApp, Color(0xFFF44336), Color(0xFFFFEBEE), onClick = onLogout)
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose Language / اختر اللغة") },
            text = {
                Column {
                    listOf("en" to "English (Default)", "ar" to "العربية (Arabic)").forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localeManager?.setLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 14.dp)
                        ) {
                            RadioButton(
                                selected = currentLangCode == code,
                                onClick = {
                                    localeManager?.setLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            }
        )
    }

    if (showThemeDialog) {
        val themeManager = com.example.ui.theme.LocalThemeManager.current
        val currentTheme by themeManager.themeMode.collectAsState()
        val themes = listOf(
            com.example.data.ThemeMode.AUTO to "System Default",
            com.example.data.ThemeMode.LIGHT to "Light",
            com.example.data.ThemeMode.DARK to "Dark"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    themes.forEach { (theme, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeManager.setThemeMode(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = (theme == currentTheme),
                                onClick = {
                                    themeManager.setThemeMode(theme)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showBackupDialog) {
        val sharedPrefs = androidx.compose.runtime.remember { context.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE) }
        var lastBackupTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(sharedPrefs.getLong("last_backup", 0L)) }
        val backupStatusText = if (lastBackupTime == 0L) {
            "Never Backed Up"
        } else {
            val dateStr = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(lastBackupTime))
            "Last Backup: $dateStr"
        }

        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Data Recovery") },
            text = {
                Column {
                    Text(
                        text = "Securely backup your offline lists, search history, and personalization preferences.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Backup Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(text = backupStatusText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            backupManager.initiateBackup()
                            val now = System.currentTimeMillis()
                            sharedPrefs.edit().putLong("last_backup", now).apply()
                            lastBackupTime = now
                            android.widget.Toast.makeText(context, "Backup completed successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Fresh Backup")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (lastBackupTime == 0L) {
                                android.widget.Toast.makeText(context, "No backup found to restore!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                backupManager.initiateRecovery()
                                android.widget.Toast.makeText(context, "Settings restored from backup!", android.widget.Toast.LENGTH_SHORT).show()
                                showBackupDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore From Last Backup")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingItem(title: String, desc: String?, icon: ImageVector, iconColor: Color, bgColor: Color, value: String? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(12.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (desc != null) {
                Text(desc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
