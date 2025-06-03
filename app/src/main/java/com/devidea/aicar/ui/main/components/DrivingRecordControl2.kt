package com.devidea.aicar.ui.main.components

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.RecordState

@Composable
fun DrivingRecordControl2(
    recordState: RecordState,
    isAutoRecordEnabled: Boolean,
    onAutoRecordToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    PermissionHandler(
        permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
        rationaleTitle = "알림 권한 필요",
        rationaleMessage = "포그라운드 알림을 보내려면 알림 권한이 필요합니다.",
        permanentlyDeniedTitle = "권한 영구 거절됨",
        permanentlyDeniedMessage = "알림 기능을 사용하려면 설정에서 권한을 허용해야 합니다."
    ) { allGranted, requestPermission ->
        // 2) 배터리 최적화 다이얼로그 상태
        var showBatteryOptDialog by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                            is RecordState.Pending -> "기록 대기중"
                            is RecordState.Stopped -> "주행 기록 취소"
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
                            val intentRequest =
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
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
