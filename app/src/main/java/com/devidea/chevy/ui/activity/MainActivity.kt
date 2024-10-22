package com.devidea.chevy.ui.activity

import android.os.Bundle
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
import androidx.compose.runtime.remember
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devidea.chevy.ui.components.CarStatus
import com.devidea.chevy.R
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.ui.screen.dashboard.Dashboard
import com.devidea.chevy.ui.screen.map.MapEnterScreen
import com.devidea.chevy.ui.components.NeumorphicBox
import com.devidea.chevy.ui.components.NeumorphicCard
import com.devidea.chevy.ui.theme.CarProjectTheme
import com.devidea.chevy.viewmodel.CarViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: CarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            BluetoothModel.initBTModel(this@MainActivity)
        }

        setContent {
            CarProjectTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { paddingValues ->
                        HomeScreen(
                            navController = navController,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    navController: NavHostController, // 외부에서 전달된 NavController 사용
    viewModel: CarViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = NavRoutes.HOME, modifier = modifier) {
        composable(NavRoutes.HOME) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                BluetoothActionComponent(viewModel = viewModel)
                CarImage(viewModel = viewModel)
                CarInformationSummary(viewModel = viewModel)
                Spacer(modifier = Modifier.height(32.dp))
                GridCard(navController)
            }
        }
        composable("${NavRoutes.DETAILS}/{cardIndex}") { backStackEntry ->
            val cardIndex = backStackEntry.arguments?.getString("cardIndex")?.toIntOrNull()
            when (cardIndex) {
                0 -> Dashboard()
                1 -> MapEnterScreen(navController = navController)
            }
        }
    }
}

@Composable
fun BluetoothActionComponent(viewModel: CarViewModel) {
    val bluetoothState by viewModel.bluetoothState.collectAsState()
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
fun CarImage(viewModel: CarViewModel) {
    val bluetoothState by viewModel.bluetoothState.collectAsState()

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
fun CarInformationSummary(viewModel: CarViewModel) {
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
fun GridCard(navController: NavHostController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp),
        horizontalArrangement = Arrangement.spacedBy(25.dp),
        userScrollEnabled = false
    ) {
        items(2) { index ->
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                onClick = { navController.navigate("details/$index") },
                cornerRadius = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Card $index", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

object NavRoutes {
    const val HOME = "home"
    const val DETAILS = "details"
}

