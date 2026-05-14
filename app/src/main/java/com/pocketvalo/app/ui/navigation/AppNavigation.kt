package com.pocketvalo.app.ui.navigation

import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocketvalo.app.ui.screen.account.AccountScreen
import com.pocketvalo.app.ui.screen.agents.AgentsScreen
import com.pocketvalo.app.ui.screen.home.HomeScreen
import com.pocketvalo.app.ui.screen.input.InputScreen
import com.pocketvalo.app.ui.screen.match.MatchScreen
import com.pocketvalo.app.ui.screen.splash.SplashScreen
import com.pocketvalo.app.ui.screen.store.StoreScreen
import com.pocketvalo.app.ui.screen.weapons.WeaponsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Input : Screen("input")
    object Home : Screen("home")
    object Store : Screen("store")
    object Match : Screen("match/{matchId}") {
        fun createRoute(matchId: String) = "match/$matchId"
    }
    object Account : Screen("account")
    object Agents : Screen("agents")
    object Weapons : Screen("weapons")
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val playerViewModel: PlayerViewModel = viewModel()
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
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) { SplashScreen(navController) }
            composable(Screen.Input.route) { InputScreen(navController, playerViewModel) }
            composable(Screen.Home.route) { HomeScreen(navController, playerViewModel) }
            composable(Screen.Store.route) { StoreScreen() }
            composable(Screen.Match.route) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchScreen(matchId, navController, playerViewModel)
            }
            composable(Screen.Account.route) { AccountScreen() }
            composable(Screen.Agents.route) { AgentsScreen() }
            composable(Screen.Weapons.route) { WeaponsScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem(Screen.Agents.route, Icons.Default.Person, "Agents"),
        BottomNavItem(Screen.Weapons.route, Icons.Default.Star, "Weapons"),
        BottomNavItem(Screen.Home.route, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Store.route, Icons.Default.ShoppingCart, "Store"),
        BottomNavItem(Screen.Account.route, Icons.Default.AccountCircle, "Account"),
    )

    NavigationBar(
        containerColor = Color(0xFF1A2332),
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFFF4655),
                    selectedTextColor = Color(0xFFFF4655),
                    unselectedIconColor = Color(0xFF9BA3AF),
                    unselectedTextColor = Color(0xFF9BA3AF),
                    indicatorColor = Color(0xFF1A2332)
                )
            )
        }
    }
}