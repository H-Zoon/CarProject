package com.devidea.chevy.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devidea.chevy.App
import com.devidea.chevy.R
import com.devidea.chevy.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.kakao.vectormap.label.TrackingManager
import kotlinx.coroutines.launch

// 권한 정보를 관리하는 데이터 클래스
data class PermissionInfo(
    val permission: String,       // 실제 Manifest.permission 문자열
    val titleRes: Int,            // UI에 표시할 권한 이름 (문자열 리소스 ID)
    val descriptionRes: Int,      // 왜 필요한지 설명 (문자열 리소스 ID)
    val isRequired: Boolean,      // 필수 권한 여부
    val imageVector: ImageVector // 아이콘 리소스(예: R.drawable.ic_camera 등)
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    mainViewModel: MainViewModel, // 필수 권한 모두 허용 시 콜백
    onAllRequiredPermissionsGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var trackingManager by remember { mutableStateOf<Boolean>(false) }
    val permissions = rememberPermissionList()
    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = permissions.map { it.permission },
        onPermissionsResult = { permissionsResult ->
            val allGranted = permissionsResult.values.all { it }
            if (allGranted) {
                onAllRequiredPermissionsGranted()
            } else {
                if (trackingManager) {
                    coroutineScope.launch { mainViewModel.saveFirstLunch() }
                } else {
                    trackingManager = true
                }
            }
        }
    )
    val isFirstLaunch by mainViewModel.isFirstLunch.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "권한 상태", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            permissions.forEach { permissionInfo ->
                val permissionState =
                    multiplePermissionsState.permissions.find { it.permission == permissionInfo.permission }
                permissionState?.let { state ->
                    item {
                        PermissionItem(
                            isFirstLaunch,
                            permissionInfo = permissionInfo,
                            permissionState = state
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (multiplePermissionsState.permissions.all { it.status.isGranted }) {
            onAllRequiredPermissionsGranted()
        }

        // 설정으로 이동 버튼
        if (multiplePermissionsState.permissions.any { !it.status.isGranted && !it.status.shouldShowRationale && !isFirstLaunch }) {
            PermanentlyDeniedSection()
        } else {
            Button(onClick = {
                multiplePermissionsState.launchMultiplePermissionRequest()
            }) {
                Text("필수 권한 요청")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionItem(
    permissionInfo1: Boolean,
    permissionInfo: PermissionInfo,
    permissionState: PermissionState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = permissionInfo.imageVector , contentDescription = "")

            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(id = permissionInfo.titleRes),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = permissionInfo.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when {
            permissionState.status.isGranted -> {
                Text(text = "허용됨", color = MaterialTheme.colorScheme.primary)
            }

            !permissionState.status.isGranted && !permissionState.status.shouldShowRationale && permissionInfo1 -> {
                Text(text = "거부됨", color = MaterialTheme.colorScheme.error)
            }

            !permissionState.status.isGranted && permissionState.status.shouldShowRationale -> {
                Text(text = "권한을 다시 확인해주세요", color = MaterialTheme.colorScheme.error)
            }

            else -> {
                Text(text = "완전히 거부됨", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}


@Composable
fun PermanentlyDeniedSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE0E0)) // 연한 빨간색 배경
            .padding(16.dp)
            .semantics { contentDescription = "Permanently Denied Permissions Section" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.permanently_denied_permissions_title),
            style = MaterialTheme.typography.titleSmall,
            color = Color.Red,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(id = R.string.permanently_denied_permissions_description),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Red
        )

        // 설정 화면으로 이동 버튼
        Button(onClick = { openAppSettings() }) {
            Text(stringResource(id = R.string.open_settings))
        }
    }
}

fun openAppSettings() {
    val context = App.instance

    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // 사용자에게 알림을 표시하거나 로그를 남깁니다.
        e.printStackTrace()
    }
}

@Composable
fun rememberPermissionList(): List<PermissionInfo> {
    return remember {
        when {
            // Android 13(API 33) 이상
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(
                    PermissionInfo(
                        permission = Manifest.permission.BLUETOOTH_SCAN,
                        titleRes = R.string.permission_bluetooth_scan_title,
                        descriptionRes = R.string.permission_bluetooth_scan_description,
                        isRequired = true,
                        imageVector = Icons.Default.Search,
                    ),
                    PermissionInfo(
                        permission = Manifest.permission.BLUETOOTH_CONNECT,
                        titleRes = R.string.permission_bluetooth_connect_title,
                        descriptionRes = R.string.permission_bluetooth_connect_description,
                        isRequired = true,
                        imageVector = Icons.Default.Link,
                    ),
                    PermissionInfo(
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        titleRes = R.string.permission_post_notifications_title,
                        descriptionRes = R.string.permission_post_notifications_description,
                        isRequired = true,  // 알림은 선택 권한으로 처리 가능
                        imageVector = Icons.Default.Notifications,
                    ),
                    PermissionInfo(
                        permission = Manifest.permission.ACCESS_FINE_LOCATION,
                        titleRes = R.string.permission_location_title,
                        descriptionRes = R.string.permission_location_description,
                        isRequired = true,  // BLE 스캔에 필요하지만, 앱 설계에 따라 필수/선택 결정
                        imageVector = Icons.Default.MyLocation,
                    )
                )
            }

            // Android 12(API 31) 이상 (API 31, 32)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                listOf(
                    PermissionInfo(
                        permission = Manifest.permission.BLUETOOTH_SCAN,
                        titleRes = R.string.permission_bluetooth_scan_title,
                        descriptionRes = R.string.permission_bluetooth_scan_description,
                        isRequired = true,
                        imageVector = Icons.Default.Search,
                    ),
                    PermissionInfo(
                        permission = Manifest.permission.BLUETOOTH_CONNECT,
                        titleRes = R.string.permission_bluetooth_connect_title,
                        descriptionRes = R.string.permission_bluetooth_connect_description,
                        isRequired = true,
                        imageVector = Icons.Default.Link,
                    ),
                    PermissionInfo(
                        permission = Manifest.permission.ACCESS_FINE_LOCATION,
                        titleRes = R.string.permission_location_title,
                        descriptionRes = R.string.permission_location_description,
                        isRequired = true,
                        imageVector = Icons.Default.MyLocation,
                    )
                )
            }

            // 그 외 (Android 11 이하)
            else -> {
                // BLE 스캔/연결을 위해 Android 11 이하에서는 FINE_LOCATION 이 필요
                listOf(
                    PermissionInfo(
                        permission = Manifest.permission.ACCESS_FINE_LOCATION,
                        titleRes = R.string.permission_location_title,
                        descriptionRes = R.string.permission_location_description,
                        isRequired = true,
                        imageVector = Icons.Default.Search,
                    )
                )
            }
        }
    }
}