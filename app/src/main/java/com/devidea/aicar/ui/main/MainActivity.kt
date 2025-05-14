package com.devidea.aicar.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.devidea.aicar.ui.main.components.BluetoothActionComponent
import com.devidea.aicar.ui.main.components.HistoryNavGraph
import com.devidea.aicar.ui.main.components.HomeScreen
import com.devidea.aicar.ui.main.components.DashboardScreen
import com.devidea.aicar.ui.main.components.SettingsScreen
import com.devidea.aicar.ui.theme.CarProjectTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class NavItem(val label: String, val icon: ImageVector) {
    object Home : NavItem("Home", Icons.Filled.Home)
    object History : NavItem("Location", Icons.Filled.Place)
    object Car : NavItem("Car", Icons.Filled.DirectionsCar)
    object Settings : NavItem("Settings", Icons.Filled.Settings)
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CarProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CarManagementMainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CarManagementMainScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf<NavItem>(NavItem.Home) }
    val historyNavController = rememberNavController()
    Scaffold(
        modifier = modifier,
        topBar = { BluetoothActionComponent() },
        bottomBar = {
            CarBottomNavBar(
                items = listOf(NavItem.Home, NavItem.History, NavItem.Car, NavItem.Settings),
                selected = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                is NavItem.Home -> HomeScreen()
                is NavItem.History -> HistoryNavGraph(
                    navController = historyNavController,
                    onBackToList = { /* 필요 시 루트로 popBackStack */ }
                )
                is NavItem.Car -> DashboardScreen()
                is NavItem.Settings -> SettingsScreen()
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
        windowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = item == selected,
                onClick = { onItemSelected(item) }
            )
        }
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
