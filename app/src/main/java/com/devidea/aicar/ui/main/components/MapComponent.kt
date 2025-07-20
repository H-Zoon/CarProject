package com.devidea.aicar.ui.main.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun SessionTrackMap(
    path: List<LatLng>,
    sliderPosition: Float,
    modifier: Modifier = Modifier,
    initialZoom: Float = 15f,
    animationDurationMs: Int = 500,
) {
    // 1) 카메라 상태 기억
    val cameraPositionState =
        rememberCameraPositionState {
            // 초기 위치: 경로 첫 번째 포인트(없으면 (0,0))
            position =
                CameraPosition.fromLatLngZoom(
                    path.firstOrNull() ?: LatLng(0.0, 0.0),
                    initialZoom,
                )
        }

    // 2) 슬라이더 위치 변화 시 카메라 애니메이션
    LaunchedEffect(sliderPosition, path) {
        // path가 비어 있거나 인덱스가 범위를 벗어나면 아무 동작도 하지 않음
        path.getOrNull(sliderPosition.toInt())?.let { target ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(target),
                durationMs = animationDurationMs,
            )
        }
    }

    // 3) 실제 Map 표시 + 폴리라인 + 마커
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = false),
    ) {
        // 경로 폴리라인
        if (path.size >= 2) {
            Polyline(
                points = path,
                color = MaterialTheme.colorScheme.primary,
                width = 5f,
            )
        }
        // 현재 위치 마커
        path.getOrNull(sliderPosition.toInt())?.let { current ->
            Marker(
                state = MarkerState(position = current),
                title = "현재 위치",
            )
        }
    }
}
