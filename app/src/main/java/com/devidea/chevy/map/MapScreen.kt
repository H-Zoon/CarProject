package com.devidea.chevy.map

import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.devidea.chevy.viewmodel.MapViewModel
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView


@Composable
fun MapScreen(viewModel: MapViewModel) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle(viewModel, context)
    AndroidView({ mapView }) { /* Additional setup if needed */ }
}

@Composable
fun rememberMapViewWithLifecycle(viewModel: MapViewModel, context: android.content.Context): View {
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                mapView.start(object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {
                        // 지도 API 가 정상적으로 종료될 때 호출됨
                    }

                    override fun onMapError(error: Exception) {
                        // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출됨
                    }
                }, object : KakaoMapReadyCallback() {
                    override fun getPosition(): LatLng {
                        //return userLocation
                        return LatLng.from(37.5665, 126.9780)
                    }

                    override fun onMapReady(kakaoMap: KakaoMap) {
                        /*kakaoMaps = kakaoMap
                        trackingManager = kakaoMap.trackingManager

                        // 중심 라벨 생성
                        centerLabel = kakaoMap.labelManager?.layer?.addLabel(
                            LabelOptions.from("dotLabel", userLocation)
                                .setStyles(
                                    LabelStyle.from(R.drawable.c).setAnchorPoint(0.5f, 0.5f)
                                )
                                .setRank(1)
                        )

                        animationPolygon = kakaoMap.shapeManager?.layer?.addPolygon(
                            PolygonOptions.from("circlePolygon")
                                .setDotPoints(DotPoints.fromCircle(userLocation, 1.0f))
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

                        collectEvent()
                        startLocationUpdates()

                        kakaoMap.setOnCameraMoveStartListener { kakaoMap, gestureType ->
                            viewModel.setCameraTracking(false)
                        }
*/
                    }
                })
            }

            override fun onStart(owner: LifecycleOwner) {

            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.pause()
            }

            override fun onStop(owner: LifecycleOwner) {

            }

            override fun onDestroy(owner: LifecycleOwner) {

            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return mapView
}