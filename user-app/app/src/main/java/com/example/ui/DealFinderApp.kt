package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlin.math.abs
import kotlin.math.roundToInt
import com.example.R
import com.example.data.*
import com.example.ui.account.AccountScreen
import com.example.ui.account.NotificationSettingsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.clearance.ClearanceScreen
import com.example.ui.compare.CompareScreen
import com.example.ui.components.HomeTopBackgroundWash
import com.example.ui.coupons.CouponsScreen
import com.example.ui.flyers.FlyerViewerScreen
import com.example.ui.flyers.FlyersScreen
import com.example.ui.home.HomeScreen
import com.example.ui.lists.ListScreen
import com.example.ui.lists.ListViewModel
import com.example.ui.location.LocationScreen
import com.example.ui.loyalty.LoyaltyCardsScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.product.ProductDetailScreen
import com.example.ui.retailer.RetailerScreen
import com.example.ui.theme.FigmaCrimsonEnd
import com.example.ui.theme.FigmaCrimsonStart
import com.example.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch

@Composable
fun DealFinderApp(
    shoppingRepo: ShoppingListRepository,
    dealFinderRepo: DealFinderRepository,
    authRepo: AuthRepository,
    locationManager: LocationManager,
    loyaltyCardDao: LoyaltyCardDao? = null,
    localeManager: LocaleManager? = null,
    initialRoute: String = Screen.Home.route
) {
    val palette = LocalAppPalette.current
    val navController = rememberNavController()
    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Clearance,
        Screen.Coupons,
        Screen.Account
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in setOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Clearance.route,
        Screen.Coupons.route,
        Screen.Account.route,
        Screen.Deals.route,
        Screen.Lists.route
    )

    var openSearchFilters by remember { mutableStateOf(false) }
    var initialSearchQuery by remember { mutableStateOf<String?>(null) }
    val appScope = rememberCoroutineScope()
    // Observe language state cleanly if provided so bottom labels and routes recompose consistently
    val activeLanguage by (localeManager?.currentLanguage?.collectAsState() ?: remember { mutableStateOf("en") })

    val listViewModel: ListViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ListViewModel(shoppingRepo) as T
    })
    val dealViewModel: DealFinderViewModel = viewModel(factory = DealFinderViewModel.Factory(dealFinderRepo, locationManager))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(authRepo))

    val currentUserId by authViewModel.currentUserId.collectAsState()
    val loyaltyCards by remember(loyaltyCardDao) {
        loyaltyCardDao?.getAllCards() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        if (currentRoute != Screen.FlyerViewer.route && currentRoute != Screen.Auth.route) {
            HomeTopBackgroundWash(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )
        }

        val tabOrder = remember(items) { items.mapIndexed { index, s -> s.route to index }.toMap() }
        val isRtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                        initialOffsetY = { it }
                    ) + fadeIn(tween(220)),
                    exit = slideOutVertically(
                        animationSpec = tween(200),
                        targetOffsetY = { it }
                    ) + fadeOut(tween(180))
                ) {
                    FigmaFloatingBottomBar(
                        items = items,
                        currentRoute = currentRoute,
                        languageKey = activeLanguage,
                        onSelectTab = { route -> navigateToTab(route) }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    val fromIdx = tabOrder[initialState.destination.route]
                    val toIdx = tabOrder[targetState.destination.route]
                    if (fromIdx != null && toIdx != null && fromIdx != toIdx) {
                        val forward = if (isRtlLayout) toIdx < fromIdx else toIdx > fromIdx
                        val offsetSign = if (forward) 1 else -1
                        slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> offsetSign * (fullWidth * 0.16f).toInt() }
                        ) + fadeIn(tween(260, easing = FastOutSlowInEasing))
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> (if (isRtlLayout) -1 else 1) * (fullWidth * 0.12f).toInt() }
                        ) + fadeIn(tween(220))
                    }
                },
                exitTransition = {
                    val fromIdx = tabOrder[initialState.destination.route]
                    val toIdx = tabOrder[targetState.destination.route]
                    if (fromIdx != null && toIdx != null && fromIdx != toIdx) {
                        val forward = if (isRtlLayout) toIdx < fromIdx else toIdx > fromIdx
                        val offsetSign = if (forward) -1 else 1
                        slideOutHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> offsetSign * (fullWidth * 0.12f).toInt() }
                        ) + fadeOut(tween(220, easing = FastOutSlowInEasing))
                    } else {
                        fadeOut(tween(180))
                    }
                },
                popEnterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> (if (isRtlLayout) 1 else -1) * (fullWidth * 0.10f).toInt() }
                    ) + fadeIn(tween(220))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> (if (isRtlLayout) -1 else 1) * (fullWidth * 0.14f).toInt() }
                    ) + fadeOut(tween(180))
                }
            ) {
                composable(Screen.Auth.route) {
                    AuthScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = { navController.popBackStack() },
                        onContinueAsGuest = { navController.popBackStack() }
                    )
                }

                composable(Screen.Home.route) {
                    val currentUser by authViewModel.currentUser.collectAsState()
                    val rawName = currentUser?.name?.trim().orEmpty()
                    val userDisplayName: String? = if (
                        currentUserId == null ||
                        rawName.isBlank() ||
                        rawName.equals("Guest", ignoreCase = true) ||
                        rawName.startsWith("qa.", ignoreCase = true) ||
                        rawName.startsWith("qa_", ignoreCase = true) ||
                        rawName.contains("qa shopper", ignoreCase = true)
                    ) {
                        null
                    } else {
                        rawName
                    }
                    HomeScreen(
                        viewModel = dealViewModel,
                        userName = userDisplayName,
                        onNavigateToCompare = { openFilters ->
                            initialSearchQuery = null
                            openSearchFilters = openFilters
                            navigateToTab(Screen.Search.route)
                        },
                        onSearchBrand = { query ->
                            openSearchFilters = false
                            initialSearchQuery = query
                            navigateToTab(Screen.Search.route)
                        },
                        onNavigateToFlyers = { navigateToTab(Screen.Deals.route) },
                        onNavigateToList = { navigateToTab(Screen.Lists.route) },
                        onNavigateToCoupons = { navigateToTab(Screen.Coupons.route) },
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) },
                        onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                        onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                        onNavigateToLoyaltyCards = { navController.navigate(Screen.LoyaltyCards.route) },
                        onAddToList = { product ->
                            listViewModel.addItem(
                                ShoppingListItem(
                                    productId = product.id,
                                    name = product.name,
                                    targetPrice = product.currentPrice
                                )
                            )
                        }
                    )
                }

                composable(Screen.Deals.route) {
                    FlyersScreen(
                        viewModel = dealViewModel,
                        onBack = { navigateToTab(Screen.Home.route) },
                        onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) },
                        onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToSearch = { navigateToTab(Screen.Search.route) },
                        onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings.route) },
                        onAddToList = { product ->
                            listViewModel.addItem(
                                ShoppingListItem(
                                    productId = product.id,
                                    name = product.name,
                                    targetPrice = product.currentPrice
                                )
                            )
                        }
                    )
                }

                composable(Screen.Search.route) {
                    CompareScreen(
                        viewModel = dealViewModel,
                        listViewModel = listViewModel,
                        openFiltersInitially = openSearchFilters,
                        onConsumedOpenFilters = { openSearchFilters = false },
                        initialSearchQuery = initialSearchQuery,
                        onConsumedInitialSearchQuery = { initialSearchQuery = null },
                        onAddToList = { product ->
                            listViewModel.addItem(
                                ShoppingListItem(
                                    productId = product.id,
                                    name = product.name,
                                    targetPrice = product.currentPrice
                                )
                            )
                        },
                        onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                        onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                        onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) },
                        onNavigateToList = { navigateToTab(Screen.Lists.route) },
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings.route) },
                        onBack = { navigateToTab(Screen.Home.route) }
                    )
                }

                composable(Screen.Lists.route) {
                    ListScreen(
                        viewModel = listViewModel,
                        dealViewModel = dealViewModel,
                        hasLoyaltyCardInWallet = loyaltyCards.isNotEmpty(),
                        isSignedIn = currentUserId != null,
                        onBack = { navigateToTab(Screen.Home.route) },
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToCompare = { navigateToTab(Screen.Search.route) },
                        onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
                    )
                }

                composable(Screen.Account.route) {
                    AccountScreen(
                        viewModel = dealViewModel,
                        listViewModel = listViewModel,
                        authViewModel = authViewModel,
                        loyaltyCardsCount = loyaltyCards.size,
                        onBack = { navigateToTab(Screen.Home.route) },
                        onLogout = { authViewModel.logout() },
                        onDeleteAccountData = {
                            appScope.launch {
                                listViewModel.clearAllLocal()
                                loyaltyCardDao?.clearAll()
                                dealFinderRepo.clearCachedFlyers()
                                authViewModel.logout()
                            }
                        },
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings.route) },
                        onNavigateToLoyaltyCards = { navController.navigate(Screen.LoyaltyCards.route) },
                        onNavigateToLists = { navigateToTab(Screen.Lists.route) },
                        onNavigateToCoupons = { navigateToTab(Screen.Coupons.route) },
                        onNavigateToFlyers = { navigateToTab(Screen.Deals.route) },
                        onNavigateToSearch = { navigateToTab(Screen.Search.route) },
                        onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                        onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                        onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) }
                    )
                }

                composable(Screen.Coupons.route) {
                    CouponsScreen(
                        viewModel = dealViewModel,
                        onBack = { navigateToTab(Screen.Home.route) },
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        viewModel = dealViewModel,
                        isSignedIn = currentUserId != null,
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onNavigateToSettings = { navController.navigate(Screen.NotificationSettings.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Location.route) {
                    LocationScreen(
                        locationManager = locationManager,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.ProductDetail.route) { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                    ProductDetailScreen(
                        productId = productId,
                        viewModel = dealViewModel,
                        isSignedIn = currentUserId != null,
                        onBack = { navController.popBackStack() },
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onNavigateToProduct = { newProductId -> navController.navigate(Screen.ProductDetail.createRoute(newProductId)) },
                        onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                        onAddToList = { selectedProduct ->
                            listViewModel.addItem(
                                ShoppingListItem(
                                    productId = selectedProduct.id,
                                    name = selectedProduct.name,
                                    targetPrice = selectedProduct.currentPrice
                                )
                            )
                        }
                    )
                }

                composable(Screen.FlyerViewer.route) { backStackEntry ->
                    val flyerId = backStackEntry.arguments?.getString("flyerId") ?: return@composable
                    FlyerViewerScreen(
                        flyerId = flyerId,
                        viewModel = dealViewModel,
                        isSignedIn = currentUserId != null,
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                        onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Retailer.route) { backStackEntry ->
                    val retailerId = backStackEntry.arguments?.getString("retailerId") ?: return@composable
                    RetailerScreen(
                        retailerId = retailerId,
                        viewModel = dealViewModel,
                        isSignedIn = currentUserId != null,
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onBack = { navController.popBackStack() },
                        onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                        onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) },
                        onAddToList = { product ->
                            listViewModel.addItem(
                                ShoppingListItem(
                                    productId = product.id,
                                    name = product.name,
                                    targetPrice = product.currentPrice
                                )
                            )
                        }
                    )
                }

                composable(Screen.NotificationSettings.route) {
                    NotificationSettingsScreen(
                        authViewModel = authViewModel,
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.LoyaltyCards.route) {
                    val emirate by dealViewModel.selectedEmirate.collectAsState()
                    val unreadCount by dealViewModel.unreadNotificationCount.collectAsState()
                    if (loyaltyCardDao != null) {
                        LoyaltyCardsScreen(
                            loyaltyCardDao = loyaltyCardDao,
                            selectedEmirate = emirate,
                            hasUnreadNotifications = unreadCount > 0,
                            onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                            onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                            onNavigateBack = {
                                if (!navController.popBackStack()) {
                                    navigateToTab(Screen.Home.route)
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_wallet_desc),
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                composable(Screen.Clearance.route) {
                    ClearanceScreen(
                        viewModel = dealViewModel,
                        onBack = { navigateToTab(Screen.Home.route) },
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onNavigateToWallet = { navController.navigate(Screen.LoyaltyCards.route) },
                        onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                        onAddBundleToList = { bundle ->
                            listViewModel.addItem(
                                ShoppingListItem(
                                    productId = bundle.id,
                                    name = bundle.title,
                                    targetPrice = bundle.bundleOfferPrice
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FigmaFloatingBottomBar(
    items: List<Screen>,
    currentRoute: String?,
    languageKey: String,
    onSelectTab: (String) -> Unit
) {
    val palette = LocalAppPalette.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.let { if (it >= 0) it else 0 }

    val animatedSlot by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 340f
        ),
        label = "LiquidNavSlot"
    )
    // Liquid elongation during fast transitions between non-adjacent tabs
    val travelDelta = abs(animatedSlot - selectedIndex.toFloat()).coerceIn(0f, 1.5f)
    val liquidStretchX = 1f + (travelDelta * 0.10f)
    val liquidCompressY = 1f - (travelDelta * 0.04f)

    // Key recomposition on languageKey to update tab labels cleanly
    key(languageKey) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(34.dp),
                color = Color.Transparent,
                shadowElevation = 14.dp,
                tonalElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (palette.isDark) {
                            listOf(Color.White.copy(alpha = 0.16f), palette.cardBorder.copy(alpha = 0.65f))
                        } else {
                            listOf(Color.White.copy(alpha = 0.96f), palette.cardBorder.copy(alpha = 0.72f))
                        }
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                val barGradient = if (palette.isDark) {
                    listOf(
                        palette.surfaceGradStart.copy(alpha = 0.94f),
                        palette.surfaceGradMid.copy(alpha = 0.96f),
                        palette.surfaceGradEnd.copy(alpha = 0.98f)
                    )
                } else {
                    listOf(
                        Color(0xFCFFFFFF),
                        Color(0xFAF8F7FC),
                        Color(0xF7EFEFF6)
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .background(Brush.verticalGradient(colors = barGradient))
                        .padding(6.dp)
                        .pointerInput(items, isRtl) {
                            var lastEmitted = -1
                            detectHorizontalDragGestures(
                                onDragStart = { lastEmitted = -1 },
                                onDragEnd = { lastEmitted = -1 },
                                onDragCancel = { lastEmitted = -1 }
                            ) { change, _ ->
                                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                                val rawRatio = change.position.x / widthPx
                                val normRatio = if (isRtl) (1f - rawRatio) else rawRatio
                                val targetIdx = (normRatio * items.size)
                                    .toInt()
                                    .coerceIn(0, items.lastIndex)
                                if (targetIdx != lastEmitted && items[targetIdx].route != currentRoute) {
                                    lastEmitted = targetIdx
                                    onSelectTab(items[targetIdx].route)
                                }
                            }
                        }
                ) {
                    val slotWidthDp = maxWidth / items.size
                    val slotWidthPx = with(density) { slotWidthDp.toPx() }
                    val stretchExtraPx = ((liquidStretchX - 1f) * slotWidthPx) * 0.5f
                    val indicatorOffsetPx = (animatedSlot * slotWidthPx - stretchExtraPx)
                        .coerceIn(0f, ((items.size - 1) * slotWidthPx))

                    // Liquid sliding crimson glass indicator capsule
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = indicatorOffsetPx.roundToInt(), y = 0) }
                            .width(slotWidthDp * liquidStretchX)
                            .fillMaxHeight()
                            .scale(scaleX = 1f, scaleY = liquidCompressY)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = Color(0x55ED0032),
                                spotColor = Color(0x55ED0032)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF2458),
                                        FigmaCrimsonStart,
                                        FigmaCrimsonEnd
                                    )
                                )
                            )
                    ) {
                        // Top specular gloss highlight inside active pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.24f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // Centered tab buttons with liquid animated icon & label typography
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { screen ->
                            val label = when (screen) {
                                Screen.Home -> stringResource(R.string.nav_home)
                                Screen.Search -> stringResource(R.string.nav_search)
                                Screen.Clearance -> stringResource(R.string.nav_clearance)
                                Screen.LoyaltyCards -> stringResource(R.string.nav_wallet)
                                Screen.Coupons -> stringResource(R.string.nav_coupons)
                                Screen.Account -> stringResource(R.string.nav_account)
                                else -> screen.title
                            }
                            val selected = currentRoute == screen.route
                            val animatedTint by animateColorAsState(
                                targetValue = if (selected) Color.White else palette.navInactive,
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                label = "NavTint_${screen.route}"
                            )
                            val itemScale by animateFloatAsState(
                                targetValue = if (selected) 1.04f else 0.96f,
                                animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f),
                                label = "NavScale_${screen.route}"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onSelectTab(screen.route) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(itemScale),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
                                ) {
                                    FigmaNavIcon(
                                        screen = screen,
                                        tint = animatedTint,
                                        isSelected = selected,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically),
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                            letterSpacing = 0.25.sp,
                                            lineHeight = 12.sp,
                                            color = animatedTint,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeightStyle = LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.Both
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FigmaNavIcon(
    screen: Screen,
    tint: Color,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strokeRatio by animateFloatAsState(
        targetValue = if (isSelected) 0.108f else 0.088f,
        animationSpec = tween(durationMillis = 220),
        label = "NavIconStroke_${screen.route}"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * strokeRatio
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (screen) {
            Screen.Home -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.44f)
                    lineTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.44f)
                    lineTo(w * 0.86f, h * 0.86f)
                    lineTo(w * 0.62f, h * 0.86f)
                    lineTo(w * 0.62f, h * 0.56f)
                    lineTo(w * 0.38f, h * 0.56f)
                    lineTo(w * 0.38f, h * 0.86f)
                    lineTo(w * 0.14f, h * 0.86f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
            }
            Screen.Search -> {
                val r = w * 0.30f
                val cx = w * 0.43f
                val cy = h * 0.43f
                drawCircle(color = tint, radius = r, center = Offset(cx, cy), style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(cx + r * 0.72f, cy + r * 0.72f),
                    end = Offset(w * 0.86f, h * 0.86f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
            }
            Screen.Clearance -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.14f),
                    size = Size(w * 0.72f, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.34f, h * 0.66f),
                    end = Offset(w * 0.66f, h * 0.34f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
                drawCircle(color = tint, radius = sw * 0.65f, center = Offset(w * 0.36f, h * 0.38f))
                drawCircle(color = tint, radius = sw * 0.65f, center = Offset(w * 0.64f, h * 0.62f))
            }
            Screen.LoyaltyCards, Screen.Lists -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.10f, h * 0.16f),
                    size = Size(w * 0.80f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.10f, h * 0.36f),
                    end = Offset(w * 0.90f, h * 0.36f),
                    strokeWidth = sw,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = w * 0.065f,
                    center = Offset(w * 0.72f, h * 0.58f),
                    style = stroke
                )
            }
            Screen.Coupons -> {
                val p = Path().apply {
                    moveTo(w * 0.52f, h * 0.12f)
                    lineTo(w * 0.84f, h * 0.12f)
                    lineTo(w * 0.84f, h * 0.44f)
                    lineTo(w * 0.44f, h * 0.84f)
                    lineTo(w * 0.12f, h * 0.52f)
                    close()
                }
                drawPath(p, color = tint, style = stroke)
                drawCircle(color = tint, radius = sw * 0.65f, center = Offset(w * 0.70f, h * 0.26f))
            }
            Screen.Account -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.17f,
                    center = Offset(w * 0.50f, h * 0.30f),
                    style = stroke
                )
                val p = Path().apply {
                    moveTo(w * 0.16f, h * 0.86f)
                    cubicTo(
                        w * 0.18f, h * 0.62f,
                        w * 0.32f, h * 0.58f,
                        w * 0.50f, h * 0.58f
                    )
                    cubicTo(
                        w * 0.68f, h * 0.58f,
                        w * 0.82f, h * 0.62f,
                        w * 0.84f, h * 0.86f
                    )
                }
                drawPath(p, color = tint, style = stroke)
            }
            else -> {}
        }
    }
}
