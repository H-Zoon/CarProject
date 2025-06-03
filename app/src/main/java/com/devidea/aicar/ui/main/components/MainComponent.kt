package com.devidea.aicar.ui.main.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(bluetoothState) {
        if(bluetoothState == ConnectionEvent.Connected) viewModel.setConnectTime()
    }

    val scrollState = rememberScrollState()


    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Car", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onNotificationClick) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            DrivingStatsContent(stats = stats,
                recordState = recordState,
                isAutoRecordEnabled = driveHistoryEnable,
                onAutoRecordToggle = {viewModel.setAutoDrivingRecordEnable(it)},)

            Spacer(modifier = Modifier.height(12.dp))
            BluetoothControl(
                bluetoothState = bluetoothState,
                lastConnectionTime  = lastConnection,
                onStartScan = { viewModel.startScan() },
                onConnect = { viewModel.connectTo(it) },
                onDisconnect = { viewModel.disconnect() },
                savedDevice = viewModel.savedDevice.collectAsState().value,
                onSaveDevice = { viewModel.saveDevice() },
                deviceList = devices
            )
            Spacer(modifier = Modifier.height(12.dp))
            /*DrivingRecordControl(
                recordState = recordState,
                onRecordToggle = { viewModel.setManualDrivingRecordToggle() },
                isAutoRecordEnabled = driveHistoryEnable,
                onAutoRecordToggle = {viewModel.setAutoDrivingRecordEnable(it)},
                bluetoothState = bluetoothState
            )*/
        }
    }
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    val packageName = this.packageName
    return pm.isIgnoringBatteryOptimizations(packageName)
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DrivingRecordControl(
    recordState: RecordState,
    onRecordToggle: () -> Unit,
    isAutoRecordEnabled: Boolean,
    onAutoRecordToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    bluetoothState: ConnectionEvent
) {
    val context = LocalContext.current

    // 0) 배터리 최적화 예외 상태 확인
    val isIgnoringBatteryOpt by remember {
        // Compose 재구성(recompose) 시에 원한다면 다시 체크하고 싶다면 rememberUpdatedState 등으로 바꿔도 됩니다.
        mutableStateOf(context.isIgnoringBatteryOptimizations())
    }

    // 1) 알림 권한 요청은 기존처럼 PermissionHandler에 위임
    PermissionHandler(
        permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
        rationaleTitle = "알림 권한 필요",
        rationaleMessage = "포그라운드 알림을 보내려면 알림 권한이 필요합니다.",
        permanentlyDeniedTitle = "권한 영구 거절됨",
        permanentlyDeniedMessage = "알림 기능을 사용하려면 설정에서 권한을 허용해야 합니다."
    ) { allGranted, requestPermission ->
        // 2) 배터리 최적화 다이얼로그 상태
        var showBatteryOptDialog by rememberSaveable { mutableStateOf(false) }

        // 3) 만약 자동 또는 수동 주행 기록을 시작할 때, 배터리 최적화가 걸려있으면 우선 안내 다이얼로그부터 띄워야 한다고 가정
        //    > 실제 앱 로직에 따라 다르게 배치할 수 있습니다(예: 화면 진입 시 바로 확인 or 토글 시점에 확인).

        // 예시: 사용자가 “자동 토글” 또는 “수동 기록 시작” 버튼을 누를 때 배터리 최적화 상태가 true(켜져있음)이면 dialog를 띄우도록.
        // -> UI 내 버튼 onCheckedChange / onClick 시점에 아래 코드를 추가

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 기존 “주행 기록 상태” Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "주행 기록 상태",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (recordState == RecordState.Recording) Color.Red else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (recordState) {
                                is RecordState.Recording -> "주행 기록중"
                                is RecordState.Pending   -> "기록 대기중"
                                is RecordState.Stopped   -> "주행 기록 취소"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // 자동 주행 기록 토글 Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "자동 기록 설정",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isAutoRecordEnabled,
                        onCheckedChange = { newValue ->
                            if (!allGranted) {
                                // 알림 권한이 없으면 권한 요청 먼저
                                requestPermission()
                                return@Switch
                            }
                            // 알림 권한이 있으면, 배터리 최적화 상태를 확인
                            if (!context.isIgnoringBatteryOptimizations()) {
                                // 배터리 최적화가 켜져있다면(즉, 예외되지 않은 상태),
                                // 토글 로직을 실행하기 전에 다이얼로그를 띄움
                                showBatteryOptDialog = true
                            } else {
                                // 배터리 최적화 예외 상태이면 바로 자동 토글 로직 실행
                                onAutoRecordToggle(newValue)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 수동 주행 기록 버튼
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAutoRecordEnabled && bluetoothState == ConnectionEvent.Connected,
                    onClick = {
                        if (!allGranted) {
                            // 알림 권한이 없으면 먼저 권한 요청
                            requestPermission()
                            return@Button
                        }
                        // 알림 권한이 있으면, 배터리 최적화 상태 확인
                        if (!context.isIgnoringBatteryOptimizations()) {
                            // 배터리 최적화 켜져있으면 다이얼로그 띄움
                            showBatteryOptDialog = true
                        } else {
                            // 이미 예외 상태이면 바로 수동 토글 로직 수행
                            onRecordToggle()
                        }
                    }
                ) {
                    Text(
                        text = when (recordState) {
                            is RecordState.Recording, is RecordState.Pending -> "수동기록 중지"
                            else -> "수동기록 시작"
                        }
                    )
                }

                // --- 4) “배터리 최적화 해제 요청” 다이얼로그 ---
                if (showBatteryOptDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatteryOptDialog = false },
                        title = { Text(text = "배터리 최적화 예외 필요") },
                        text = {
                            Text(
                                text = "백그라운드에서 주행 기록 기능이 정상 작동하려면, 시스템의 배터리 최적화(Doze 모드)에서 예외로 설정해야 합니다.\n\n" +
                                        "지금 설정 화면으로 이동하시겠습니까?"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showBatteryOptDialog = false
                                // 5) 배터리 최적화 예외 요청 인텐트 시작
                                val intentRequest = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intentRequest)
                            }) {
                                Text(text = "설정으로 이동")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatteryOptDialog = false }) {
                                Text(text = "취소")
                            }
                        }
                    )
                }
            }
        }
    }
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

    // 숫자를 천 단위로 콤마 찍거나 통화 포맷으로 보여주기 위한 포맷터
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            // Local currency가 KRW(₩)가 아니면 Locale("ko", "KR") 식으로 바꿔도 됩니다.
            // 예: NumberFormat.getCurrencyInstance(Locale("ko", "KR"))
            maximumFractionDigits = 0 // 소수점 없는 원단위
        }
    }

    // 메인 화면을 Column으로 감싸고, 중앙 정렬 + 패딩 적용
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            text = "${currentMonth}월 주행 통계",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 카드 형태로 통계 데이터를 그룹화
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {

                DrivingRecordControl2(
                    recordState = recordState,
                    isAutoRecordEnabled = isAutoRecordEnabled,
                    onAutoRecordToggle = onAutoRecordToggle
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 3.dp
                )

                // 총 주행 거리
                StatRow(
                    label = "총 주행 거리",
                    value = "${numberFormatter.format(stats.totalDistanceKm)} km"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp
                )

                // 평균 연비
                StatRow(
                    label = "평균 연비",
                    value = "${"%.2f".format(stats.averageKPL)} km/L"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp
                )

                // 총 연료비
                StatRow(
                    label = "총 연료비",
                    // currencyFormatter가 예: "₩1,234,000" 형태로 만들어줌
                    value = currencyFormatter.format(stats.totalFuelCost)
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

