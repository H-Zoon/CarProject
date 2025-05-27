package com.devidea.aicar.ui.main.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.R
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.ui.main.viewmodels.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


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
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // 2) Dialog / Modal 상태 통합
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var showDeviceList by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedSave by rememberSaveable { mutableStateOf(false) }

    // 3) 상태 텍스트는 derivedStateOf 로 효율화
    val statusText by remember(bluetoothState) {
        derivedStateOf {
            when (bluetoothState) {
                ConnectionEvent.Scanning -> "검색 중..."
                ConnectionEvent.Connecting -> "연결 중..."
                ConnectionEvent.Connected -> "연결 완료"
                ConnectionEvent.Disconnected -> "연결 해제됨"
                ConnectionEvent.Error -> "연결에 실패하였습니다."
            }
        }
    }

    val context = LocalContext.current

    // 4) 스캔/연결 상태 변화 처리
    LaunchedEffect(bluetoothState) {
        showDeviceList = bluetoothState == ConnectionEvent.Scanning

        if (bluetoothState == ConnectionEvent.Connected && savedDevice == null && !hasPromptedSave) {
            showSaveDialog = true
            hasPromptedSave = true
        }
    }

    // 5) 스캔 모달
    if (showDeviceList) {
        BleDeviceListModal(
            deviceList = deviceList,
            savedDevice = savedDevice,
            requestConnect = {
                onConnect(it)
            },
            onBack = onDisconnect
        )
    }

    // 6) 저장 확인 다이얼로그
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                hasPromptedSave = true
            },
            title = { Text("기기 저장") },
            text = { Text("현재 연결된 기기를 다음에 자동 연결 기기로 저장할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    onSaveDevice()
                    showSaveDialog = false
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    hasPromptedSave = true
                }) { Text("취소") }
            }
        )
    }

    // 7) 메인 카드 UI
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            ConnectionStatusRow(state = bluetoothState, statusText = statusText)
            Spacer(Modifier.height(8.dp))
            ConnectionDeviceRow()
            Spacer(Modifier.height(8.dp))
            LastConnectionTimeRow(timeText = lastConnectionTime)
            Spacer(Modifier.height(16.dp))
            ActionButton(
                state = bluetoothState,
                enabled = bluetoothState in listOf(
                    ConnectionEvent.Disconnected,
                    ConnectionEvent.Connected
                ),
                onClick = {
                    when {
                        permissionState.allPermissionsGranted -> {
                            if (bluetoothState == ConnectionEvent.Connected) onDisconnect()
                            else onStartScan()
                        }

                        permissionState.shouldShowRationale -> showRationale = true
                        else -> permissionState.launchMultiplePermissionRequest()
                    }
                }
            )
        }
    }

    // 8) 권한 요청 라쇼날 다이얼로그
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("권한 필요") },
            text = { Text("블루투스 사용을 위해 권한이 필요합니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionState.launchMultiplePermissionRequest()
                }) { Text("다시 요청") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun ConnectionStatusRow(state: ConnectionEvent, statusText: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("블루투스 연결 상태", style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(
                        if (state == ConnectionEvent.Connected) Color.Green else Color.Gray,
                        shape = CircleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(statusText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ConnectionDeviceRow(nameText: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("연결된 기기", style = MaterialTheme.typography.bodyLarge)
        Text(
            nameText ?: "연결된 기기가 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LastConnectionTimeRow(timeText: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("마지막 연결 시간", style = MaterialTheme.typography.bodyLarge)
        Text(
            timeText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButton(
    state: ConnectionEvent,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            when (state) {
                ConnectionEvent.Connected -> "연결 해제"
                ConnectionEvent.Disconnected,
                ConnectionEvent.Error -> "기기 검색"

                ConnectionEvent.Connecting -> "연결중"
                ConnectionEvent.Scanning -> "검색중"
            }
        )
    }
}

@Composable
fun BleDeviceListModal(
    requestConnect: (ScannedDevice) -> Unit,
    savedDevice: ScannedDevice?,
    deviceList: List<ScannedDevice>,
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

            val sortedDevices = remember(deviceList, savedDevice) {
                if (savedDevice != null) {
                    // 1) 스캔된 리스트에서 saved.address와 일치하는 객체를 찾아보고
                    val matched = deviceList.find { it.address == savedDevice.address }
                    if (matched != null) {
                        // 2) 그 객체를 맨 앞에, 나머지는 뒤에
                        val others = deviceList.filter { it.address != matched.address }
                        listOf(matched) + others
                    } else {
                        // 매칭된 객체가 없으면 원본 순서 유지
                        deviceList
                    }
                } else {
                    // saved가 없으면 원본 순서 유지
                    deviceList
                }
            }

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
                    items(sortedDevices) { device ->
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
                            if (savedDevice?.address == device.address) {
                                // 저장된 기기가 스캔 결과에 포함된 순간
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "저장된 기기",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}