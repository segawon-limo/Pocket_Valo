package com.pocketvalo.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocketvalo.app.ui.screen.account.AccountScreen
import com.pocketvalo.app.ui.screen.agents.AgentDetailScreen
import com.pocketvalo.app.ui.screen.agents.AgentsScreen
import com.pocketvalo.app.ui.screen.home.HomeScreen
import com.pocketvalo.app.ui.screen.input.InputScreen
import com.pocketvalo.app.ui.screen.loading.LoadingScreen
import com.pocketvalo.app.ui.screen.login.LoginScreen
import com.pocketvalo.app.ui.screen.match.MatchScreen
import com.pocketvalo.app.ui.screen.splash.SplashScreen
import com.pocketvalo.app.ui.screen.store.StoreScreen
import com.pocketvalo.app.ui.screen.weapons.WeaponsScreen
import com.pocketvalo.app.ui.viewmodel.AccountViewModel
import com.pocketvalo.app.ui.viewmodel.AgentsViewModel
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel
import com.pocketvalo.app.ui.viewmodel.StoreViewModel
import com.pocketvalo.app.ui.viewmodel.WatchlistViewModel
import com.pocketvalo.app.ui.viewmodel.WeaponsViewModel
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.pocketvalo.app.R

sealed class Screen(val route: String) {
    object Splash       : Screen("splash")
    object Welcome      : Screen("welcome")
    object Login        : Screen("login")
    object AddAccount   : Screen("add_account")
    object Loading      : Screen("loading")
    object Input        : Screen("input")
    object Home         : Screen("home")
    object Store        : Screen("store")
    object Match        : Screen("match/{matchId}") {
        fun createRoute(matchId: String) = "match/$matchId"
    }
    object Account      : Screen("account")
    object Agents       : Screen("agents")
    object Weapons      : Screen("weapons")
    object NightMarket  : Screen("night_market")
    object BundleDetail : Screen("bundle_detail/{uuid}/{duration}/{basePrice}/{discPrice}") {
        fun createRoute(uuid: String, duration: Long, basePrice: Int, discPrice: Int) =
            "bundle_detail/$uuid/$duration/$basePrice/$discPrice"
    }
    object Watchlist    : Screen("watchlist")
    object AgentDetail  : Screen("agent/{agentId}") {
        fun createRoute(agentId: String) = "agent/$agentId"
    }
}

data class BottomNavItem(
    val route: String,
//    val icon: ImageVector,
    val icon: Painter,
    val label: String
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val playerViewModel: PlayerViewModel   = viewModel()
    val accountViewModel: AccountViewModel = viewModel()
    val storeViewModel: StoreViewModel     = viewModel()
    val weaponsViewModel: WeaponsViewModel  = viewModel()

    LaunchedEffect(Unit) {
        accountViewModel.onNavigateToLoading = {
            playerViewModel.resetForSwitch()
            navController.navigate(Screen.Loading.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    }

    val screensWithBottomNav = listOf(
        Screen.Home.route,
        Screen.Store.route,
        Screen.Agents.route,
        Screen.Weapons.route,
        Screen.Account.route
    )

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        containerColor = Color(0xFF0F1923),
        bottomBar = {
            if (currentRoute in screensWithBottomNav) {
                BottomNavigationBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Splash.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) { SplashScreen(navController) }
            composable(Screen.Welcome.route) {
                com.pocketvalo.app.ui.screen.welcome.WelcomeScreen(navController)
            }
            composable(Screen.Login.route) { LoginScreen(navController) }
            composable(Screen.AddAccount.route) {
                LoginScreen(
                    navController   = navController,
                    isAddAccount    = true,
                    onSwitchAccount = { playerViewModel.resetForSwitch() }
                )
            }
            composable(Screen.Loading.route) {
                LoadingScreen(navController, playerViewModel, storeViewModel, weaponsViewModel)
            }
            composable(Screen.Input.route)   { InputScreen(navController, playerViewModel) }
            composable(Screen.Home.route)    { HomeScreen(navController, playerViewModel) }

            composable(Screen.Store.route) {
                StoreScreen(storeViewModel = storeViewModel, navController = navController)
            }
            composable(Screen.NightMarket.route) {
                com.pocketvalo.app.ui.screen.store.NightMarketScreen(
                    storeViewModel = storeViewModel,
                    onBack         = { navController.popBackStack() }
                )
            }
            composable(Screen.BundleDetail.route) { backStackEntry ->
                val uuid      = backStackEntry.arguments?.getString("uuid") ?: ""
                val duration  = backStackEntry.arguments?.getString("duration")?.toLongOrNull() ?: 0L
                val basePrice = backStackEntry.arguments?.getString("basePrice")?.toIntOrNull() ?: 0
                val discPrice = backStackEntry.arguments?.getString("discPrice")?.toIntOrNull() ?: 0
                val skinItems = storeViewModel.uiState.value.store?.bundles
                    ?.firstOrNull { it.uuid == uuid }?.skinItems ?: emptyList()
                com.pocketvalo.app.ui.screen.store.BundleDetailScreen(
                    bundleUuid               = uuid,
                    durationRemainingSeconds = duration,
                    totalBasePrice           = basePrice,
                    totalDiscountedPrice     = discPrice,
                    skinItems                = skinItems,
                    storeViewModel           = storeViewModel,
                    navController            = navController
                )
            }
            composable(Screen.Watchlist.route) {
                val watchlistViewModel: WatchlistViewModel = viewModel()
                com.pocketvalo.app.ui.screen.store.WatchlistScreen(
                    viewModel = watchlistViewModel,
                    onBack    = { navController.popBackStack() }
                )
            }
            composable(Screen.Match.route) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchScreen(matchId, navController, playerViewModel)
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    playerViewModel  = playerViewModel,
                    accountViewModel = accountViewModel,
                    onAddAccount     = { navController.navigate(Screen.AddAccount.route) }
                )
            }
            composable(Screen.Agents.route) { backStackEntry ->
                val agentsViewModel: AgentsViewModel = viewModel(backStackEntry)
                AgentsScreen(navController, agentsViewModel)
            }
            composable(Screen.AgentDetail.route) { backStackEntry ->
                val agentId = backStackEntry.arguments?.getString("agentId") ?: ""
                val agentsEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Agents.route)
                }
                val agentsViewModel: AgentsViewModel = viewModel(agentsEntry)
                AgentDetailScreen(agentId, navController, agentsViewModel)
            }
            composable(Screen.Weapons.route) {
                WeaponsScreen(weaponsViewModel)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem(Screen.Agents.route,  painterResource(R.drawable.ic_agents),        "Agents"),
        BottomNavItem(Screen.Weapons.route, painterResource(R.drawable.ic_weapons),          "Collection"),
        BottomNavItem(Screen.Home.route,    painterResource(R.drawable.ic_home),          "Home"),
        BottomNavItem(Screen.Store.route,   painterResource(R.drawable.ic_store),  "Store"),
        BottomNavItem(Screen.Account.route, painterResource(R.drawable.ic_account), "Account"),
    )

    NavigationBar(
        containerColor = Color(0xFF1A2332),
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick  = {
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon   = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(24.dp),) },
                label  = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Color(0xFFFF4655),
                    selectedTextColor   = Color(0xFFFF4655),
                    unselectedIconColor = Color(0xFF9BA3AF),
                    unselectedTextColor = Color(0xFF9BA3AF),
                    indicatorColor      = Color(0xFF1A2332)
                )
            )
        }
    }
}