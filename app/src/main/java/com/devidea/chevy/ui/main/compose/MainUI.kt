package com.devidea.chevy.ui.main.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.R
import com.devidea.chevy.service.ConnectionEvent
import com.devidea.chevy.service.ScannedDevice
import com.devidea.chevy.ui.main.MainViewModel

// 탭 종류 정의
sealed class NavItem(val label: String, val icon: ImageVector) {
    object Home : NavItem("Home", Icons.Filled.Home)
    object Location : NavItem("Location", Icons.Filled.Place)
    object Car : NavItem("Car", Icons.Filled.DirectionsCar)
    object Settings : NavItem("Settings", Icons.Filled.Settings)
}

@Composable
fun CarManagementMainScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf<NavItem>(NavItem.Home) }

    Scaffold(
        topBar = { if (selectedTab == NavItem.Home) BluetoothActionComponent(viewModel = viewModel) },
        bottomBar = {
            CarBottomNavBar(
                items = listOf(NavItem.Home, NavItem.Location, NavItem.Car, NavItem.Settings),
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
                is NavItem.Location -> MAuthenticationScreen()//LocationScreen()
                is NavItem.Car -> DiagnosticScreen()//CarContent()
                is NavItem.Settings -> {}//SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    CenterAlignedTopAppBar(
        title = { Text("My Car", style = MaterialTheme.typography.titleLarge) },
        actions = {
            IconButton(onClick = { /* 알림 클릭 */ }) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun BluetoothActionComponent(viewModel: MainViewModel) {
    val bluetoothState by viewModel.events.collectAsState(ConnectionEvent.Disconnected)
    val context = LocalContext.current

    if (bluetoothState == ConnectionEvent.Scanning) {
        //BleDeviceListModal(viewModel, requestScan = {viewModel.startScan()}, requestConnect = {viewModel.connectTo(it)} }, onBack = {viewModel.disconnect()})
        BleDeviceListModal(viewModel = viewModel, requestScan = {}, requestConnect = {viewModel.connectTo(it)}, onBack = {})
    }

    var statusText by remember { mutableStateOf("준비됨") }

    // 2) 이벤트 수집
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { evt ->
            statusText = when (evt) {
                ConnectionEvent.Scanning    -> "검색 중..."
                ConnectionEvent.Connecting  -> "연결 중..."
                ConnectionEvent.Connected   -> "연결 완료"
                ConnectionEvent.Disconnected-> "연결 해제됨"
                is ConnectionEvent.Error    -> "오류: ${evt.message}"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium
        )

        ElevatedButton(
            onClick = {
                when (bluetoothState) {
                    ConnectionEvent.Connecting -> viewModel.disconnect()
                    else -> viewModel.startScan()
                }
            },
            modifier = Modifier
                .size(48.dp)
                .shadow(10.dp, shape = RoundedCornerShape(50.dp)), // 기본 그림자 오프셋 10dp
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface // 원하시면 다른 색으로 변경 가능
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            when (bluetoothState) {
                ConnectionEvent.Connected -> {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_link),
                        contentDescription = stringResource(R.string.bluetooth_connected),
                        modifier = Modifier.size(38.dp)
                    )
                }

                ConnectionEvent.Disconnected -> {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_search),
                        contentDescription = stringResource(R.string.bluetooth_disconnected),
                        modifier = Modifier.size(38.dp)
                    )
                }

                ConnectionEvent.Scanning, ConnectionEvent.Connecting -> {
                    CircularProgressIndicator(
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ConnectionEvent.Error -> {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_search),
                        contentDescription = stringResource(R.string.bluetooth_disconnected),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BleDeviceListModal(
    viewModel: MainViewModel,
    requestScan: () -> Unit,
    requestConnect: (ScannedDevice) -> Unit,
    onBack: () -> Unit
) {
    // Dialog를 사용해 화면 중앙에 모달처럼 띄움
    Dialog(onDismissRequest = onBack) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            val devices by viewModel.devices.observeAsState(emptyList())

            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "블루투스 기기 선택",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기"
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 기기 리스트
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)  // 너무 길어지지 않도록 제한
                ) {
                    items(devices) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { requestConnect(device) })
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = device.name ?: "Known",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FuelGauge(currentPercent = 0.76f)
        Spacer(modifier = Modifier.height(24.dp))
        ActionButtons(viewModel)
        Spacer(modifier = Modifier.height(24.dp))
        MaintenanceSection(
            items = listOf(
                MaintenanceItem("Oil Change", "Due in 500 km"),
                MaintenanceItem("Tire Rotation", "Apr 25")
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        TripHistorySection(
            trips = listOf(
                TripItem("Apr 20", "58.3 km", "Seoul"),
                TripItem("Apr 15", "231.6 km", "Incheon")
            )
        )
    }
}

@Composable
fun FuelGauge(currentPercent: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            progress = currentPercent,
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape),
            strokeWidth = 12.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(currentPercent * 100).toInt()}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Fuel",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ActionButtons(viewModel: MainViewModel) {
    val carState by viewModel.snapshot.collectAsState()
    val gmState by viewModel.snapshot2.collectAsState()

    Column {
        Text(text = "rpm: ${carState.rpm}")
        Text(text = "speed: ${carState.speed}")
        Text(text = "ect: ${carState.ect}")
        Text(text = "throttle: ${carState.throttle}")
        Text(text = "load: ${carState.load}")
        Text(text = "iat: ${carState.iat}")
        Text(text = "maf: ${carState.maf}")
        Text(text = "batt: ${carState.batt}")
        Text(text = "fuelRate: ${carState.fuelRate}")
    }

    /*Column {
        Text(text = "oilLife: ${gmState.oilLife}")
        Text(text = "transTemp: ${gmState.transTemp}")
        Text(text = "oilTemp: ${gmState.oilTemp}")
        Text(text = "batt12v: ${gmState.batt12v}")
        Text(text = "fuelUsedMl: ${gmState.fuelUsedMl}")
        Text(text = "chargeCurrent: ${gmState.chargeCurrent}")
        Text(text = "gearPos: ${gmState.gearPos}")
        Text(text = "outsideTemp: ${gmState.outsideTemp}")
        Text(text = "transPressure: ${gmState.transPressure}")
    }*/

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { viewModel.startInfo() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = "Lock")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lock")
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(
            onClick = { viewModel.stopInfo() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.LockOpen, contentDescription = "Unlock")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlock")
        }
    }
}

data class MaintenanceItem(val title: String, val subtitle: String)

@Composable
fun MaintenanceSection(items: List<MaintenanceItem>) {
    Text(
        text = "Maintenance",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column {
        items.forEach { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class TripItem(val date: String, val distance: String, val location: String)

@Composable
fun TripHistorySection(trips: List<TripItem>) {
    Text(
        text = "Trip History",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column {
        trips.forEach { trip ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trip.date,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = trip.distance,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trip.location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
    MaterialTheme {
        //CarManagementMainScreen()
    }
}
