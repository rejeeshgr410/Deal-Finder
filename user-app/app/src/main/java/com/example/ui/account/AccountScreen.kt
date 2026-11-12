package com.example.ui.account

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
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.example.ui.home.SectionHeader
import com.example.ui.theme.*

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
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    
    Scaffold(
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { AccountHeader(userLocation = selectedEmirate) }
            
            item { 
                ProfileCard(
                    name = currentUser?.name ?: "Hassan Ahmed", 
                    email = currentUser?.email ?: "hassan@email.com"
                )
            }
            
            item {
                StatPillsRow()
            }
            
            item {
                SectionHeader(title = "My Tools", action = "View all", onAction = {})
                Spacer(modifier = Modifier.height(6.dp))
                MyToolsGrid(onNavigateToLoyaltyCards = onNavigateToLoyaltyCards)
            }

            item {
                SectionHeader(title = "My Activity", action = "View all", onAction = {})
                Spacer(modifier = Modifier.height(6.dp))
                MyActivityCard()
            }

            item {
                SectionHeader(title = "Saved & Followed", action = "View all", onAction = {})
                Spacer(modifier = Modifier.height(6.dp))
                SavedFollowedStores(viewModel)
            }

            item {
                SectionHeader(title = "App Settings", action = "", onAction = {})
                Spacer(modifier = Modifier.height(6.dp))
                AppSettingsCard(onLogout, onNavigateToNotificationSettings)
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AccountHeader(userLocation: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Manage your profile, lists and preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(userLocation, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Slate800)
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.padding(10.dp), tint = Slate800)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(name: String, email: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF3C7),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900
                )
                Text(
                    email, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFEDE9FE),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Regular Member", 
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF6D28D9)
                        )
                    }
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Slate400)
        }
    }
}

@Composable
fun StatPillsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill(
            icon = Icons.Outlined.LocalOffer,
            count = "42",
            label = "Saved Deals",
            iconColor = Color(0xFF6D28D9),
            bgColor = Color(0xFFEDE9FE),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.Favorite,
            count = "18",
            label = "Favorite Stores",
            iconColor = Color(0xFFE11D48),
            bgColor = Color(0xFFFFE4E6),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.AutoMirrored.Outlined.List,
            count = "7",
            label = "Shopping Lists",
            iconColor = Color(0xFFD97706),
            bgColor = Color(0xFFFEF3C7),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Outlined.Notifications,
            count = "12",
            label = "Price Alerts",
            iconColor = Color(0xFF16A34A),
            bgColor = Color(0xFFDCFCE7),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatPill(
    icon: ImageVector,
    count: String,
    label: String,
    iconColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape, 
                color = bgColor, 
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                count, 
                fontWeight = FontWeight.ExtraBold, 
                style = MaterialTheme.typography.titleMedium,
                color = Slate900
            )
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                color = Slate500,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

data class ToolGridItem(val title: String, val desc: String, val icon: ImageVector, val iconColor: Color, val bgColor: Color)

@Composable
fun MyToolsGrid(onNavigateToLoyaltyCards: () -> Unit = {}) {
    val tools = listOf(
        ToolGridItem("Smart Lists", "Create & manage smart lists", Icons.Outlined.ShoppingBasket, Color(0xFF16A34A), Color(0xFFDCFCE7)),
        ToolGridItem("Budget Planner", "Track spending & savings", Icons.Outlined.AccountBalanceWallet, Color(0xFF6D28D9), Color(0xFFEDE9FE)),
        ToolGridItem("Price History", "Track price trends of products", Icons.Outlined.ShowChart, Color(0xFFEA580C), Color(0xFFFFEDD5)),
        ToolGridItem("Coupons", "View saved coupons", Icons.Outlined.ConfirmationNumber, Color(0xFFE11D48), Color(0xFFFFE4E6)),
        ToolGridItem("Saved Flyers", "Your saved flyers", Icons.Outlined.MenuBook, Color(0xFF0284C7), Color(0xFFE0F2FE)),
        ToolGridItem("Deal Alerts", "Manage your alerts", Icons.Outlined.Campaign, Color(0xFFEA580C), Color(0xFFFFEDD5)),
        ToolGridItem("Nearby Stores", "Find stores near you", Icons.Outlined.LocationOn, Color(0xFF16A34A), Color(0xFFDCFCE7)),
        ToolGridItem("Compare History", "View your recent comparisons", Icons.Outlined.CompareArrows, Color(0xFF6D28D9), Color(0xFFEDE9FE))
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tools.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { tool ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = tool.bgColor,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(tool.icon, contentDescription = null, tint = tool.iconColor, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text(
                                    tool.title, 
                                    style = MaterialTheme.typography.titleSmall, 
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    tool.desc, 
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyActivityCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            ActivityRow(
                title = "Recent Orders (Lists)",
                desc = "View your recent shopping lists",
                icon = Icons.Outlined.Assignment,
                iconColor = Color(0xFF6D28D9),
                bgColor = Color(0xFFEDE9FE),
                count = "3"
            )
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            ActivityRow(
                title = "Recently Viewed",
                desc = "Products, flyers and deals you viewed",
                icon = Icons.Outlined.Visibility,
                iconColor = Color(0xFFD97706),
                bgColor = Color(0xFFFEF3C7),
                count = "23"
            )
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            ActivityRow(
                title = "Browsing History",
                desc = "Your recently searched items",
                icon = Icons.Outlined.History,
                iconColor = Color(0xFF16A34A),
                bgColor = Color(0xFFDCFCE7),
                count = "15"
            )
        }
    }
}

@Composable
fun ActivityRow(
    title: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    count: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape, 
            color = bgColor, 
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.titleSmall,
                color = Slate900
            )
            Text(
                desc, 
                style = MaterialTheme.typography.labelSmall, 
                color = Slate400
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                count, 
                style = MaterialTheme.typography.titleSmall, 
                color = Slate500,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SavedFollowedStores(viewModel: com.example.ui.DealFinderViewModel) {
    val stores by viewModel.stores.collectAsState()
    val displayStores = if (stores.isNotEmpty()) stores else listOf(
        com.example.data.RetailerEntity(id = "carrefour", name = "Carrefour", logoUrl = "file:///android_asset/logos/carrefour.png"),
        com.example.data.RetailerEntity(id = "lulu", name = "LuLu Hypermarket", logoUrl = "file:///android_asset/logos/lulu.png"),
        com.example.data.RetailerEntity(id = "nesto", name = "Nesto Hypermarket", logoUrl = "file:///android_asset/logos/nesto.png"),
        com.example.data.RetailerEntity(id = "viva", name = "Viva Supermarket", logoUrl = "file:///android_asset/logos/viva.png"),
        com.example.data.RetailerEntity(id = "life", name = "Life Pharmacy", logoUrl = "file:///android_asset/logos/life.png")
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(displayStores) { store ->
            val dealsCount = when {
                store.name.contains("lulu", ignoreCase = true) -> 18
                store.name.contains("carrefour", ignoreCase = true) -> 12
                store.name.contains("nesto", ignoreCase = true) -> 9
                store.name.contains("viva", ignoreCase = true) -> 7
                else -> 5
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .width(108.dp)
                    .height(130.dp)
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.size(52.dp)
                        ) {
                            AsyncImage(
                                model = store.logoUrl.ifBlank { "file:///android_asset/logos/default_logo.png" },
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            store.name, 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                            textAlign = TextAlign.Center, 
                            maxLines = 1,
                            color = Slate900
                        )
                        Text(
                            "$dealsCount Deals", 
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                            color = Slate400
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text("❤️", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AppSettingsCard(onLogout: () -> Unit, onNavigateToNotificationSettings: () -> Unit) {
    val localeManager = com.example.data.LocalLocaleManager.current
    val currentLangCode by (localeManager?.currentLanguage?.collectAsState() ?: remember { mutableStateOf("en") })
    val currentLangLabel = if (currentLangCode == "ar") "العربية" else "English"

    var showLanguageDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SettingRow(
                title = "Settings",
                desc = "Manage your app preferences",
                icon = Icons.Outlined.Settings,
                iconColor = Color(0xFF6D28D9),
                bgColor = Color(0xFFEDE9FE)
            )
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            SettingRow(
                title = "Language",
                desc = "",
                icon = Icons.Outlined.Language,
                iconColor = Color(0xFF0284C7),
                bgColor = Color(0xFFE0F2FE),
                value = currentLangLabel,
                onClick = { showLanguageDialog = true }
            )
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            SettingRow(
                title = "Help & Support",
                desc = "FAQs, support and feedback",
                icon = Icons.Outlined.HelpOutline,
                iconColor = Color(0xFF16A34A),
                bgColor = Color(0xFFDCFCE7)
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose Language", fontWeight = FontWeight.Bold, color = Slate900) },
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
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = currentLangCode == code,
                                onClick = {
                                    localeManager?.setLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close", color = Slate900) }
            }
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    value: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.titleSmall,
                color = Slate900
            )
            if (desc.isNotBlank()) {
                Text(
                    desc, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Slate400
                )
            }
        }
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
    }
}
