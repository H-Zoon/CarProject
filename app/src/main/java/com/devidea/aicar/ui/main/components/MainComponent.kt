package com.devidea.aicar.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.aicar.drive.usecase.RecordState
import com.devidea.aicar.ui.main.viewmodels.MainViewModel

const val TAG = "MainViewComponent"

@OptIn(ExperimentalMaterial3Api::class)
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
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
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
            Spacer(modifier = Modifier.height(24.dp))
            DrivingRecordControl(
                recordState = recordState,
                onRecordToggle = { viewModel.toggleRecording() },
                isAutoRecordEnabled = driveHistoryEnable,
                onAutoRecordToggle = { viewModel.setDrivingHistory(it) }
            )
        }
    }
}

@Composable
fun DrivingRecordControl(
    recordState: RecordState,
    onRecordToggle: () -> Unit,
    isAutoRecordEnabled: Boolean,
    onAutoRecordToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth(),                       // 가로 전체 폭 차지
                horizontalArrangement = Arrangement.SpaceBetween,  // 양 끝으로 요소 배치
                verticalAlignment = Alignment.CenterVertically     // 필요에 따라 수직 정렬
            ) {
                Text(
                    text = "주행 기록 상태",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            is RecordState.Pending -> "기록 대기중"
                            is RecordState.Stopped -> "주행 기록 취소"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // 자동 주행 기록 토글
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
                    onCheckedChange = onAutoRecordToggle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAutoRecordEnabled,
                onClick = onRecordToggle
            ) {
                Text(text =
                    when (recordState){
                        is RecordState.Recording,  is RecordState.Pending -> "수동기록 중지"
                        else -> "수동기록 시작"
                    }
                )
            }
        }
    }
}

