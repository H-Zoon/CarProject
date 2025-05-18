package com.devidea.aicar.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devidea.aicar.R
import com.devidea.aicar.ui.main.components.HomeScreen
import com.devidea.aicar.ui.main.components.DashboardScreen
import com.devidea.aicar.ui.main.components.NotificationScreen
import com.devidea.aicar.ui.main.components.SettingsScreen
import com.devidea.aicar.ui.main.components.history.SessionListScreen
import com.devidea.aicar.ui.main.components.history.SessionOverviewScreen
import com.devidea.aicar.ui.theme.CarProjectTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class NavItem(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Home : NavItem("home", R.string.title_main, Icons.Filled.Home)
    object History : NavItem("history", R.string.title_history, Icons.Filled.Place)
    object Car : NavItem("car", R.string.title_manage, Icons.Filled.DirectionsCar)
    object Settings : NavItem("setting", R.string.title_setting, Icons.Filled.Settings)
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarProjectTheme {
                CarManagementMainScreen()
            }
        }
    }
}

@Composable
fun CarManagementMainScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(NavItem.Home, NavItem.History, NavItem.Car, NavItem.Settings)
    val selectedTab = resolveNavItemFromRoute(currentRoute)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            CarBottomNavBar(
                items = navItems,
                selected = selectedTab,
                onItemSelected = { item ->
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavItem.Home.route) {
                HomeScreen(
                    modifier,
                    onNotificationClick = { navController.navigate("notification") }
                )
            }

            composable("notification") {
                NotificationScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavItem.History.route) {
                SessionListScreen(
                    onSessionClick = { sessionId ->
                        navController.navigate("sessionOverview/$sessionId")
                    }
                )
            }

            composable(
                route = "sessionOverview/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId =
                    backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                SessionOverviewScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavItem.Car.route) {
                DashboardScreen(modifier)
            }

            composable(NavItem.Settings.route) {
                SettingsScreen(modifier)
            }
        }
    }
}

@Composable
fun CarBottomNavBar(
    items: List<NavItem>,
    selected: NavItem,
    onItemSelected: (NavItem) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.titleRes)) },
                label = { Text(stringResource(item.titleRes)) },
                selected = item == selected,
                onClick = { onItemSelected(item) }
            )
        }
    }
}

fun resolveNavItemFromRoute(currentRoute: String?): NavItem {
    return when {
        currentRoute == null -> NavItem.Home
        currentRoute.startsWith("sessionOverview") || currentRoute == NavItem.History.route -> NavItem.History
        currentRoute == NavItem.Home.route -> NavItem.Home
        currentRoute == NavItem.Car.route -> NavItem.Car
        currentRoute == NavItem.Settings.route -> NavItem.Settings
        else -> NavItem.Home
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCarManagementMainScreen() {
    CarProjectTheme {
        CarManagementMainScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}
