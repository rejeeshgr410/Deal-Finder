package com.example.ui.account

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.DemoRegistry
import com.example.data.LocalLocaleManager
import com.example.data.LocationManager
import com.example.data.RetailerEntity
import com.example.data.ThemeMode
import com.example.ui.DealFinderViewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.lists.ListViewModel
import com.example.ui.theme.*

private data class AccountToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val onClick: () -> Unit
)

private fun Modifier.pressScaleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun AccountScreen(
    viewModel: DealFinderViewModel,
    listViewModel: ListViewModel,
    authViewModel: AuthViewModel,
    loyaltyCardsCount: Int = 0,
    onBack: () -> Unit = {},
    onLogout: () -> Unit,
    onDeleteAccountData: () -> Unit = onLogout,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToLoyaltyCards: () -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    onNavigateToCoupons: () -> Unit = {},
    onNavigateToFlyers: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {},
    onNavigateToFlyer: (String) -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val selectedEmirate by viewModel.selectedEmirate.collectAsState()
    val savedDeals by viewModel.savedDeals.collectAsState()
    val savedFlyers by viewModel.savedFlyers.collectAsState()
    val followedRetailers by viewModel.followedRetailers.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val shoppingListItems by listViewModel.uiState.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()

    val demoAvailable by DemoRegistry.isDemoAvailable.collectAsState()
    val demoEnabled by DemoRegistry.isDemoEnabled.collectAsState()

    val localeManager = LocalLocaleManager.current
    val currentLangCode by (localeManager?.currentLanguage?.collectAsState() ?: remember { mutableStateOf("en") })
    val themeManager = LocalThemeManager.current
    val currentThemeMode by themeManager.themeMode.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSavedItemsDialog by remember { mutableStateOf(false) }
    var showFollowedDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }

    val isSignedIn = currentUserId != null
    val displayName = when {
        !isSignedIn -> stringResource(R.string.guest_shopper)
        !currentUser?.name.isNullOrBlank() -> currentUser!!.name
        else -> "Deal Finder Member"
    }
    val displayEmail = when {
        !isSignedIn -> stringResource(R.string.guest_signin_hint)
        !currentUser?.email.isNullOrBlank() -> currentUser!!.email
        else -> ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Unified Top Header Row
        item {
            AccountHeaderRow(
                localizedEmirate = localizedEmirate,
                hasUnreadNotifications = unreadNotifCount > 0,
                onBack = onBack,
                onNavigateToLocation = onNavigateToLocation,
                onNavigateToNotifications = onNavigateToNotifications
            )
        }

        // 2. Profile Hero Card
        item {
            ProfileHeroCard(
                name = displayName,
                email = displayEmail,
                localizedEmirate = localizedEmirate,
                isSignedIn = isSignedIn,
                onClick = {
                    if (isSignedIn) showProfileDialog = true else onNavigateToAuth()
                },
                onSignInClick = onNavigateToAuth
            )
        }

        // 3. Elevated Stats Capsule
        item {
            AccountKpiCapsule(
                savedDealsCount = savedDeals.size,
                walletCardsCount = loyaltyCardsCount,
                listItemsCount = shoppingListItems.size,
                followedStoresCount = followedRetailers.size,
                onDealsClick = { showSavedItemsDialog = true },
                onWalletClick = onNavigateToLoyaltyCards,
                onListsClick = onNavigateToLists,
                onStoresClick = { showFollowedDialog = true }
            )
        }

        // 4. Quick Tools Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountSectionHeader(
                    icon = Icons.Outlined.GridView,
                    title = stringResource(R.string.quick_tools),
                    actionLabel = stringResource(R.string.nav_wallet),
                    onAction = onNavigateToLoyaltyCards
                )
                QuickToolsBentoGrid(
                    items = listOf(
                        AccountToolItem(
                            title = stringResource(R.string.loyalty_wallet_title),
                            subtitle = stringResource(R.string.cards_label),
                            icon = Icons.Outlined.AccountBalanceWallet,
                            iconColor = QuickActionFlyersIcon,
                            bgColor = QuickActionFlyersBg,
                            onClick = onNavigateToLoyaltyCards
                        ),
                        AccountToolItem(
                            title = stringResource(R.string.smart_list_title),
                            subtitle = stringResource(R.string.open_basket_optimizer),
                            icon = Icons.AutoMirrored.Outlined.List,
                            iconColor = QuickActionListIcon,
                            bgColor = QuickActionListBg,
                            onClick = onNavigateToLists
                        ),
                        AccountToolItem(
                            title = stringResource(R.string.coupons_title),
                            subtitle = stringResource(R.string.coupons_subtitle),
                            icon = Icons.Outlined.ConfirmationNumber,
                            iconColor = QuickActionCouponsIcon,
                            bgColor = QuickActionCouponsBg,
                            onClick = onNavigateToCoupons
                        ),
                        AccountToolItem(
                            title = stringResource(R.string.enable_notifications),
                            subtitle = stringResource(R.string.notification_preferences),
                            icon = Icons.Outlined.NotificationsActive,
                            iconColor = QuickActionCompareIcon,
                            bgColor = QuickActionCompareBg,
                            onClick = onNavigateToNotificationSettings
                        )
                    )
                )
            }
        }

        // 5. My Activity Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountSectionHeader(
                    icon = Icons.Outlined.Insights,
                    title = stringResource(R.string.my_activity),
                    actionLabel = stringResource(R.string.nav_search),
                    onAction = onNavigateToSearch
                )
                MyActivitySurfaceCard(
                    listsCount = shoppingListItems.size,
                    savedCount = savedDeals.size + savedFlyers.size,
                    searchesCount = recentSearches.size,
                    onListsClick = onNavigateToLists,
                    onSavedClick = { showSavedItemsDialog = true },
                    onSearchesClick = onNavigateToSearch
                )
            }
        }

        // 6. Saved & Followed Stores Carousel (dynamic live + demo stores)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountSectionHeader(
                    icon = Icons.Outlined.Storefront,
                    title = stringResource(R.string.saved_followed_stores),
                    actionLabel = stringResource(R.string.view_all),
                    onAction = onNavigateToSearch
                )
                FollowedStoresStrip(
                    viewModel = viewModel,
                    onStoreClick = onNavigateToRetailer
                )
            }
        }

        // 7. Preferences & Privacy Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountSectionHeader(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(R.string.preferences_support),
                    actionLabel = null,
                    onAction = {}
                )
                PreferencesSurfaceCard(
                    localizedEmirate = localizedEmirate,
                    languageLabel = if (currentLangCode == "ar") stringResource(R.string.arabic) else stringResource(R.string.english),
                    themeLabel = when (currentThemeMode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.AUTO -> "Auto"
                    },
                    isSignedIn = isSignedIn,
                    demoAvailable = demoAvailable,
                    demoEnabled = demoEnabled,
                    onToggleDemoMode = { checked ->
                        DemoRegistry.setDemoEnabled(checked)
                        Toast.makeText(
                            context,
                            if (checked) "Demo data enabled" else "Live data only mode enabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onNotificationPrefs = onNavigateToNotificationSettings,
                    onLocationPrefs = onNavigateToLocation,
                    onLanguageClick = { showLanguageDialog = true },
                    onThemeClick = { showThemeDialog = true },
                    onSupportClick = { showSupportDialog = true },
                    onDeleteDataClick = { showDeleteDataDialog = true }
                )
            }
        }

        // 8. Sign Out / Sign In Primary Action
        item {
            AccountFooterActions(
                isSignedIn = isSignedIn,
                onSignOut = onLogout,
                onSignIn = onNavigateToAuth
            )
        }
    }

    // Profile Details Dialog
    if (showProfileDialog) {
        AccountThemedDialog(onDismiss = { showProfileDialog = false }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarRingBadge(name = displayName, isSignedIn = isSignedIn, sizeDp = 64)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = displayName,
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                if (displayEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayEmail,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.subtleSurface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileDetailLine(stringResource(R.string.location), localizedEmirate)
                        HorizontalDivider(color = palette.divider, thickness = 1.dp)
                        ProfileDetailLine(
                            stringResource(R.string.language),
                            if (currentLangCode == "ar") stringResource(R.string.arabic) else stringResource(R.string.english)
                        )
                        HorizontalDivider(color = palette.divider, thickness = 1.dp)
                        ProfileDetailLine(stringResource(R.string.my_loyalty_cards), "$loyaltyCardsCount")
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                CrimsonGradientButton(
                    text = stringResource(R.string.done),
                    onClick = { showProfileDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Saved Deals & Flyers Dialog
    if (showSavedItemsDialog) {
        AccountThemedDialog(onDismiss = { showSavedItemsDialog = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saved Deals & Flyers (${savedDeals.size + savedFlyers.size})",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                )
                if (savedDeals.isEmpty() && savedFlyers.isEmpty()) {
                    Text(
                        text = "No saved deals or flyers yet. Tap the bookmark icon on any deal or flyer to save it here.",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                } else {
                    savedDeals.take(4).forEach { deal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.subtleSurface)
                                .clickable {
                                    showSavedItemsDialog = false
                                    onNavigateToProduct(deal.id)
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(deal.name, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = palette.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AED %.2f".format(deal.currentPrice), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = palette.crimsonLink)
                        }
                    }
                    savedFlyers.take(4).forEach { flyer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.subtleSurface)
                                .clickable {
                                    showSavedItemsDialog = false
                                    onNavigateToFlyer(flyer.id)
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(flyer.title, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = palette.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.crimsonLink, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                CrimsonGradientButton(
                    text = if (savedDeals.isEmpty() && savedFlyers.isEmpty()) "Browse Flyers" else stringResource(R.string.close),
                    onClick = {
                        val wasEmpty = savedDeals.isEmpty() && savedFlyers.isEmpty()
                        showSavedItemsDialog = false
                        if (wasEmpty) onNavigateToFlyers()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Followed Stores Dialog
    if (showFollowedDialog) {
        AccountThemedDialog(onDismiss = { showFollowedDialog = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${stringResource(R.string.saved_followed_stores)} (${followedRetailers.size})",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                )
                if (followedRetailers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_followed_stores),
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                } else {
                    followedRetailers.take(6).forEach { store ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.subtleSurface)
                                .clickable {
                                    showFollowedDialog = false
                                    onNavigateToRetailer(store.id)
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(store.name, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.crimsonLink, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                CrimsonGradientButton(
                    text = stringResource(R.string.done),
                    onClick = { showFollowedDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AccountThemedDialog(onDismiss = { showLanguageDialog = false }) {
            Column {
                Text(
                    text = stringResource(R.string.language),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.language_desc),
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        color = palette.textSecondary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "en" to "English (Default)",
                    "ar" to "العربية (Arabic RTL)"
                ).forEach { (code, label) ->
                    val selected = currentLangCode == code
                    SelectableOptionRow(
                        label = label,
                        selected = selected,
                        onClick = {
                            localeManager?.setLanguage(code)
                            showLanguageDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = { showLanguageDialog = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.close), color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Appearance Dialog
    if (showThemeDialog) {
        AccountThemedDialog(onDismiss = { showThemeDialog = false }) {
            Column {
                Text(
                    text = stringResource(R.string.appearance),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.appearance_desc),
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        color = palette.textSecondary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    ThemeMode.LIGHT to "Light (Recommended)",
                    ThemeMode.AUTO to "Follow System",
                    ThemeMode.DARK to "Dark"
                ).forEach { (mode, label) ->
                    SelectableOptionRow(
                        label = label,
                        selected = currentThemeMode == mode,
                        onClick = {
                            themeManager.setThemeMode(mode)
                            showThemeDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = { showThemeDialog = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.close), color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Help & Support Dialog
    if (showSupportDialog) {
        AccountThemedDialog(onDismiss = { showSupportDialog = false }) {
            Column {
                Text(
                    text = stringResource(R.string.help_support),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = palette.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.subtleSurface)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProfileDetailLine(
                            label = "Support",
                            value = "support@dealfinder.ae",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:support@dealfinder.ae")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Email: support@dealfinder.ae", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        HorizontalDivider(color = palette.divider, thickness = 1.dp)
                        ProfileDetailLine("Version", BuildConfig.VERSION_NAME)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                CrimsonGradientButton(
                    text = stringResource(R.string.done),
                    onClick = { showSupportDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Delete Account Data Dialog
    if (showDeleteDataDialog) {
        AccountThemedDialog(onDismiss = { showDeleteDataDialog = false }) {
            Column {
                Text(
                    text = stringResource(R.string.delete_account_data),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.delete_account_data_desc),
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = palette.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.subtleSurface)
                            .border(1.dp, palette.divider, RoundedCornerShape(50))
                            .pressScaleClickable { showDeleteDataDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }
                    CrimsonGradientButton(
                        text = stringResource(R.string.delete),
                        onClick = {
                            showDeleteDataDialog = false
                            onDeleteAccountData()
                            Toast.makeText(
                                context,
                                "Local data cleared, push token revoked & signed out.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeaderRow(
    localizedEmirate: String,
    hasUnreadNotifications: Boolean,
    onBack: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, CircleShape)
                    .pressScaleClickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.account_title),
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.5).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.account_subtitle),
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        color = palette.textSecondary,
                        letterSpacing = (-0.25).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .pressScaleClickable(onClick = onNavigateToLocation)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = localizedEmirate,
                        style = TextStyle(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = palette.textPrimary
                    )
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
                        .pressScaleClickable(onClick = onNavigateToNotifications),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        modifier = Modifier.size(16.dp),
                        tint = palette.textPrimary
                    )
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
private fun ProfileHeroCard(
    name: String,
    email: String,
    localizedEmirate: String,
    isSignedIn: Boolean,
    onClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val cardShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, cardShape, ambientColor = Color(0x2024203C), spotColor = Color(0x2024203C))
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, cardShape)
            .pressScaleClickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x48FFD5E2), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.15f),
                    radius = size.maxDimension * 0.45f
                ),
                center = Offset(size.width * 0.92f, size.height * 0.15f),
                radius = size.maxDimension * 0.45f
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarRingBadge(name = name, isSignedIn = isSignedIn, sizeDp = 58)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = TextStyle(
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.35).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = email,
                        style = TextStyle(
                            fontSize = 11.5.sp,
                            color = palette.textSecondary,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.crimsonPillBg)
                            .border(1.dp, palette.crimsonPillBorder, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = palette.crimsonLink,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSignedIn) stringResource(R.string.member_badge) else stringResource(R.string.guest_badge),
                                style = TextStyle(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.crimsonLink,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.subtleSurface)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = localizedEmirate,
                            style = TextStyle(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            if (isSignedIn) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.crimsonPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View profile",
                        tint = palette.crimsonLink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                CrimsonGradientButton(
                    text = stringResource(R.string.sign_in),
                    onClick = onSignInClick,
                    heightDp = 34,
                    horizontalPaddingDp = 12
                )
            }
        }
    }
}

@Composable
private fun AvatarRingBadge(
    name: String,
    isSignedIn: Boolean,
    sizeDp: Int
) {
    val initials = remember(name, isSignedIn) {
        if (!isSignedIn) ""
        else {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            when {
                parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "DF"
            }
        }
    }

    Box(modifier = Modifier.size(sizeDp.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x30ED0032),
                    spotColor = Color(0x30ED0032)
                )
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF5F8), Color(0xFFFFDCE6)))),
                contentAlignment = Alignment.Center
            ) {
                if (isSignedIn && initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        style = TextStyle(
                            fontSize = (sizeDp * 0.34f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FigmaCrimsonText,
                            letterSpacing = (-0.5).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = FigmaCrimsonText,
                        modifier = Modifier.size((sizeDp * 0.48f).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountKpiCapsule(
    savedDealsCount: Int,
    walletCardsCount: Int,
    listItemsCount: Int,
    followedStoresCount: Int,
    onDealsClick: () -> Unit,
    onWalletClick: () -> Unit,
    onListsClick: () -> Unit,
    onStoresClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = Color(0x1E24203C), spotColor = Color(0x1E24203C))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, shape)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KpiMetricSlot(
            count = savedDealsCount,
            label = stringResource(R.string.nav_deals),
            icon = Icons.Outlined.LocalOffer,
            accent = palette.crimsonLink,
            onClick = onDealsClick,
            modifier = Modifier.weight(1f)
        )
        KpiVerticalDivider()
        KpiMetricSlot(
            count = walletCardsCount,
            label = stringResource(R.string.nav_wallet),
            icon = Icons.Outlined.AccountBalanceWallet,
            accent = QuickActionCompareIcon,
            onClick = onWalletClick,
            modifier = Modifier.weight(1f)
        )
        KpiVerticalDivider()
        KpiMetricSlot(
            count = listItemsCount,
            label = stringResource(R.string.nav_lists),
            icon = Icons.AutoMirrored.Outlined.List,
            accent = QuickActionListIcon,
            onClick = onListsClick,
            modifier = Modifier.weight(1f)
        )
        KpiVerticalDivider()
        KpiMetricSlot(
            count = followedStoresCount,
            label = stringResource(R.string.saved_coupons),
            icon = Icons.Outlined.FavoriteBorder,
            accent = QuickActionDealsIcon,
            onClick = onStoresClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiMetricSlot(
    count: Int,
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = count.toString(),
                style = TextStyle(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    fontFeatureSettings = "tnum",
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textMuted,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun KpiVerticalDivider() {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(palette.divider)
    )
}

@Composable
private fun AccountSectionHeader(
    icon: ImageVector,
    title: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.25).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        if (!actionLabel.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .pressScaleClickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.crimsonLink,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.crimsonLink,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickToolsBentoGrid(items: List<AccountToolItem>) {
    val palette = LocalAppPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                pair.forEach { tool ->
                    val cardShape = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(7.dp, cardShape, ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
                            .clip(cardShape)
                            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                            .border(1.dp, palette.cardBorder, cardShape)
                            .pressScaleClickable(onClick = tool.onClick)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(tool.bgColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = null,
                                    tint = tool.iconColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tool.title,
                                    style = TextStyle(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tool.subtitle,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = palette.textSecondary,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
private fun MyActivitySurfaceCard(
    listsCount: Int,
    savedCount: Int,
    searchesCount: Int,
    onListsClick: () -> Unit,
    onSavedClick: () -> Unit,
    onSearchesClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, cardShape, ambientColor = Color(0x1A24203C), spotColor = Color(0x1A24203C))
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, cardShape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        AccountRowItem(
            title = stringResource(R.string.smart_list_title),
            subtitle = stringResource(R.string.smart_list_subtitle),
            icon = Icons.Outlined.ShoppingBag,
            iconColor = QuickActionCompareIcon,
            bgColor = QuickActionCompareBg,
            countBadge = listsCount.toString(),
            onClick = onListsClick
        )
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        AccountRowItem(
            title = "Saved Deals & Flyers",
            subtitle = "Bookmarked items and circulars",
            icon = Icons.Outlined.BookmarkBorder,
            iconColor = QuickActionListIcon,
            bgColor = QuickActionListBg,
            countBadge = savedCount.toString(),
            onClick = onSavedClick
        )
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        AccountRowItem(
            title = stringResource(R.string.recent_searches),
            subtitle = stringResource(R.string.trending_searches),
            icon = Icons.Outlined.History,
            iconColor = QuickActionDealsIcon,
            bgColor = QuickActionDealsBg,
            countBadge = searchesCount.toString(),
            onClick = onSearchesClick
        )
    }
}

@Composable
private fun FollowedStoresStrip(
    viewModel: DealFinderViewModel,
    onStoreClick: (String) -> Unit
) {
    val palette = LocalAppPalette.current
    val followedStores by viewModel.followedRetailers.collectAsState()
    val activeStores by viewModel.stores.collectAsState()
    val products by viewModel.products.collectAsState()

    val primaryStores = if (followedStores.isNotEmpty()) followedStores else activeStores

    if (primaryStores.isEmpty()) {
        val cardShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, cardShape, ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
                .clip(cardShape)
                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                .border(1.dp, palette.cardBorder, cardShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_followed_stores),
                fontSize = 11.5.sp,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(primaryStores, key = { it.id }) { store ->
                val dealsCount = products.count { it.retailerId == store.id }
                val isFollowed = followedStores.any { it.id == store.id }
                StoreFollowTile(
                    store = store,
                    dealsCount = dealsCount,
                    isFollowed = isFollowed,
                    onClick = { onStoreClick(store.id) },
                    onToggleFollow = { viewModel.toggleFollowedRetailer(store) }
                )
            }
        }
    }
}

@Composable
private fun StoreFollowTile(
    store: RetailerEntity,
    dealsCount: Int,
    isFollowed: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val palette = LocalAppPalette.current
    val cardShape = RoundedCornerShape(18.dp)
    val pastelBg = remember(store.name, palette.isDark) {
        if (palette.isDark) palette.subtleSurface
        else {
            val lower = store.name.lowercase()
            when {
                "lulu" in lower -> FlyerMintCard
                "carrefour" in lower -> FlyerBlueCard
                "nesto" in lower -> FlyerCreamCard
                "viva" in lower -> FlyerPinkCard
                else -> Color(0xFFF3F2F8)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(124.dp)
            .shadow(7.dp, cardShape, ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, cardShape)
            .pressScaleClickable(onClick = onClick)
            .padding(9.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(pastelBg),
                contentAlignment = Alignment.Center
            ) {
                if (store.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = store.logoUrl,
                        contentDescription = store.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                } else {
                    Text(
                        text = store.name.take(3).uppercase(),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = store.name,
                style = TextStyle(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (dealsCount > 0) "$dealsCount Active Deals" else "View Offers",
                style = TextStyle(
                    fontSize = 9.5.sp,
                    color = palette.textSecondary,
                    fontFeatureSettings = "tnum",
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .pressScaleClickable(onClick = onToggleFollow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFollowed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFollowed) "Unfollow store" else "Follow store",
                tint = if (isFollowed) FigmaCrimsonLink else FigmaTextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun PreferencesSurfaceCard(
    localizedEmirate: String,
    languageLabel: String,
    themeLabel: String,
    isSignedIn: Boolean,
    demoAvailable: Boolean,
    demoEnabled: Boolean,
    onToggleDemoMode: (Boolean) -> Unit,
    onNotificationPrefs: () -> Unit,
    onLocationPrefs: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeClick: () -> Unit,
    onSupportClick: () -> Unit,
    onDeleteDataClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, cardShape, ambientColor = Color(0x1A24203C), spotColor = Color(0x1A24203C))
            .clip(cardShape)
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, cardShape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        AccountRowItem(
            title = stringResource(R.string.notification_preferences),
            subtitle = stringResource(R.string.notification_preferences_desc),
            icon = Icons.Outlined.Notifications,
            iconColor = QuickActionCompareIcon,
            bgColor = QuickActionCompareBg,
            onClick = onNotificationPrefs
        )
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        AccountRowItem(
            title = stringResource(R.string.emirate_region),
            subtitle = stringResource(R.string.emirate_region_desc),
            icon = Icons.Outlined.LocationOn,
            iconColor = QuickActionFlyersIcon,
            bgColor = QuickActionFlyersBg,
            pillValue = localizedEmirate,
            onClick = onLocationPrefs
        )
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        AccountRowItem(
            title = stringResource(R.string.language),
            subtitle = stringResource(R.string.language_desc),
            icon = Icons.Outlined.Language,
            iconColor = Color(0xFF0284C7),
            bgColor = Color(0xFFE0F2FE),
            pillValue = languageLabel,
            onClick = onLanguageClick
        )
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        AccountRowItem(
            title = stringResource(R.string.appearance),
            subtitle = stringResource(R.string.appearance_desc),
            icon = Icons.Outlined.Palette,
            iconColor = QuickActionListIcon,
            bgColor = QuickActionListBg,
            pillValue = themeLabel,
            onClick = onThemeClick
        )
        if (demoAvailable) {
            HorizontalDivider(color = palette.divider, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(QuickActionCouponsBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        tint = QuickActionCouponsIcon,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.demo_mode),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.demo_mode_desc),
                        style = TextStyle(
                            fontSize = 10.5.sp,
                            color = palette.textSecondary,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = demoEnabled,
                    onCheckedChange = onToggleDemoMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FigmaCrimsonEnd
                    )
                )
            }
        }
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        AccountRowItem(
            title = stringResource(R.string.help_support),
            subtitle = stringResource(R.string.help_support_desc),
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            iconColor = QuickActionDealsIcon,
            bgColor = QuickActionDealsBg,
            onClick = onSupportClick
        )
        if (isSignedIn) {
            HorizontalDivider(color = palette.divider, thickness = 1.dp)
            AccountRowItem(
                title = stringResource(R.string.delete_account_data),
                subtitle = stringResource(R.string.delete_account_data_desc),
                icon = Icons.Outlined.DeleteOutline,
                iconColor = FigmaCrimsonEnd,
                bgColor = Color(0xFFFFE5EB),
                titleColor = FigmaCrimsonEnd,
                onClick = onDeleteDataClick
            )
        }
    }
}

@Composable
private fun AccountRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    countBadge: String? = null,
    pillValue: String? = null,
    titleColor: Color = LocalAppPalette.current.textPrimary,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        color = palette.textSecondary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (countBadge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.crimsonPillBg)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countBadge,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.crimsonLink,
                        fontFeatureSettings = "tnum",
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (pillValue != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.subtleSurface)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = pillValue,
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AccountFooterActions(
    isSignedIn: Boolean,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isSignedIn) {
            val pillShape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .shadow(7.dp, pillShape, ambientColor = Color(0x28ED0032), spotColor = Color(0x28ED0032))
                    .clip(pillShape)
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.crimsonPillBorder, pillShape)
                    .pressScaleClickable(onClick = onSignOut),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = palette.crimsonLink,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.sign_out),
                        style = TextStyle(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.crimsonLink,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        } else {
            CrimsonGradientButton(
                text = stringResource(R.string.sign_in_or_create),
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                heightDp = 46
            )
        }

        Text(
            text = stringResource(R.string.footer_tagline, stringResource(R.string.currency_aed)),
            style = TextStyle(
                fontSize = 10.5.sp,
                color = palette.textMuted,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
private fun AccountThemedDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAppPalette.current
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(16.dp, shape, ambientColor = Color(0x3024203C), spotColor = Color(0x3024203C))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                .border(1.dp, palette.cardBorder, shape)
                .padding(20.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SelectableOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) palette.crimsonPillBg else palette.subtleSurface)
            .border(
                width = 1.dp,
                color = if (selected) FigmaAccentBorder else palette.divider,
                shape = shape
            )
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) palette.crimsonLink else palette.textPrimary,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd))
                    else Brush.verticalGradient(listOf(Color.White, Color.White))
                )
                .border(
                    width = 1.dp,
                    color = if (selected) Color.Transparent else Color(0xFFD5D3E2),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileDetailLine(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.5.sp,
                color = palette.textSecondary,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
        Text(
            text = value,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (onClick != null) palette.crimsonLink else palette.textPrimary,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
private fun CrimsonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    heightDp: Int = 42,
    horizontalPaddingDp: Int = 16
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(heightDp.dp)
            .shadow(8.dp, shape, ambientColor = Color(0x40ED0032), spotColor = Color(0x40ED0032))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
            .pressScaleClickable(onClick = onClick)
            .padding(horizontal = horizontalPaddingDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1
        )
    }
}
