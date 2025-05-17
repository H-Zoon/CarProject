package com.devidea.aicar.ui.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.R
import com.devidea.aicar.ui.main.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val lastDevice by viewModel.lastSavedDevice.collectAsState(initial = null)
    val autoOnCharge by viewModel.autoConnectOnCharge.collectAsState(initial = false)
    var showConfirm by remember { mutableStateOf<ResetTarget?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_setting), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 마지막 연결된 기기 정보
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = "마지막 기기", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (lastDevice != null) {
                        Text(text = lastDevice!!.name)
                        Text(text = lastDevice!!.address)
                    } else {
                        Text(text = "저장된 기기가 없습니다.")
                    }
                }
            }

            // 충전 감지 시 자동 연결 설정
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "충전 시 자동 연결", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "기기를 충전기에 연결하면 마지막 기기로 자동 연결합니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = autoOnCharge,
                    onCheckedChange = { enabled ->
                        viewModel.setAutoConnectOnCharge(enabled)
                    }
                )
            }

            Text("초기화", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showConfirm = ResetTarget.SAVED_DEVICE },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("저장된 블루투스 기기 초기화")
                }
                Button(
                    onClick = { showConfirm = ResetTarget.DRIVING_RECORD },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("주행 기록 초기화")
                }
                Button(
                    onClick = { showConfirm = ResetTarget.WIDGET_SETTINGS },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("위젯 설정 초기화")
                }
                Divider()
                Button(
                    onClick = { showConfirm = ResetTarget.ALL },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text("전체 설정 초기화", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // 확인 다이얼로그
        showConfirm?.let { target ->
            AlertDialog(
                onDismissRequest = { showConfirm = null },
                title = { Text("초기화 확인") },
                text = {
                    Text(
                        when (target) {
                            ResetTarget.SAVED_DEVICE -> "저장된 블루투스 기기를 정말 삭제하시겠습니까?"
                            ResetTarget.DRIVING_RECORD -> "모든 주행 기록을 정말 삭제하시겠습니까?"
                            ResetTarget.WIDGET_SETTINGS -> "위젯 순서를 기본값으로 되돌리시겠습니까?"
                            ResetTarget.ALL -> "모든 설정을 초기화하시겠습니까? 이 작업은 되돌릴 수 없습니다."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        when (target) {
                            ResetTarget.SAVED_DEVICE -> viewModel.resetSavedDevice()
                            ResetTarget.DRIVING_RECORD -> viewModel.resetDrivingRecord()
                            ResetTarget.WIDGET_SETTINGS -> viewModel.resetWidgetSettings()
                            ResetTarget.ALL -> viewModel.resetAll()
                        }
                        showConfirm = null
                    }) { Text("초기화") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = null }) { Text("취소") }
                }
            )
        }
    }
}

private enum class ResetTarget {
    SAVED_DEVICE, DRIVING_RECORD, WIDGET_SETTINGS, ALL
}