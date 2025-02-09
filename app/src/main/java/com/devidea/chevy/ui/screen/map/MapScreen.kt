package com.devidea.chevy.ui.screen.map

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.devidea.chevy.R
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.viewmodel.MapViewModel
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.animation.Interpolation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.TrackingManager
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyles
import com.kakao.vectormap.shape.PolygonStylesSet
import com.kakao.vectormap.shape.ShapeAnimator
import com.kakao.vectormap.shape.animation.CircleWave
import com.kakao.vectormap.shape.animation.CircleWaves

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val cameraState by viewModel.cameraIsTracking.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val viewState by viewModel.uiState.collectAsState()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var label by remember { mutableStateOf<Label?>(null) }
    var label2 by remember { mutableStateOf<Label?>(null) }
    var trackingManager by remember { mutableStateOf<TrackingManager?>(null) }

    val context = LocalContext.current

    val mapView = rememberMapViewWithLifecycle(context) { map ->
        kakaoMap = map // onMapReady 콜백에서 kakaoMap 저장
        trackingManager = map.trackingManager
    }

    // userLocation이나 kakaoMap이 변경될 때마다 moveCameraTo 호출
    LaunchedEffect(userLocation, kakaoMap) {
        userLocation?.let { location ->
            kakaoMap?.let { map ->
                makeLabel(map, viewModel, location) {
                    label = it
                }
            }
        }
    }

    LaunchedEffect(cameraState, label) {
        if (cameraState) {
            trackingManager?.startTracking(label)
            trackingManager?.setTrackingRotation(false)
        } else {
            trackingManager?.stopTracking()
        }
    }

    LaunchedEffect(viewState) {
        kakaoMap?.let { map ->
            if (viewState is MapViewModel.UiState.ShowDetail) {
                trackingManager?.stopTracking()
                goalLabel(map, (viewState as MapViewModel.UiState.ShowDetail).item) {
                    label2 = it
                }
            } else {
                map.labelManager?.layer?.remove(label2)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        FloatingActionButton(
            onClick = { viewModel.setCameraTracking(!cameraState) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape,
            containerColor = if (cameraState) Color(0xFFff722b) else Color.Gray
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "내 위치",
                tint = Color.White
            )
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(
    //viewModel: MapViewModel,
    context: Context,
    onMapReady: (KakaoMap) -> Unit // 콜백 매개변수 추가
): View {
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                mapView.start(object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {
                        // 지도 API가 정상적으로 종료될 때 호출됨
                    }

                    override fun onMapError(error: Exception) {
                        // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출됨
                    }
                }, object : KakaoMapReadyCallback() {
                    override fun getPosition(): LatLng {
                        return LatLng.from(37.5665, 126.9780)
                    }

                    override fun onMapReady(kakaoMap: KakaoMap) {
                        onMapReady(kakaoMap) // kakaoMap 객체 전달
                    }
                })
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.pause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                //mapView.destroy()
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return mapView
}

fun makeLabel(kakaoMap: KakaoMap, viewModel: MapViewModel, location: LatLng, label: (Label) -> Unit) {
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

fun goalLabel(kakaoMap: KakaoMap, document: Document, label: (Label) -> Unit) {
    val cameraUpdate =
        CameraUpdateFactory.newCenterPosition(
            LatLng.from(
                document.y,
                document.x
            )
        )
    kakaoMap.moveCamera(cameraUpdate)

    val detailLabel = kakaoMap.labelManager?.layer?.addLabel(
        LabelOptions.from(
            "dotLabel2", LatLng.from(
                document.y,
                document.x
            )
        )
            .setStyles(
                LabelStyle.from(R.drawable.icon_pin_orange).setAnchorPoint(0.5f, 1f)
            )
            .setRank(1)
    )

    if (detailLabel != null) {
        label(detailLabel)
    }
}