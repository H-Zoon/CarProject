package com.devidea.aicar.ui.main.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.R
import com.devidea.aicar.ui.main.viewmodels.SettingsViewModel
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lastDevice by viewModel.lastSavedDevice.collectAsState(initial = null)
    val autoOnCharge by viewModel.autoConnectOnCharge.collectAsState(initial = false)
    val fuelCost by viewModel.fuelCost.collectAsState(initial = 0)
    var showConfirm by remember { mutableStateOf<ResetTarget?>(null) }
    var showFuelDialog by remember { mutableStateOf(false) }
    var newFuelCostText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_setting),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ───────────── 연결 설정 ─────────────
            Text("연결 설정", style = MaterialTheme.typography.titleLarge)

            // 마지막 연결된 기기 정보
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "마지막 기기", style = MaterialTheme.typography.titleMedium)
                    if (lastDevice != null) {
                        Text(text = lastDevice!!.name, style = MaterialTheme.typography.bodyLarge)
                        Text(text = lastDevice!!.address, style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text(text = "저장된 기기가 없습니다.", style = MaterialTheme.typography.bodyMedium)
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
                        text = "기기 충전시 마지막 기기로 자동 연결합니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = autoOnCharge,
                    onCheckedChange = { enabled ->
                        viewModel.setAutoConnectOnCharge(enabled)
                    }
                )
            }

            Divider()

            // ───────────── 주행 기록 설정 ─────────────
            Text("주행 기록 설정", style = MaterialTheme.typography.titleLarge)

            // 유류비 설정 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "유류비 설정", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "현재 유류비: ${fuelCost}원",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = {
                            newFuelCostText = fuelCost.toString()
                            showFuelDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("유류비 수정")
                    }
                }
            }

            Divider()

            // ───────────── 초기화 ─────────────
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
                Button(
                    onClick = { showConfirm = ResetTarget.ALL },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text("전체 설정 초기화", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Divider()
            // ───────────── 초기화 ─────────────
            Text("도움", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    val email = "developsidea@gmail.com"
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                        putExtra(Intent.EXTRA_SUBJECT, "앱 건의하기")
                        putExtra(Intent.EXTRA_TEXT, "AICAR")
                    }
                    val chooser = Intent.createChooser(intent, "앱 건의하기")
                    try {
                        context.startActivity(chooser)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "이메일 앱이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("건의하기")
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
                        },
                        style = MaterialTheme.typography.bodyMedium
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
                    }) {
                        Text("초기화", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = null }) {
                        Text("취소", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            )
        }

        // 유류비 수정 다이얼로그
        if (showFuelDialog) {
            AlertDialog(
                onDismissRequest = { showFuelDialog = false },
                title = { Text("유류비 수정", style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "유류비를 입력하세요 (최대 4자리):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextField(
                            value = newFuelCostText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 4) {
                                    newFuelCostText = input
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cost = newFuelCostText.toIntOrNull() ?: 0
                        viewModel.setFuelCost(cost)
                        showFuelDialog = false
                    }) {
                        Text("저장", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFuelDialog = false }) {
                        Text("취소", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            )
        }
    }
}

private enum class ResetTarget {
    SAVED_DEVICE, DRIVING_RECORD, WIDGET_SETTINGS, ALL
}