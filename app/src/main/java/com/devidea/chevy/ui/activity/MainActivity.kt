package com.devidea.chevy.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devidea.chevy.R
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.eventbus.UIEventBus
import com.devidea.chevy.eventbus.UIEvents
import com.devidea.chevy.ui.components.CardItem
import com.devidea.chevy.ui.components.LogsScreen
import com.devidea.chevy.ui.screen.dashboard.Dashboard
import com.devidea.chevy.ui.screen.map.MapEnterScreen
import com.devidea.chevy.ui.components.NeumorphicBox
import com.devidea.chevy.ui.components.NeumorphicCard
import com.devidea.chevy.ui.screen.navi.NaviScreen
import com.devidea.chevy.ui.theme.CarProjectTheme
import com.devidea.chevy.viewmodel.MainViewModel
import com.devidea.chevy.viewmodel.NaviViewModel
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_CitsGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_GuideStateDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_LocationGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_RouteGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_SafetyGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_VoiceGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideState
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import com.kakaomobility.knsdk.ui.view.KNNaviView_GuideStateDelegate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    KNGuidance_GuideStateDelegate,
    KNGuidance_SafetyGuideDelegate,
    KNGuidance_CitsGuideDelegate,
    KNGuidance_LocationGuideDelegate,
    KNGuidance_RouteGuideDelegate,
    KNGuidance_VoiceGuideDelegate,
    KNNaviView_GuideStateDelegate {

    private val viewModel: MainViewModel by viewModels()
    private val naviViewModel: NaviViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            BluetoothModel.initBTModel(this@MainActivity)
        }

        KNSDK.sharedGuidance()?.apply {
            guideStateDelegate = this@MainActivity
            locationGuideDelegate = this@MainActivity
            routeGuideDelegate = this@MainActivity
            safetyGuideDelegate = this@MainActivity
            voiceGuideDelegate = this@MainActivity
            citsGuideDelegate = this@MainActivity
        }

        setContent {
            CarProjectTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Car Project") },
                            actions = {
                                IconButton(onClick = {
                                    navController.navigate(MainViewModel.NavRoutes.Logs.route)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "View Logs"
                                    )
                                }
                            }
                        )
                    },
                    content = { paddingValues ->
                        HomeScreen(
                            navController = navController,
                            mainViewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        )
                    }
                )
            }
        }
    }

    // Delegate Methods
    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        viewModel.guidanceCheckingRouteChange(aGuidance)
    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        viewModel.guidanceDidUpdateIndoorRoute(aGuidance, aRoute)
    }

    override fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        viewModel.guidanceDidUpdateRoutes(
            aGuidance,
            aRoutes,
            aMultiRouteInfo
        )
    }

    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        viewModel.guidanceGuideEnded(aGuidance)
    }

    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        viewModel.guidanceGuideStarted(aGuidance)
    }

    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        viewModel.guidanceOutOfRoute(aGuidance)
    }

    override fun guidanceRouteChanged(
        aGuidance: KNGuidance,
        aFromRoute: KNRoute,
        aFromLocation: KNLocation,
        aToRoute: KNRoute,
        aToLocation: KNLocation,
        aChangeReason: KNGuideRouteChangeReason
    ) {
        viewModel.guidanceRouteChanged(
            aGuidance,
            aFromRoute,
            aFromLocation,
            aToRoute,
            aToLocation,
            aChangeReason
        )
    }

    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        viewModel.guidanceRouteUnchanged(aGuidance)
    }

    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        viewModel.guidanceRouteUnchangedWithError(aGuidnace, aError)
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        viewModel.didUpdateCitsGuide(aGuidance, aCitsGuide)
    }

    override fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        aLocationGuide.location?.let { naviViewModel.updateCurrentLocation(it) }
        viewModel.guidanceDidUpdateLocation(aGuidance, aLocationGuide)
    }

    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        viewModel.guidanceDidUpdateRouteGuide(aGuidance, aRouteGuide)
    }

    override fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        viewModel.guidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide)
    }

    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        viewModel.guidanceDidUpdateAroundSafeties(guidance, safeties)
        safeties?.forEach { safety ->
            Toast.makeText(
                this,
                "AroundSafety Safety type: ${safety.safetyType()}, description: ${safety.isOnStraightWay()}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ): Boolean {
        viewModel.shouldPlayVoiceGuide(aGuidance,
            aVoiceGuide,
            aNewData)
        return true
    }

    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        viewModel.willPlayVoiceGuide(aGuidance,
            aVoiceGuide)
    }

    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        viewModel.didFinishPlayVoiceGuide(aGuidance,
            aVoiceGuide)
    }

    override fun naviViewGuideEnded() {
    }

    override fun naviViewGuideState(state: KNGuideState) {
    }
}


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun HomeScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val cardItems = listOf(
        CardItem("Title 1", "This is a description", MainViewModel.NavRoutes.Details),
        CardItem("Title 2", "This is a description", MainViewModel.NavRoutes.Map),
    )

    LaunchedEffect(Unit) {
        mainViewModel.navigationEvent.collect { route ->
            route.let {
                navController.navigate(it.route)
            }
        }
    }

    NavHost(navController, startDestination = MainViewModel.NavRoutes.Home.route, modifier = modifier) {
        composable(MainViewModel.NavRoutes.Home.route) {
            // 기존의 Home 화면 컴포즈 코드
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                BluetoothActionComponent(viewModel = mainViewModel)
                CarImage(viewModel = mainViewModel)
                CarInformationSummary(viewModel = mainViewModel)
                Spacer(modifier = Modifier.height(32.dp))
                GridCard(cardItems)
            }
        }
        composable(MainViewModel.NavRoutes.Details.route) {
            Dashboard()
        }
        composable(MainViewModel.NavRoutes.Map.route) {
            MapEnterScreen()
        }
        composable(MainViewModel.NavRoutes.Nav.route) {
            NaviScreen(activity = LocalContext.current as MainActivity, guidanceEvent = mainViewModel.requestNavGuidance.value)
        }
        composable(MainViewModel.NavRoutes.Logs.route) {
            LogsScreen()
        }
    }
}

@Composable
fun BluetoothActionComponent(viewModel: MainViewModel) {
    val bluetoothState by viewModel.bluetoothStatus.collectAsState()
    val context = LocalContext.current

    val onClickAction = remember(bluetoothState) {
        when (bluetoothState) {
            BTState.CONNECTED -> {
                { BluetoothModel.disconnectBT() }
            }

            else -> {
                { BluetoothModel.connectBT() }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bluetoothState.description(context),
            style = MaterialTheme.typography.titleMedium
        )

        NeumorphicCard(
            modifier = Modifier.size(48.dp),
            defaultShadowOffset = 10,
            onClick = { onClickAction() },
            cornerRadius = 50.dp
        ) {
            when (bluetoothState) {
                BTState.CONNECTED -> {
                    Image(
                        painter = painterResource(id = R.drawable.icon_link),
                        contentDescription = stringResource(R.string.bluetooth_connected),
                        modifier = Modifier.size(38.dp)
                    )
                }

                BTState.DISCONNECTED, BTState.NOT_FOUND -> {
                    Image(
                        painter = painterResource(id = R.drawable.icon_search),
                        contentDescription = stringResource(R.string.bluetooth_disconnected),
                        modifier = Modifier.size(38.dp)
                    )
                }

                BTState.SCANNING, BTState.CONNECTING -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CarImage(viewModel: MainViewModel) {
    val bluetoothState by viewModel.bluetoothStatus.collectAsState()

    // ColorFilter는 상태에 따라 계산되고, remember를 사용하여 최적화
    val colorFilter = remember(bluetoothState) {
        if (bluetoothState != BTState.CONNECTED) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else {
            null
        }
    }

    // painterResource는 @Composable 함수이므로 remember 외부에서 호출
    val painter = painterResource(id = R.drawable.asset_car)

    Image(
        painter = painter,
        contentDescription = stringResource(R.string.car_image_description),
        modifier = Modifier.size(200.dp),
        colorFilter = colorFilter
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            InfoSection(
                title = stringResource(R.string.summary_last_connect),
                content = viewModel.lastConnectDate.collectAsState().value
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            InfoSection(
                title = stringResource(R.string.summary_recent_mileage),
                content = viewModel.recentMileage.collectAsState().value
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            InfoSection(
                title = stringResource(R.string.summary_recent_fuel_efficiency),
                content = viewModel.fullEfficiency.collectAsState().value
            )
        }
    }
}

@Composable
fun InfoSection(title: String, content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
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
fun GridCard(cardItem:List<CardItem>) {

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
                    .height(80.dp),
                onClick = {
                    coroutineScope.launch {
                       UIEventBus.post(UIEvents.reuestNavHost(cardItem[index].route))
                    }
                },
                cornerRadius = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = cardItem[index].title, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
