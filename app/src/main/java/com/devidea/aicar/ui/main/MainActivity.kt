package com.devidea.aicar.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.devidea.aicar.R
import com.devidea.aicar.ui.main.components.history.HistoryNavGraph
import com.devidea.aicar.ui.main.components.HomeScreen
import com.devidea.aicar.ui.main.components.DashboardScreen
import com.devidea.aicar.ui.main.components.SettingsScreen
import com.devidea.aicar.ui.theme.CarProjectTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class NavItem(@StringRes val titleRes: Int, val icon: ImageVector) {
    object Home : NavItem(R.string.title_main, Icons.Filled.Home)
    object History : NavItem(R.string.title_history, Icons.Filled.Place)
    object Car : NavItem(R.string.title_manage, Icons.Filled.DirectionsCar)
    object Settings : NavItem(R.string.title_setting, Icons.Filled.Settings)
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
    var selectedTab by remember { mutableStateOf<NavItem>(NavItem.Home) }
    val historyNavController = rememberNavController()
    Scaffold(
        modifier = modifier,
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
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when (selectedTab) {
                is NavItem.Home -> HomeScreen(modifier)
                is NavItem.History -> HistoryNavGraph(
                    navController = historyNavController,
                    onBackToList = { /* 필요 시 루트로 popBackStack */ }
                )

                is NavItem.Car -> DashboardScreen(modifier)
                is NavItem.Settings -> SettingsScreen(modifier)
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

@Preview(showBackground = true)
@Composable
fun PreviewCarManagementMainScreen() {
    CarProjectTheme {
        CarManagementMainScreen(
            modifier = Modifier.fillMaxSize()
        )
    }
}
