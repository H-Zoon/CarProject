package com.devidea.aicar.ui.main.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devidea.aicar.service.RecordState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DrivingRecordControl2(
    recordState: RecordState,
    isAutoRecordEnabled: Boolean,
    onAutoRecordToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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

    Column(modifier = modifier.fillMaxWidth()) {
        // 주행 기록 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "주행 기록 상태", style = MaterialTheme.typography.bodyMedium)
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
                        is RecordState.Pending -> "기록 대기중"
                        is RecordState.Stopped -> "주행 기록 취소"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp))

        // 자동 기록 토글
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "자동 기록 설정", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isAutoRecordEnabled,
                onCheckedChange = { newValue ->
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
                        onAutoRecordToggle(newValue)
                    }
                }
            )
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
                    TextButton(onClick = { showFgPermanentlyDeniedDialog = false }) { Text(text = "취소") }
                }
            )
        }

        // 백그라운드 위치 권한: 설정 유도
        if (showBgPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showBgPermissionDialog = false },
                title = { Text(text = "백그라운드 위치 권한 필요") },
                text = { Text(text = "Android 최신 버전에서는 설정에서 백그라운드 위치 권한을 직접 허용해야 합니다.") },
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
                text = { Text(
                    text = "백그라운드에서 주행 기록 기능이 정상 작동하려면, 시스템의 배터리 최적화에서 예외로 설정해야 합니다."
                )},
                confirmButton = {
                    TextButton(onClick = {
                        showBatteryOptDialog = false
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
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