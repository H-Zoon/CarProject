package com.devidea.chevy.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devidea.chevy.App
import com.devidea.chevy.R
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.service.BleService
import com.devidea.chevy.service.BleServiceManager
import com.devidea.chevy.ui.components.CardItem
import com.devidea.chevy.ui.screen.dashboard.Dashboard
import com.devidea.chevy.ui.screen.map.MapEnterScreen
import com.devidea.chevy.ui.components.NeumorphicBox
import com.devidea.chevy.ui.components.NeumorphicCard
import com.devidea.chevy.ui.screen.navi.KNavi
import com.devidea.chevy.ui.theme.CarProjectTheme
import com.devidea.chevy.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var serviceManager: BleServiceManager
    @Inject
    lateinit var kNavi: KNavi
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CarProjectTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize(), content = { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        MainNavHost(
                            navController = navController, mainViewModel = viewModel
                        )
                    }
                })
            }
        }
    }

    @Composable
    fun MainNavHost(
        navController: NavHostController, mainViewModel: MainViewModel
    ) {
        val cardItems = listOf(
            CardItem("차량정보", "This is a description", MainViewModel.NavRoutes.Details),
            CardItem("네비게이션", "This is a description", MainViewModel.NavRoutes.Map)
        )

        // navigationEvent를 수집하여 네비게이션 처리
        LaunchedEffect(mainViewModel.navigationEvent) {
            mainViewModel.navigationEvent.collect { route ->
                navController.navigate(route.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
            }
        }

        val guidanceEvent by mainViewModel.requestNavGuidance.collectAsState()

        NavHost(navController = navController, startDestination = MainViewModel.NavRoutes.Home.route) {
            composable(MainViewModel.NavRoutes.Home.route) {
                HomeScreen(mainViewModel, cardItems)
            }
            composable(MainViewModel.NavRoutes.Details.route) {
                Dashboard()
            }
            composable(MainViewModel.NavRoutes.Map.route) {
                MapEnterScreen(mainViewModel)
            }
            composable(MainViewModel.NavRoutes.Nav.route) {
                kNavi.NaviScreen(guidanceEvent = guidanceEvent)
            }
        }
    }

    @Composable
    fun HomeScreen(viewModel: MainViewModel, cardItems: List<CardItem>) {
        Column(
            modifier = Modifier
                .fillMaxSize() // 전체 화면을 채우도록 설정
                .padding(20.dp), // 원하는 패딩 값으로 수정 (예: 16dp)
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BluetoothActionComponent(viewModel = viewModel)
            CarImage(viewModel = viewModel)
            CarInformationSummary(viewModel = viewModel)
            Spacer(modifier = Modifier.height(32.dp))
            GridCard(cardItems, viewModel)
        }
    }

    @Composable
    fun BluetoothActionComponent(viewModel: MainViewModel) {
        val bluetoothState by viewModel.bluetoothStatus.collectAsState()
        val context = LocalContext.current

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bluetoothState.description(context), style = MaterialTheme.typography.titleMedium
            )

            NeumorphicCard(
                modifier = Modifier.size(48.dp), defaultShadowOffset = 10, onClick = {
                    when (bluetoothState) {
                        BTState.CONNECTED -> viewModel.disconnect()
                        else -> viewModel.connect()
                    }
                }, cornerRadius = 50.dp
            ) {
                when (bluetoothState) {
                    BTState.CONNECTED -> {
                        Image(
                            painter = painterResource(id = R.drawable.icon_link), contentDescription = stringResource(R.string.bluetooth_connected), modifier = Modifier.size(38.dp)
                        )
                    }

                    BTState.DISCONNECTED, BTState.NOT_FOUND -> {
                        Image(
                            painter = painterResource(id = R.drawable.icon_search), contentDescription = stringResource(R.string.bluetooth_disconnected), modifier = Modifier.size(38.dp)
                        )
                    }

                    BTState.SCANNING, BTState.CONNECTING -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp, modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CarImage(viewModel: MainViewModel) {
        val bluetoothState by viewModel.bluetoothStatus.collectAsState()

        val colorFilter = remember(bluetoothState) {
            if (bluetoothState != BTState.CONNECTED) {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            } else {
                null
            }
        }

        val painter = painterResource(id = R.drawable.asset_car)

        Image(
            painter = painter, contentDescription = stringResource(R.string.car_image_description), modifier = Modifier.size(200.dp), colorFilter = colorFilter
        )
    }

    @Composable
    fun CarInformationSummary(viewModel: MainViewModel) {
        NeumorphicBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), horizontalArrangement = Arrangement.SpaceAround
            ) {
                InfoSection(
                    title = stringResource(R.string.summary_last_connect), content = viewModel.lastConnectDate.collectAsState().value
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                InfoSection(
                    title = stringResource(R.string.summary_recent_mileage), content = viewModel.recentMileage.collectAsState().value
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                InfoSection(
                    title = stringResource(R.string.summary_recent_fuel_efficiency), content = viewModel.fullEfficiency.collectAsState().value
                )
            }
        }
    }

    @Composable
    fun InfoSection(title: String, content: String, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content, style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    fun GridCard(
        cardItem: List<CardItem>, viewModel: MainViewModel
    ) {

        val coroutineScope = rememberCoroutineScope()

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(35.dp),
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            userScrollEnabled = false
        ) {
            items(cardItem.size) { index ->
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp), onClick = {
                        coroutineScope.launch {
                            viewModel.requestNavHost(cardItem[index].route)
                        }
                    }, cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cardItem[index].title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
