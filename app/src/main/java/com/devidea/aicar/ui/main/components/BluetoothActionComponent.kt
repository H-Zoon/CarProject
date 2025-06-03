package com.devidea.aicar.ui.main.components

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.ScannedDevice
import com.google.accompanist.permissions.ExperimentalPermissionsApi


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
    // PermissionHandler 래핑
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
        // allGranted == true 이면 이미 권한을 획득한 상태
        // requestPermission() 호출 시점에 “rationale 다이얼로그 / 시스템 팝업 / 설정 이동 다이얼로그”가 자동 처리됨

        // 1) 스캔 모달 제어 상태
        var showDeviceList by rememberSaveable { mutableStateOf(false) }

        // 2) 블루투스 상태 변화 처리 (기존 로직 재사용)
        LaunchedEffect(bluetoothState) {
            showDeviceList = (bluetoothState == ConnectionEvent.Scanning)
            if (bluetoothState == ConnectionEvent.Connected) {
                onSaveDevice()
            }
        }

        // 3) 스캔 모달
        if (showDeviceList) {
            BleDeviceListModal(
                deviceList = deviceList,
                savedDevice = savedDevice,
                requestConnect = { onConnect(it) },
                onBack = onDisconnect
            )
        }

        // 4) 메인 카드 UI
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                // 연결 상태
                ConnectionStatusRow(
                    state = bluetoothState,
                    statusText = when (bluetoothState) {
                        ConnectionEvent.Scanning -> "검색 중..."
                        ConnectionEvent.Connecting -> "연결 중..."
                        ConnectionEvent.Connected -> "연결 완료"
                        ConnectionEvent.Idle -> "연결 대기중"
                        ConnectionEvent.Error -> "연결에 실패하였습니다."
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

                // 저장된 기기 보여주기
                ConnectionDeviceRow(savedDevice?.name)
                Spacer(modifier = Modifier.height(8.dp))
                LastConnectionTimeRow(timeText = lastConnectionTime)
                Spacer(modifier = Modifier.height(16.dp))

                // 스캔 / 연결 토글 버튼
                ActionButton(
                    state = bluetoothState,
                    enabled = bluetoothState != ConnectionEvent.Connecting,
                    onClick = {
                        if (allGranted) {
                            // 이미 권한이 허용된 상태: 스캔/연결 토글
                            if (bluetoothState == ConnectionEvent.Connected) {
                                onDisconnect()
                            } else {
                                onStartScan()
                            }
                        } else {
                            // 권한이 없으면 PermissionHandler 내부 로직에 따라 다이얼로그 또는 설정 이동
                            requestPermission()
                        }
                    }
                )
            }
        }
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
        Text("마지막 연결된 기기", style = MaterialTheme.typography.bodyLarge)
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
                ConnectionEvent.Idle,
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
                                    text = device.name,
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