package com.devidea.chevy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devidea.chevy.App.Companion.instance
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.dashboard.Dashboard
import com.devidea.chevy.dashboard.LEDSeconds
import com.devidea.chevy.ui.theme.NeumorphicBox
import com.devidea.chevy.ui.theme.NeumorphicCard
import com.devidea.chevy.ui.theme.CarProjectTheme
import com.devidea.chevy.viewmodel.CarViewModel
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.v2.common.BuildConfig.VERSION_NAME
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: CarViewModel by viewModels()
    val a = CarEventModule()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BluetoothModel.initBTModel(this)

        setContent {
            CarProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    //TopAppBar(title = { Text("Car Project") })
                }, content = { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp), // 적절한 패딩을 추가하여 UI 간격 설정
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(16.dp)) // 버튼과 MyApp 사이에 간격 추가
                        HomeScreen(viewModel)
                    }
                })
            }
        }
    }
}

@Composable
fun CarImage(viewModel: CarViewModel) {
    val bluetoothState by viewModel.bluetoothState.collectAsState()

    // 상태에 따라 컬러 필터 결정
    val colorFilter = if (bluetoothState != BTState.CONNECTED) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    } else {
        null // 컬러 상태에서는 필터를 적용하지 않음
    }

    Image(
        painter = painterResource(id = R.drawable.asset_car), // 로컬 이미지 리소스 ID
        contentDescription = "Grayscale Local Image",
        modifier = Modifier.size(200.dp),
        colorFilter = colorFilter
    )
}

@Composable
fun BluetoothActionComponent(viewModel: CarViewModel) {
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val onClickAction = if (bluetoothState == BTState.CONNECTED) {
        { BluetoothModel.connectBT() }
    } else {
        { BluetoothModel.connectBT() }
    }


    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bluetoothState.description(LocalContext.current), style = MaterialTheme.typography.titleMedium
        )

        NeumorphicCard(
            modifier = Modifier
                .width(48.dp)
                .height(48.dp),
            defaultShadowOffset = 10,
            onClick = { onClickAction() },
            cornerRadius = 50.dp
        ) {
            when (bluetoothState) {
                BTState.CONNECTED -> {
                    Image(
                        painter = painterResource(id = R.drawable.icon_link),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp) // 크기를 48dp로 설정
                    )
                }

                BTState.DISCONNECTED, BTState.NOT_FOUND -> {
                    Image(
                        painter = painterResource(id = R.drawable.icon_search),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp) // 크기를 48dp로 설정
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
fun CarInformationSummary(viewModel: CarViewModel) {
    val lastDate by viewModel.lastConnectDate.collectAsState()
    val recentlyMileage by viewModel.recentMileage.collectAsState()
    val fullEfficiency by viewModel.fullEfficiency.collectAsState()
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
            BluetoothStatusSection(title = LocalContext.current.getString(R.string.summery_last_connect), content = lastDate)
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            BluetoothStatusSection(title = LocalContext.current.getString(R.string.summery_recent_mileage), content = recentlyMileage)
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            BluetoothStatusSection(title = LocalContext.current.getString(R.string.summery_recent_fuel_efficiency), content = fullEfficiency)
        }
    }
}

@Composable
fun BluetoothStatusSection(title: String, content: String) {
    Column(
        modifier = Modifier.padding(5.dp),
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
fun HomeScreen(viewModel: CarViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BluetoothActionComponent(viewModel = viewModel)
                CarImage(viewModel = viewModel)
                CarInformationSummary(viewModel = viewModel)
                Spacer(modifier = Modifier.height(32.dp))
                GridCard(navController)
            }
        }
        composable("details/0") {
            CarStatusScreen()
        }
        composable("details/1") {
            Dashboard()
        }
        composable("details/2") {

        }
        /*composable("details/{cardIndex}") { backStackEntry ->
            val cardIndex = backStackEntry.arguments?.getString("cardIndex")
            //DefaultDetailsScreen(cardIndex)
        }*/
    }
}

@Composable
fun GridCard(navController: NavHostController) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp),
        horizontalArrangement = Arrangement.spacedBy(25.dp),
        userScrollEnabled = false
    ) {
        items(4) { index ->
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                onClick = {
                    if (index == 2) {
                        Log.e("KeyHash", Utility.getKeyHash(context))
                        KNSDK.apply {
                            initializeWithAppKey(
                                aAppKey = "e31e85ed66b03658041340618628e93f", aClientVersion = "1.0.0",
                                aAppUserId = null, aLangType = KNLanguageType.KNLanguageType_KOREAN, aCompletion = {
                                    if (it != null) {
                                        when (it.code) {
                                            KNError_Code_C103 -> {
                                                Log.d("NAVI_ROTATION", "내비 인증 실패: $it")
                                                return@initializeWithAppKey
                                            }

                                            KNError_Code_C302 -> {
                                                Log.d("NAVI_ROTATION", "내비 권한 오류 : $it")
                                                return@initializeWithAppKey
                                            }

                                            else -> {
                                                Log.d("NAVI_ROTATION", "내비 초기화 실패: $it")
                                                return@initializeWithAppKey
                                            }
                                        }
                                    } else {
                                        val intent = Intent(context, NaviActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                })
                        }
                    } else if (index == 3) {
                        val intent = Intent(context, MapActivity::class.java)
                        context.startActivity(intent)
                    } else {
                        navController.navigate("details/$index")
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = "Card $index", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

