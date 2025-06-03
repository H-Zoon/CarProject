package com.devidea.aicar.ui.main.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permissions: List<String>,
    rationaleTitle: String,
    rationaleMessage: String,
    permanentlyDeniedTitle: String,
    permanentlyDeniedMessage: String,
    modifier: Modifier = Modifier,
    content: @Composable (
        allGranted: Boolean,
        requestPermission: () -> Unit
    ) -> Unit
) {
    // 1) 권한 상태 관리 객체
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    // 2) 다이얼로그를 보여줄 때 쓰는 상태 변수들
    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    // 3) “권한 요청 시도” 함수를 래핑해서, 내부에서 showRationale / showPermanentlyDenied 를 토글
    val wrappedRequest: () -> Unit = {
        // 이미 모든 권한이 허용된 상태라면 따로 요청하지 않음
        if (permissionState.allPermissionsGranted) {
            //return
        }

        // 한 번 이상의 요청 후, 모든 권한이 거절된 상태이고 더 이상 권한 다이얼로그를 띄울 수 없는 경우
        val anyPermanentlyDenied = permissionState.permissions.any { perm ->
            // Accompanist PermissionState 에는 isPermanentlyDenied 확장 속성이 있음
            //   true: “다시 묻지 않음” 옵션을 체크하거나, 시스템 설정에서 영구적으로 비활성화된 경우
            perm.status.shouldShowRationale
        }

        if (anyPermanentlyDenied) {
            // “영구 거절” 상태이므로 앱 설정으로 이동하라는 다이얼로그를 띄움
            showPermanentlyDeniedDialog = true
        } else if (permissionState.shouldShowRationale) {
            // “처음 요청하거나, 이전에 거절했으나 ‘다시 묻기’를 해도 되는 상태” → rationale 다이얼로그
            showRationaleDialog = true
        } else {
            // “아직 요청한 적이 없거나, ‘다시 묻지 않음’이 체크되지 않은 최초 요청” → 시스템 권한 요청 팝업
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // 4) 먼저 “영구 거절” 다이얼로그
    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = { Text(text = permanentlyDeniedTitle) },
            text = { Text(text = permanentlyDeniedMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showPermanentlyDeniedDialog = false
                    // 앱 설정 화면으로 이동
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }) {
                    Text(text = "설정으로 이동")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentlyDeniedDialog = false }) {
                    Text(text = "취소")
                }
            }
        )
    }

    // 5) 일반 거절(다시 묻기) 다이얼로그
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text(text = rationaleTitle) },
            text = { Text(text = rationaleMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    // 다시 시스템 권한 요청
                    permissionState.launchMultiplePermissionRequest()
                }) {
                    Text(text = "다시 요청")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text(text = "취소")
                }
            }
        )
    }

    // 6) 최종적으로, 실제 UI(버튼 등)를 그리면서 “현재 권한 상태”와 “wrappedRequest” 함수만 제공
    content(
        permissionState.allPermissionsGranted,
        wrappedRequest
    )
}