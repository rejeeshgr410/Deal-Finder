package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MockupYellowPill
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ShoppingListRepository
import com.example.ui.compare.CompareScreen
import com.example.ui.flyers.FlyersScreen
import com.example.ui.home.HomeScreen
import com.example.ui.lists.ListScreen
import com.example.ui.lists.ListViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.example.ui.account.AccountScreen
import com.example.ui.account.NotificationSettingsScreen
import com.example.ui.coupons.CouponsScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.location.LocationScreen

import com.example.ui.product.ProductDetailScreen
import com.example.ui.flyers.FlyerViewerScreen
import com.example.ui.retailer.RetailerScreen

import com.example.data.AuthRepository
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AuthScreen
import com.example.data.DealFinderRepository
import com.example.ui.DealFinderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun DealFinderApp(
    shoppingRepo: ShoppingListRepository,
    dealFinderRepo: DealFinderRepository,
    authRepo: AuthRepository,
    locationManager: com.example.data.LocationManager,
    loyaltyCardDao: com.example.data.LoyaltyCardDao? = null,
    localeManager: com.example.data.LocaleManager? = null,
    initialRoute: String = Screen.Home.route
) {
    val navController = rememberNavController()
    
    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Lists,
        Screen.Deals,
        Screen.Account
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            Surface(
                color = Color.White,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { screen ->
                        val selected = currentRoute == screen.route
                        if (selected) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MockupYellowPill,
                                modifier = Modifier
                                    .width(68.dp)
                                    .height(52.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = screen.activeIcon,
                                        contentDescription = screen.title,
                                        tint = Slate900,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Slate900
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = Slate400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val listViewModel: ListViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ListViewModel(shoppingRepo) as T
        })
        val dealViewModel: DealFinderViewModel = viewModel(factory = DealFinderViewModel.Factory(dealFinderRepo, locationManager))
        val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(authRepo))
        
        val currentUserId by authViewModel.currentUserId.collectAsState()

        LaunchedEffect(initialRoute) {
            if (initialRoute != Screen.Home.route) {
                navController.navigate(initialRoute)
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) {
                val currentUser by authViewModel.currentUser.collectAsState()
                HomeScreen(
                    viewModel = dealViewModel,
                    userName = currentUser?.name ?: "Guest",
                    onNavigateToCompare = { navController.navigate(Screen.Search.route) },
                    onNavigateToFlyers = { navController.navigate(Screen.Deals.route) },
                    onNavigateToList = { navController.navigate(Screen.Lists.route) },
                    onNavigateToCoupons = { navController.navigate(Screen.Coupons.route) },
                    onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) },
                    onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                    onNavigateToLoyaltyCards = { navController.navigate(Screen.LoyaltyCards.route) },
                    onAddToList = { product ->
                        listViewModel.addItem(
                            com.example.data.ShoppingListItem(
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
                    onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) },
                    onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) }
                ) 
            } 
            
            composable(Screen.Search.route) { // Map Compare logic to Search for now
                CompareScreen(
                    viewModel = dealViewModel,
                    listViewModel = listViewModel,
                    onAddToList = { product ->
                        listViewModel.addItem(
                            com.example.data.ShoppingListItem(
                                productId = product.id,
                                name = product.name,
                                targetPrice = product.currentPrice
                            )
                        )
                    },
                    onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) },
                    onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                    onNavigateToFlyer = { flyerId -> navController.navigate(Screen.FlyerViewer.createRoute(flyerId)) },
                    onNavigateToList = { navController.navigate(Screen.Lists.route) }
                ) 
            }
            composable(Screen.Lists.route) {
                if (currentUserId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Button(onClick = { navController.navigate(Screen.Auth.route) }) {
                            Text("Log in to view Lists")
                        }
                    }
                } else {
                    ListScreen(
                        viewModel = listViewModel,
                        dealViewModel = dealViewModel,
                        onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                        onNavigateToCompare = { navController.navigate(Screen.Search.route) }
                    )
                }
            }
            composable(Screen.Account.route) {
                if (currentUserId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Button(onClick = { navController.navigate(Screen.Auth.route) }) {
                            Text("Log in to view Account")
                        }
                    }
                } else {
                    AccountScreen(
                        viewModel = dealViewModel,
                        listViewModel = listViewModel,
                        authViewModel = authViewModel,
                        onLogout = { authViewModel.logout() },
                        onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings.route) },
                        onNavigateToLoyaltyCards = { navController.navigate(Screen.LoyaltyCards.route) }
                    )
                }
            }
            composable(Screen.Coupons.route) {
                if (currentUserId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Button(onClick = { navController.navigate(Screen.Auth.route) }) {
                            Text("Log in to view Coupons")
                        }
                    }
                } else {
                    CouponsScreen(viewModel = dealViewModel)
                }
            }
            composable(Screen.Notifications.route) {
                if (currentUserId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Button(onClick = { navController.navigate(Screen.Auth.route) }) {
                            Text("Log in to view Notifications")
                        }
                    }
                } else {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }
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
                    onBack = { navController.popBackStack() },
                    onNavigateToProduct = { newProductId -> navController.navigate(Screen.ProductDetail.createRoute(newProductId)) },
                    onNavigateToRetailer = { retailerId -> navController.navigate(Screen.Retailer.createRoute(retailerId)) },
                    onAddToList = {
                        val product = dealViewModel.products.value.find { it.id == productId }
                        if (product != null) {
                            listViewModel.addItem(
                                com.example.data.ShoppingListItem(
                                    productId = product.id,
                                    name = product.name,
                                    targetPrice = product.currentPrice
                                )
                            )
                        }
                    }
                )
            }
            composable(Screen.FlyerViewer.route) { backStackEntry ->
                val flyerId = backStackEntry.arguments?.getString("flyerId") ?: return@composable
                FlyerViewerScreen(
                    flyerId = flyerId,
                    viewModel = dealViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Retailer.route) { backStackEntry ->
                val retailerId = backStackEntry.arguments?.getString("retailerId") ?: return@composable
                RetailerScreen(
                    retailerId = retailerId,
                    viewModel = dealViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToProduct = { productId -> navController.navigate(Screen.ProductDetail.createRoute(productId)) }
                )
            }
            composable(Screen.NotificationSettings.route) {
                NotificationSettingsScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LoyaltyCards.route) {
                if (loyaltyCardDao != null) {
                    com.example.ui.loyalty.LoyaltyCardsScreen(
                        loyaltyCardDao = loyaltyCardDao,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
