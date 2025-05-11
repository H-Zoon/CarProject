package com.devidea.aicar.ui.main.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.R
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.ui.main.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothActionComponent(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val bluetoothState by viewModel.bluetoothEvent.collectAsState(ConnectionEvent.Disconnected)
    var statusText by remember { mutableStateOf("준비됨") }
    var isShowBleDeviceListModal by remember { mutableStateOf(false) }

    LaunchedEffect(bluetoothState) {
        isShowBleDeviceListModal = (bluetoothState == ConnectionEvent.Scanning)
    }

    if (isShowBleDeviceListModal) {
        BleDeviceListModal(viewModel = viewModel, requestScan = {viewModel.startScan()}, requestConnect = {viewModel.connectTo(it)}, onBack = {viewModel.stopScan()})
    }
    // 1) 블루투스 권한(안드로이드 12+)
    val permsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.POST_NOTIFICATIONS    // Android 13+ 알림 권한
        )
    )
    // 요청 여부 플래그
    var permissionRequested by remember { mutableStateOf(false) }
    // 라쇼날 다이얼로그 표시 여부
    var showRationaleDialog by remember { mutableStateOf(false) }


    // 2) 이벤트 수집
    LaunchedEffect(viewModel.bluetoothEvent) {
        if(viewModel.bluetoothEvent == ConnectionEvent.Connected) viewModel.setConnectTime()

        viewModel.bluetoothEvent.collect { evt ->
            statusText = when (evt) {
                ConnectionEvent.Scanning    -> "검색 중..."
                ConnectionEvent.Connecting  -> "연결 중..."
                ConnectionEvent.Connected   -> "연결 완료"
                ConnectionEvent.Disconnected-> "연결 해제됨"
                ConnectionEvent.Error -> "연결에 실패 하였습니다."
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
                when {
                    // 이미 모든 권한 승인된 상태
                    permsState.allPermissionsGranted -> {
                        when (bluetoothState) {
                            ConnectionEvent.Connecting -> viewModel.disconnect()
                            else -> viewModel.startScan()
                        }
                    }
                    // 최초 클릭: 권한 요청
                    !permissionRequested -> {
                        permissionRequested = true
                        permsState.launchMultiplePermissionRequest()
                    }
                    // 거부했지만 라쇼날 표시 가능한 경우
                    permsState.shouldShowRationale -> {
                        showRationaleDialog = true
                    }
                    // 영구 거부된 경우
                    else -> {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
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

        if (showRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showRationaleDialog = false },
                title = { Text("권한 필요") },
                text = { Text("블루투스 연결을 위해 권한이 필요합니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRationaleDialog = false
                        permsState.launchMultiplePermissionRequest()
                    }) {
                        Text("다시 요청")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRationaleDialog = false }) {
                        Text("취소")
                    }
                }
            )
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