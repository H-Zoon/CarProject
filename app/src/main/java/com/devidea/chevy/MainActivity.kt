package com.devidea.chevy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.ui.neumorphic.NeumorphicBox
import com.devidea.chevy.ui.neumorphic.NeumorphicCard
import com.devidea.chevy.ui.theme.CarProjectTheme
import com.devidea.chevy.viewmodel.CarViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var viewModel: CarViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BluetoothModel.initBTModel(this)

        setContent {
            CarProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    TopAppBar(title = { Text("Car Project") })
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
                        MyApp(viewModel)
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
    val colorFilter = if (bluetoothState.state != 1) {
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
    val onClickAction = if (bluetoothState.state == 1) {
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
            text = bluetoothState.description, style = MaterialTheme.typography.headlineLarge
        )



        val icon = when (bluetoothState.state) {
            1 -> Icons.Default.Done
                /*IconButton(
                    onClick = { BluetoothModel.disconnectBT() },
                    modifier = Modifier
                ) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = arrowImage, // 아이콘 리소스 사용
                        contentDescription = "Icon Button",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }*/


            0, 4 -> Icons.Default.Search

            2, 3 -> Icons.Default.Build
                /*CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )*/
            else -> Icons.Default.Build
        }

        Icons.Default.Search

        NeumorphicCard(
            modifier = Modifier
                .width(48.dp)
                .height(48.dp),
            onClick = { onClickAction() },
            cornerRadius = 50.dp
        ) {
            IconButton(
                onClick = { BluetoothModel.disconnectBT() },
                modifier = Modifier
            ) {
                Icon(
                    modifier = Modifier.size(30.dp),
                    imageVector = icon, // 아이콘 리소스 사용
                    contentDescription = "Icon Button",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CarInformationSummary() {
    NeumorphicBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BluetoothStatusSection(title = "Title 1", content = "Part 1")
            VerticalDivider(modifier = Modifier.height(20.dp))
            BluetoothStatusSection(title = "Title 2", content = "Part 2")
            VerticalDivider(modifier = Modifier.height(20.dp))
            BluetoothStatusSection(title = "Title 3", content = "Part 3")
        }
    }
}

@Composable
fun BluetoothStatusSection(title: String, content: String) {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = content, style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BluetoothStatusScreenPreview() {
    CarInformationSummary()
}

@Composable
fun MyApp(viewModel: CarViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BluetoothActionComponent(viewModel = viewModel)
                CarImage(viewModel = viewModel)
                CarInformationSummary()
                HomeScreen(navController)
            }
        }
        composable("details/0") { CarStatusScreen() }
        composable("details/1") {
            val context = LocalContext.current
            val intent = Intent(context, MainActivity2::class.java)
            context.startActivity(intent)
        }/*composable("details/{cardIndex}") { backStackEntry ->
            val cardIndex = backStackEntry.arguments?.getString("cardIndex")
            //DefaultDetailsScreen(cardIndex)
        }*/
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(4) { index ->
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                onClick = { navController.navigate("details/$index") }
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val viewModel = CarViewModel() // 임시 ViewModel 인스턴스 생성
    CarProjectTheme {
        MyApp(viewModel = viewModel)
    }
}

