package com.devidea.chevy.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devidea.chevy.App.Companion.CHANNEL_ID
import com.devidea.chevy.R
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.service.BleServiceManager
import com.devidea.chevy.ui.components.CardItem
import com.devidea.chevy.ui.screen.dashboard.Dashboard
import com.devidea.chevy.ui.main.compose.MAuthenticationScreen
import com.devidea.chevy.ui.components.NeumorphicBox
import com.devidea.chevy.ui.components.NeumorphicCard
import com.devidea.chevy.ui.components.PermissionRequestScreen
import com.devidea.chevy.ui.main.compose.CarManagementMainScreen
import com.devidea.chevy.ui.navi.NavigateData
import com.devidea.chevy.ui.navi.KNavi
import com.devidea.chevy.ui.theme.CarProjectTheme
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
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
        //enableEdgeToEdge()
        val splashScreen = installSplashScreen()

        /*splashScreen.setOnExitAnimationListener { splashScreenView ->
            // 아이콘 애니메이션의 전체 지속시간과 시작 시간을 가져옵니다.
            val iconAnimationDuration = splashScreenView.iconAnimationDurationMillis
            val iconAnimationStart = splashScreenView.iconAnimationStartMillis
            val iconView = splashScreenView.iconView
            val remainingDuration = (iconAnimationDuration - (iconAnimationStart - System.currentTimeMillis())).coerceAtLeast(0L)
            iconView.animate().translationY(iconView.height.toFloat()).alpha(0f).setStartDelay(remainingDuration).setDuration(500L).withEndAction {
                    splashScreenView.remove()
                }.start()
        }

        // 스플래시 화면을 조금 더 유지하고 싶다면 setKeepOnScreenCondition 이용
        splashScreen.setKeepOnScreenCondition {
            false
        }*/

        setContent {
            CarProjectTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize(), content = { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        CarManagementMainScreen()
                        //MainNavHost(navController = navController, mainViewModel = viewModel)
                    }
                })
            }
        }
    }

    @Composable
    fun MainNavHost(
        navController: NavHostController, mainViewModel: MainViewModel
    ) {
        NavHost(
            navController = navController, startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(mainViewModel, navController)
            }
            composable("details") {
                Dashboard()
            }
            composable("map") {
                MAuthenticationScreen(viewModel = viewModel)
            }
            composable("navi") { backStackEntry ->
                // SavedStateHandle에서 데이터를 가져옴
                val knTrip = backStackEntry.savedStateHandle.get<KNTrip>("knTrip")
                val curRoutePriority = backStackEntry.savedStateHandle.get<KNRoutePriority>("curRoutePriority")
                    ?: KNRoutePriority.KNRoutePriority_Recommand
                val curAvoidOptions = backStackEntry.savedStateHandle.get<KNRouteAvoidOption>("curAvoidOptions")
                    ?: KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent

                // SafetyScreen에 데이터를 전달
                kNavi.Navigation(null, navController = navController)
            }

            composable("findLoad") { backStackEntry ->
                val aaaInstance = navController.previousBackStackEntry?.savedStateHandle?.get<NavigateData>("aaaKey")
                if (aaaInstance != null) {
                    //kNavi.LoadRequestScreen(aaaInstance, navController)
                }
            }
        }
    }

    @Composable
    fun HomeScreen(
        viewModel: MainViewModel, navController: NavHostController
    ) {
        // 권한 승인 상태를 저장하는 상태 변수
        var permissionsGranted by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        // 권한이 아직 승인되지 않은 경우 PermissionRequestScreen을 보여줌
        if (!permissionsGranted) {
            PermissionRequestScreen(mainViewModel = viewModel, onAllRequiredPermissionsGranted = {
                coroutineScope.launch {
                    val serviceChannel = NotificationChannel(
                        CHANNEL_ID, "LeBluetoothService Channel", NotificationManager.IMPORTANCE_DEFAULT
                    )
                    getSystemService(NotificationManager::class.java).createNotificationChannel(
                        serviceChannel
                    )
                    serviceManager.bindService()
                    // 상태 업데이트: 권한이 승인되었으므로 메인 화면(Column)을 보여줌
                    permissionsGranted = true
                }
            }, onPermissionDenied = {
                permissionsGranted = false
            })
        } else {
            // 권한이 모두 승인된 경우 메인 화면을 보여줌
            val cardItems = listOf(
                CardItem("차량정보", "This is a description", "details"), CardItem("네비게이션", "This is a description", "map")
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                BluetoothActionComponent(viewModel = viewModel)
                CarImage(viewModel = viewModel)
                CarInformationSummary(viewModel = viewModel)
                Spacer(modifier = Modifier.height(32.dp))
                GridCard(cardItems = cardItems, navController = navController)
            }
        }
    }

    /*@Composable
    fun HomeScreen(viewModel: MainViewModel, cardItems: List<CardItem>, navController: NavHostController) {
        PermissionRequestScreen(viewModel,
            onAllRequiredPermissionsGranted = {
                val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "LeBluetoothService Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                getSystemService(NotificationManager::class.java).createNotificationChannel(
                    serviceChannel
                )
                serviceManager.bindService()
            },
            onPermissionDenied = {
            }
        )

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
            GridCard(cardItems, viewModel, navController)
        }
    }*/

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
                        else -> {
                            viewModel.connect()
                        }
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
        cardItems: List<CardItem>, navController: NavHostController
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(35.dp),
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            userScrollEnabled = false
        ) {
            items(cardItems.size) { index ->
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp), onClick = {
                        navController.navigate(cardItems[index].route)
                    }, cornerRadius = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cardItems[index].title, style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
