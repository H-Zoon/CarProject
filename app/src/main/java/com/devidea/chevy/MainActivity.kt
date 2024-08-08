package com.devidea.chevy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.codec.ToDeviceCodec
import com.devidea.chevy.codec.ToureDevCodec
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Car Project") }
                        )
                    },
                    content = { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp), // 적절한 패딩을 추가하여 UI 간격 설정
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = { BluetoothModel.connectBT() }) {
                                Text("Connect to Bluetooth") // 버튼에 텍스트 추가
                            }
                            Spacer(modifier = Modifier.height(16.dp)) // 버튼과 MyApp 사이에 간격 추가
                            MyApp(viewModel)
                        }
                    },
                    bottomBar = {
                        BottomAppBar {
                            Text("Bottom App Bar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BluetoothStatusScreen(viewModel: CarViewModel) {
    val bluetoothState by viewModel.bluetoothState.collectAsState()

    // UI to display the Bluetooth status
    Text(text = bluetoothState.description)
}

@Composable
fun BluetoothStatusScreen() {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .background(color = Color.LightGray, shape = RoundedCornerShape(8.dp))
    ) {
        BluetoothStatusSection(title = "Title 1", content = "Part 1")
        VerticalDivider(modifier = Modifier.height(20.dp))
        BluetoothStatusSection(title = "Title 2", content = "Part 2")
        VerticalDivider(modifier = Modifier.height(20.dp))
        BluetoothStatusSection(title = "Title 3", content = "Part 3")
    }
}

@Composable
fun BluetoothStatusSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BluetoothStatusScreenPreview() {
    BluetoothStatusScreen()
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
                BluetoothStatusScreen(viewModel)
                BluetoothStatusScreen()
                HomeScreen(navController)
            }
        }
        composable("details/0") { CarStatusScreen() }
        composable("details/1") {
            val context = LocalContext.current
            val intent = Intent(context, MainActivity2::class.java)
            context.startActivity(intent)
        }
        /*composable("details/{cardIndex}") { backStackEntry ->
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(4) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable {
                        navController.navigate("details/$index")
                    },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
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

