package com.devidea.chevy.ui.map.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.devidea.chevy.R
import com.devidea.chevy.network.reomote.Document
import com.devidea.chevy.ui.map.MapViewModel
import com.devidea.chevy.ui.navi.NavigateData
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.animation.Interpolation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.route.RouteLineLayer
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyles
import com.kakao.vectormap.shape.PolygonStylesSet
import com.kakao.vectormap.shape.ShapeAnimator
import com.kakao.vectormap.shape.animation.CircleWave
import com.kakao.vectormap.shape.animation.CircleWaves
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute

@Composable
fun MapScreen(viewModel: MapViewModel, bottomPadding: Dp = 0.dp) {
    val cameraState by viewModel.cameraIsTracking.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val viewState by viewModel.uiState.collectAsState()
    val kakaoMapState by viewModel.kakaoMap.collectAsState()
    var userPositionMark by remember { mutableStateOf<Label?>(null) }
    var placeMark by remember { mutableStateOf<Label?>(null) }
    val density = LocalDensity.current
    val bottomPaddingValue = with(density) { bottomPadding.roundToPx() }

    LaunchedEffect(Unit) {
        viewModel.startMap { /* 필요 시 추가 callback */ }
    }

    AndroidView(
        factory = { viewModel.mapView }
    )

    LaunchedEffect(bottomPadding, kakaoMapState) {
        viewModel.kakaoMap.value?.setPadding(   // (L, T, R, B)
            0, 0, 0, bottomPaddingValue
        )
    }

    // userLocation이나 kakaoMap이 변경될 때마다 moveCameraTo 호출
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            viewModel.kakaoMap.value?.let {
                makeLabel(it, viewModel, location) {
                    userPositionMark = it
                }
            }
        }
    }

    LaunchedEffect(cameraState, userPositionMark) {
        viewModel.kakaoMap.value?.let { map ->
            when (cameraState) {
                true -> {
                    map.trackingManager?.startTracking(userPositionMark)
                    map.trackingManager?.setTrackingRotation(false)
                }

                false -> {
                    map.trackingManager?.stopTracking()
                }
            }
        }
    }

    LaunchedEffect(viewState) {
        viewModel.kakaoMap.value?.let { map ->
            when (viewState) {
                is MapViewModel.UiState.ShowDetail -> {
                    map.trackingManager?.stopTracking()
                    goalLabel(map, (viewState as MapViewModel.UiState.ShowDetail).item) {
                        placeMark = it
                    }
                }

                is MapViewModel.UiState.DrawRoute -> {
                    map.labelManager?.layer?.remove(placeMark)
                }

                is MapViewModel.UiState.Idle -> {
                    map.trackingManager?.startTracking(userPositionMark)
                    map.trackingManager?.setTrackingRotation(false)
                    map.labelManager?.layer?.remove(placeMark)
                    map.routeLineManager?.layer?.removeAll()
                    map.moveCamera(CameraUpdateFactory.zoomTo(15))
                }

                else -> {
                    map.labelManager?.layer?.remove(placeMark)
                }
            }
        }
    }
}

fun makeLabel(
    kakaoMap: KakaoMap,
    viewModel: MapViewModel,
    location: LatLng,
    label: (Label) -> Unit
) {
    // 중심 라벨 생성
    val centerLabel: Label? = kakaoMap.labelManager?.layer?.addLabel(
        LabelOptions.from("dotLabel", location)
            .setStyles(
                LabelStyle.from(R.drawable.ic_label_center).setAnchorPoint(0.5f, 0.5f)
            )
            .setRank(1)
    )

    val animationPolygon: Polygon? = kakaoMap.shapeManager?.layer?.addPolygon(
        PolygonOptions.from("circlePolygon")
            .setDotPoints(DotPoints.fromCircle(location, 1.0f))
            .setStylesSet(
                PolygonStylesSet.from(
                    PolygonStyles.from(
                        Color(
                            0xFFff722b
                        ).toArgb()
                    )
                )
            )
    )

    val circleWaves: CircleWaves = CircleWaves.from(
        "circleWaveAnim",
        CircleWave.from(1F, 0F, 0F, 100F)
    )
        .setHideShapeAtStop(false)
        .setInterpolation(Interpolation.CubicInOut)
        .setDuration(1500)
        .setRepeatCount(500)
    val shapeAnimator: ShapeAnimator? =
        kakaoMap.shapeManager?.addAnimator(circleWaves)

    shapeAnimator?.addPolygons(animationPolygon)
    shapeAnimator?.start()

    centerLabel?.addShareTransform(animationPolygon)

    kakaoMap.setOnCameraMoveStartListener { kakaoMap, gestureType ->
        viewModel.setCameraTracking(false)
    }
    if (centerLabel != null) {
        label(centerLabel)
    }
}

//사용자 검색결과 표시 마크
fun goalLabel(kakaoMap: KakaoMap, document: Document, label: (Label) -> Unit) {
    kakaoMap.setPadding(   // (L, T, R, B)
        0, 0, 0, 0
    )
    val cameraUpdate =
        CameraUpdateFactory.newCenterPosition(LatLng.from(document.y, document.x))
    kakaoMap.moveCamera(cameraUpdate)

    val detailLabel = kakaoMap.labelManager?.layer?.addLabel(
        LabelOptions.from("dotLabel2", LatLng.from(document.y, document.x)
        ).setStyles(
                LabelStyle.from(R.drawable.icon_pin_orange).setAnchorPoint(0.5f, 1f)
            )
            .setRank(1)
    )

    if (detailLabel != null) {
        label(detailLabel)
    }
}

@Composable
fun LoadRequestScreen(data: NavigateData, viewModel: MapViewModel) {
    val stateList = remember { mutableStateListOf<KNRoute>() }
    var focusedIndex by remember { mutableStateOf(-1) }

    // stateList가 갱신되었을 때, 첫 번째 카드에 포커스 주기
    LaunchedEffect(stateList) {
        if (stateList.isNotEmpty()) {
            focusedIndex = 0
        }
    }

    LaunchedEffect(data) {
        stateList.clear()
        try {
            // ViewModel 에 위임한 suspend 호출
            val routes = viewModel.fetchAllRoutes(data)
            stateList.addAll(routes)

        } catch (e: Exception) {

        } finally {

        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(stateList) { index, route ->
            val isFocused = index == focusedIndex

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    // 포커스된 카드에는 테두리
                    .then(
                        if (isFocused)
                            Modifier.border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                shape = MaterialTheme.shapes.medium
                            )
                        else
                            Modifier
                    )
                    .clickable {
                        if (isFocused) {
                            // 이미 포커스된 카드를 다시 탭했을 때
                            //foo()
                        } else {
                            // 다른 카드를 탭했을 때
                            DrawRouteLine(stateList[index], viewModel)
                            focusedIndex = index
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "경로 우선순위: ${getRoutePriorityName(route.priority)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "경로 거리: ${route.totalDist} m")
                    Text(text = "경로 시간: ${route.totalTime} 초")
                    Text(text = "경로 요금: ${route.totalCost} 원")
                }
            }
        }
    }
}

fun getRoutePriorityName(priority: KNRoutePriority?): String {
    return when (priority) {
        KNRoutePriority.KNRoutePriority_Recommand -> "추천 경로"
        KNRoutePriority.KNRoutePriority_Time -> "시간 우선"
        KNRoutePriority.KNRoutePriority_Distance -> "거리 우선"
        KNRoutePriority.KNRoutePriority_HighWay -> "고속도로 우선"
        KNRoutePriority.KNRoutePriority_WideWay -> "큰 길 우선"
        else -> "알 수 없음"
    }
}

fun mapStatusToStyle(status: Int): RouteLineStyle {
    return when (status) {
        // 교통 상태 정보 없음(0) 또는 원활(4) -> 파란색
        0, 4 -> RouteLineStyle.from(16f, android.graphics.Color.BLUE)
        // 교통 서행(3) -> 청색 (Cyan)
        3 -> RouteLineStyle.from(16f, android.graphics.Color.CYAN)
        // 교통 지체(2) -> 노란색
        2 -> RouteLineStyle.from(16f, android.graphics.Color.YELLOW)
        // 교통 정체(1) 또는 교통사고(6) -> 빨간색
        1, 6 -> RouteLineStyle.from(16f, android.graphics.Color.RED)
        else -> RouteLineStyle.from(16f, android.graphics.Color.BLUE)
    }
}

private fun DrawRouteLine(
    route: KNRoute,
    viewModel: MapViewModel
) {
    val mapList: List<Map<String, Number>>? = route.routePolylineWGS84()
    // 1. RouteLineLayer 가져오기
    val layer: RouteLineLayer = viewModel.kakaoMap.value!!.routeLineManager!!.layer
    layer.removeAll()
    // 2. 교통 흐름에 따라 사용할 스타일을 미리 생성
    // (여기서는 스타일셋에 4개의 스타일을 등록하지만,
    //  실제 세그먼트에는 각 세그먼트별 교통 상태에 맞는 색상을 직접 지정함)
    val styles1 = RouteLineStyles.from(RouteLineStyle.from(16f, android.graphics.Color.BLUE))
    val styles2 = RouteLineStyles.from(RouteLineStyle.from(16f, android.graphics.Color.CYAN))
    val styles3 = RouteLineStyles.from(RouteLineStyle.from(16f, android.graphics.Color.YELLOW))
    val styles4 = RouteLineStyles.from(RouteLineStyle.from(16f, android.graphics.Color.RED))
    val stylesSet = RouteLineStylesSet.from(styles1, styles2, styles3, styles4)

    // 3. routePolylineWGS84 데이터를 바탕으로 RouteLineSegment 생성하기
    val segments = mutableListOf<RouteLineSegment>()

    if (mapList.isNullOrEmpty().not()) {
        // 첫 번째 데이터의 trfSt 값을 초기 교통 상태로 사용
        var currentStatus = mapList!![0]["trfSt"]?.toInt() ?: 0
        val currentPoints = mutableListOf<LatLng>()

        for (point in mapList) {
            val lat = point["y"]?.toDouble() ?: 0.0
            val lng = point["x"]?.toDouble() ?: 0.0
            val latLng = LatLng.from(lat, lng)
            val status = point["trfSt"]?.toInt() ?: currentStatus

            if (status != currentStatus && currentPoints.size >= 2) {
                // 기존 세그먼트 생성
                segments.add(
                    RouteLineSegment.from(
                        currentPoints,
                        mapStatusToStyle(currentStatus)
                    )
                )
                // 마지막 좌표를 새로운 세그먼트의 시작점으로 유지
                val lastPoint = currentPoints.last()
                currentPoints.clear()
                currentPoints.add(lastPoint)
                currentStatus = status
            }
            currentPoints.add(latLng)
        }
        // 마지막 남은 좌표들로 세그먼트 생성 (필요한 경우)
        if (currentPoints.size >= 2) {
            segments.add(
                RouteLineSegment.from(
                    currentPoints,
                    mapStatusToStyle(currentStatus)
                )
            )
        }
    }

    val options = RouteLineOptions.from(segments)
        .setStylesSet(stylesSet)

    layer.addRouteLine(options)

    // 6. 모든 좌표의 Bounds 계산 후 중심 좌표 및 동적 줌 레벨 적용
    if (!mapList.isNullOrEmpty()) {
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE

        // 좌표 리스트 전체를 순회하며 경계 계산
        for (point in mapList) {
            val lat = point["y"]?.toDouble() ?: continue
            val lng = point["x"]?.toDouble() ?: continue
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            if (lng < minLng) minLng = lng
            if (lng > maxLng) maxLng = lng
        }
        // 중심 좌표 계산
        val centerLat = (minLat + maxLat) / 2.0
        val centerLng = (minLng + maxLng) / 2.0
        val centerPoint = LatLng.from(centerLat, centerLng)

        // 경계 크기 계산 (위도/경도 차이)
        val latDiff = maxLat - minLat
        val lngDiff = maxLng - minLng
        val maxDiff = maxOf(latDiff, lngDiff)

        // 동적 줌 레벨 산출 (임계값은 필요에 따라 조정)
        val dynamicZoom = when {
            maxDiff < 0.005 -> 16
            maxDiff < 0.01 -> 15
            maxDiff < 0.02 -> 14
            maxDiff < 0.05 -> 13
            maxDiff < 0.1 -> 12
            maxDiff < 0.2 -> 11
            maxDiff < 0.5 -> 10
            else -> 9
        }
        // 계산된 중심과 줌 레벨로 카메라 업데이트
        viewModel.kakaoMap.value!!.moveCamera(CameraUpdateFactory.newCenterPosition(centerPoint))
        viewModel.kakaoMap.value!!.moveCamera(CameraUpdateFactory.zoomTo(dynamicZoom))
    }
}


