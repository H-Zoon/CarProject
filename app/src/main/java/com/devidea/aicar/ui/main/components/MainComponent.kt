package com.devidea.aicar.ui.main.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.RecordState
import com.devidea.aicar.storage.room.drive.DrivingDao.MonthlyStats
import com.devidea.aicar.ui.main.viewmodels.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.net.toUri
import com.devidea.aicar.service.ScannedDevice
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

const val TAG = "MainViewComponent"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val driveHistoryEnable by viewModel.driveHistoryEnable.collectAsState()
    val recordState by viewModel.recordState.collectAsStateWithLifecycle(initialValue = RecordState.Stopped)
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val lastConnection by viewModel.lastConnectDate.collectAsStateWithLifecycle()
    val devices by viewModel.devices.observeAsState(emptyList())
    val stats by viewModel.monthlyStats.collectAsState()

    val notifications by viewModel.notifications.collectAsState(initial = emptyList())

    LaunchedEffect(bluetoothState) {
        if (bluetoothState == ConnectionEvent.Connected) viewModel.setConnectTime()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "AiCar",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // 1. BadgedBox를 사용하여 IconButton을 감쌉니다.
                    BadgedBox(
                        badge = {
                            // 2. 읽지 않은 알림이 있으면 Badge를 표시합니다.
                            if (notifications.any { !it.isRead }) {
                                Badge() // 내용이 없으면 작은 점으로 표시됩니다.
                            }
                        }
                    ) {
                        // 3. 기존 IconButton은 BadgedBox의 content가 됩니다.
                        IconButton(onClick = onNotificationClick) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "알림",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DrivingStatsContent(
                    stats = stats,
                    recordState = recordState,
                    isAutoRecordEnabled = driveHistoryEnable,
                    onAutoRecordToggle = { viewModel.setAutoDrivingRecordEnable(it) }
                )
            }

            item {
                BluetoothControl(
                    bluetoothState = bluetoothState,
                    lastConnectionTime = lastConnection,
                    onStartScan = { viewModel.startScan() },
                    onConnect = { viewModel.connectTo(it) },
                    onDisconnect = { viewModel.disconnect() },
                    savedDevice = viewModel.savedDevice.collectAsState().value,
                    onSaveDevice = { viewModel.saveDevice() },
                    deviceList = devices
                )
            }
        }
    }
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    val packageName = this.packageName
    return pm.isIgnoringBatteryOptimizations(packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingStatsContent(
    stats: MonthlyStats,
    recordState: RecordState,
    isAutoRecordEnabled: Boolean,
    onAutoRecordToggle: (Boolean) -> Unit
) {
    val currentMonth: Int = remember {
        Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }
    }

    Column {
        // Header with month info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${currentMonth}월 주행 통계",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "이번 달 주행 데이터를 확인하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats cards in a grid layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(320.dp) // Fixed height to prevent scrolling issues
        ) {
            item {
                StatCard(
                    icon = Icons.Default.Route,
                    label = "총 주행 거리",
                    value = "${numberFormatter.format(stats.totalDistanceKm)} km",
                    backgroundColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1976D2)
                )
            }

            item {
                StatCard(
                    icon = Icons.Default.LocalGasStation,
                    label = "평균 연비",
                    value = "${"%.1f".format(stats.averageKPL)} km/L",
                    backgroundColor = Color(0xFFE8F5E8),
                    iconColor = Color(0xFF388E3C)
                )
            }

            item {
                StatCard(
                    icon = Icons.Default.AttachMoney,
                    label = "총 연료비",
                    value = currencyFormatter.format(stats.totalFuelCost),
                    backgroundColor = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFF57C00)
                )
            }
        }

        // Recording control section
        DrivingRecordControl(
            recordState = recordState,
            isAutoRecordEnabled = isAutoRecordEnabled,
            onAutoRecordToggle = onAutoRecordToggle
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothControl(
    bluetoothState: ConnectionEvent,
    lastConnectionTime: String,
    savedDevice: ScannedDevice?,
    deviceList: List<ScannedDevice>,
    onStartScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSaveDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionHandler(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ),
        rationaleTitle = "권한 필요",
        rationaleMessage = "블루투스 스캔 및 연결을 위해 권한이 필요합니다.",
        permanentlyDeniedTitle = "권한 영구 거절됨",
        permanentlyDeniedMessage = "블루투스 기능을 사용하려면 설정에서 권한을 허용해야 합니다."
    ) { allGranted, requestPermission ->
        var showDeviceList by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(bluetoothState) {
            showDeviceList = (bluetoothState == ConnectionEvent.Scanning)
            if (bluetoothState == ConnectionEvent.Connected) {
                onSaveDevice()
            }
        }

        if (showDeviceList) {
            BleDeviceListModal(
                deviceList = deviceList,
                savedDevice = savedDevice,
                requestConnect = { onConnect(it) },
                onBack = onDisconnect
            )
        }

        ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with Bluetooth icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "블루투스 연결",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Connection status indicator
                    ConnectionStatusBadge(bluetoothState)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Connection details card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Device info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "연결된 기기",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = savedDevice?.name ?: "연결된 기기가 없습니다",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (savedDevice != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            if (savedDevice != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (bluetoothState == ConnectionEvent.Connected)
                                        Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
                                ) {
                                    Icon(
                                        imageVector = if (bluetoothState == ConnectionEvent.Connected)
                                            Icons.Default.CheckCircle else Icons.Default.DeviceHub,
                                        contentDescription = null,
                                        tint = if (bluetoothState == ConnectionEvent.Connected)
                                            Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }

                        if (lastConnectionTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "마지막 연결: $lastConnectionTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action button
                Button(
                    onClick = {
                        if (allGranted) {
                            if (bluetoothState == ConnectionEvent.Connected) {
                                onDisconnect()
                            } else {
                                onStartScan()
                            }
                        } else {
                            requestPermission()
                        }
                    },
                    enabled = bluetoothState != ConnectionEvent.Connecting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (bluetoothState) {
                            ConnectionEvent.Connected -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = when (bluetoothState) {
                            ConnectionEvent.Connected -> Icons.Default.BluetoothDisabled
                            ConnectionEvent.Scanning -> Icons.Default.BluetoothSearching
                            ConnectionEvent.Connecting -> Icons.Default.Bluetooth
                            else -> Icons.Default.BluetoothSearching
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (bluetoothState) {
                            ConnectionEvent.Connected -> "연결 해제"
                            ConnectionEvent.Connecting -> "연결중..."
                            ConnectionEvent.Scanning -> "검색중..."
                            ConnectionEvent.Error -> "재시도"
                            else -> "기기 검색"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status message
                if (bluetoothState == ConnectionEvent.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "연결에 실패했습니다. 다시 시도해주세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBadge(bluetoothState: ConnectionEvent) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when (bluetoothState) {
            ConnectionEvent.Connected -> Color(0xFFE8F5E8)
            ConnectionEvent.Connecting -> Color(0xFFFFF3E0)
            ConnectionEvent.Scanning -> Color(0xFFE3F2FD)
            ConnectionEvent.Error -> Color(0xFFFFEBEE)
            else -> Color(0xFFF5F5F5)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = when (bluetoothState) {
                            ConnectionEvent.Connected -> Color(0xFF4CAF50)
                            ConnectionEvent.Connecting -> Color(0xFFFF9800)
                            ConnectionEvent.Scanning -> Color(0xFF2196F3)
                            ConnectionEvent.Error -> Color(0xFFF44336)
                            else -> Color(0xFF9E9E9E)
                        },
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (bluetoothState) {
                    ConnectionEvent.Connected -> "연결됨"
                    ConnectionEvent.Connecting -> "연결중"
                    ConnectionEvent.Scanning -> "검색중"
                    ConnectionEvent.Error -> "오류"
                    else -> "대기중"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = when (bluetoothState) {
                    ConnectionEvent.Connected -> Color(0xFF2E7D32)
                    ConnectionEvent.Connecting -> Color(0xFFE65100)
                    ConnectionEvent.Scanning -> Color(0xFF1565C0)
                    ConnectionEvent.Error -> Color(0xFFC62828)
                    else -> Color(0xFF616161)
                }
            )
        }
    }
}

@Composable
fun BleDeviceListModal(
    requestConnect: (ScannedDevice) -> Unit,
    savedDevice: ScannedDevice?,
    deviceList: List<ScannedDevice>,
    onBack: () -> Unit
) {
    Dialog(onDismissRequest = onBack) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(0.7f),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            val sortedDevices = remember(deviceList, savedDevice) {
                if (savedDevice != null) {
                    val matched = deviceList.find { it.address == savedDevice.address }
                    if (matched != null) {
                        val others = deviceList.filter { it.address != matched.address }
                        listOf(matched) + others
                    } else {
                        deviceList
                    }
                } else {
                    deviceList
                }
            }

            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "기기 선택",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Device list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedDevices.size) { device ->
                        DeviceListItem(
                            device = sortedDevices[device],
                            isSelected = savedDevice?.address == sortedDevices[device].address,
                            onClick = { requestConnect(sortedDevices[device]) }
                        )
                    }

                    if (sortedDevices.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(), // LazyColumn의 전체 크기를 채움
                                contentAlignment = Alignment.Center      // 내부 요소를 중앙에 정렬
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BluetoothDisabled,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "검색된 기기가 없습니다",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "기기를 검색 가능 모드로 설정하고\n다시 시도해주세요",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: ScannedDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(10.dp)
                        .size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "선택됨",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DrivingRecordControl(
    recordState: RecordState,
    isAutoRecordEnabled: Boolean,
    onAutoRecordToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 포그라운드 권한 상태
    val fgPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    // 백그라운드 위치 권한 상태 (Android Q 이상)
    val bgLocationState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    // 다이얼로그 상태
    var showFgRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showFgPermanentlyDeniedDialog by rememberSaveable { mutableStateOf(false) }
    var showBgPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var showBatteryOptDialog by rememberSaveable { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "주행 기록 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = when (recordState) {
                    is RecordState.Recording -> Color(0xFFE8F5E8)
                    is RecordState.Pending -> Color(0xFFFFF3E0)
                    is RecordState.Stopped -> Color(0xFFF5F5F5)
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (recordState) {
                            is RecordState.Recording -> Icons.Default.FiberManualRecord
                            is RecordState.Pending -> Icons.Default.Pending
                            is RecordState.Stopped -> Icons.Default.Stop
                        },
                        contentDescription = null,
                        tint = when (recordState) {
                            is RecordState.Recording -> Color(0xFF4CAF50)
                            is RecordState.Pending -> Color(0xFFFF9800)
                            is RecordState.Stopped -> Color(0xFF9E9E9E)
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = when (recordState) {
                            is RecordState.Recording -> "주행 기록 중"
                            is RecordState.Pending -> "기록 대기 중"
                            is RecordState.Stopped -> "주행 기록 중지됨"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (recordState) {
                            is RecordState.Recording -> Color(0xFF2E7D32)
                            is RecordState.Pending -> Color(0xFFE65100)
                            is RecordState.Stopped -> Color(0xFF616161)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto record toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "자동 기록 활성화",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "블루투스 연결 시 자동 시작",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Switch(
                    checked = isAutoRecordEnabled,
                    onCheckedChange = {
                        // 1) 포그라운드 권한 처리
                        if (!fgPermissionsState.allPermissionsGranted) {
                            val anyShouldRationale = fgPermissionsState.permissions.any {
                                !it.status.isGranted && it.status.shouldShowRationale
                            }
                            val anyPermanentlyDenied = fgPermissionsState.permissions.any {
                                !it.status.isGranted && !it.status.shouldShowRationale
                            }
                            when {
                                //anyPermanentlyDenied -> showFgPermanentlyDeniedDialog = true
                                anyShouldRationale -> showFgRationaleDialog = true
                                else -> fgPermissionsState.launchMultiplePermissionRequest()
                            }
                            return@Switch
                        }

                        // 2) 백그라운드 위치 권한 처리 (Android Q 이상)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !bgLocationState.status.isGranted) {
                            // 설명 다이얼로그 노출 후 설정 화면으로 이동
                            showBgPermissionDialog = true
                            return@Switch
                        }

                        // 3) 배터리 최적화 예외 확인
                        if (!context.isIgnoringBatteryOptimizations()) {
                            showBatteryOptDialog = true
                        } else {
                            onAutoRecordToggle(it)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }


        // 포그라운드 권한: 이유 설명
        if (showFgRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showFgRationaleDialog = false },
                title = { Text(text = "권한 필요") },
                text = { Text(text = "앱 기능을 위해 알림 및 위치 권한이 필요합니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        showFgRationaleDialog = false
                        fgPermissionsState.launchMultiplePermissionRequest()
                    }) { Text(text = "다시 요청") }
                },
                dismissButton = {
                    TextButton(onClick = { showFgRationaleDialog = false }) { Text(text = "취소") }
                }
            )
        }

        // 포그라운드 권한: 영구 거부
        if (showFgPermanentlyDeniedDialog) {
            AlertDialog(
                onDismissRequest = { showFgPermanentlyDeniedDialog = false },
                title = { Text(text = "권한 영구 거절됨") },
                text = { Text(text = "앱 설정에서 권한을 허용해야 합니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        showFgPermanentlyDeniedDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }) { Text(text = "설정으로 이동") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showFgPermanentlyDeniedDialog = false
                    }) { Text(text = "취소") }
                }
            )
        }

        // 백그라운드 위치 권한: 설정 유도
        if (showBgPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showBgPermissionDialog = false },
                title = { Text(text = "백그라운드 위치 권한 필요") },
                // 사용자에게 어떤 행동을 해야하는지 구체적으로 안내
                text = {
                    Text(
                        text = "정확한 기록을 위해 백그라운드 위치 권한이 필요합니다.\n\n" +
                                "설정 화면으로 이동 후, [권한] > [위치] 메뉴에서 '항상 허용'을 선택해주세요."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBgPermissionDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }) { Text(text = "설정으로 이동") }
                },
                dismissButton = {
                    TextButton(onClick = { showBgPermissionDialog = false }) { Text(text = "취소") }
                }
            )
        }

        // 배터리 최적화 예외 다이얼로그
        if (showBatteryOptDialog) {
            AlertDialog(
                onDismissRequest = { showBatteryOptDialog = false },
                title = { Text(text = "배터리 최적화 예외 필요") },
                text = {
                    Text(
                        text = "백그라운드에서 주행 기록 기능이 정상 작동하려면, 시스템의 배터리 최적화에서 예외로 설정해야 합니다."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBatteryOptDialog = false
                        val intent =
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        context.startActivity(intent)
                    }) { Text(text = "설정으로 이동") }
                },
                dismissButton = {
                    TextButton(onClick = { showBatteryOptDialog = false }) { Text(text = "취소") }
                }
            )
        }
    }
}
