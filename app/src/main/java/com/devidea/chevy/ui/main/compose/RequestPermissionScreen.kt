package com.devidea.chevy.ui.main.compose

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestButton(
    permissions: List<String>,
    modifier: Modifier = Modifier,
    buttonText: String = "권한 요청",
    onAllGranted: () -> Unit,
    onShowRationale: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
) {
    // 1) MultiplePermissionsState 생성
    val permissionsState = rememberMultiplePermissionsState(permissions)

    // 2) 요청 여부 추적용 로컬 상태
    var permissionRequested by remember { mutableStateOf(false) }

    // 3) 버튼 클릭 시 요청 플래그 세팅 및 권한 요청
    Button(
        onClick = {
            permissionRequested = true
            permissionsState.launchMultiplePermissionRequest()
        },
        modifier = modifier
    ) {
        Text(buttonText)
    }

    // 4) 권한 상태 변화 감지 & 콜백 분기
    LaunchedEffect(
        permissionsState.allPermissionsGranted,
        permissionsState.shouldShowRationale,
        permissionRequested
    ) {
        // 요청 전에는 아무 동작 안 함
        if (!permissionRequested) return@LaunchedEffect

        when {
            // 모두 승인
            permissionsState.allPermissionsGranted -> {
                onAllGranted()
            }
            // 거부·재요청 가능
            permissionsState.shouldShowRationale -> {
                onShowRationale()
            }
            // 영구 거부
            else -> {
                onPermissionPermanentlyDenied()
            }
        }
    }
}