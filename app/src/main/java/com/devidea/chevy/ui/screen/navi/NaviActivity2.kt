package com.devidea.chevy.ui.screen.navi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.eventbus.GuidanceEvent
import com.devidea.chevy.eventbus.KNNAVEventBus
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.viewmodel.NaviViewModel
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.ui.view.KNNaviView

@Composable
fun NaviScreen(
    activity: MainActivity,
    viewModel: NaviViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    // 상태 수집
    val currentLocation by viewModel.currentLocation.collectAsState()
    val safetyGuide by viewModel.safetyGuide.collectAsState()
    val routeGuide by viewModel.routeGuide.collectAsState()
    val naviView = remember {
        KNNaviView(activity).apply {
            sndVolume = 1f
            useDarkMode = true
            fuelType = KNCarFuel.KNCarFuel_Gasoline
            carType = KNCarType.KNCarType_1
            guideStateDelegate = activity
        }
    }

    AndroidView(
        factory = { naviView },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            // 필요 시 업데이트 로직 구현
            currentLocation?.let { location ->
                // naviView 업데이트 예시
            }

            safetyGuide?.let { guide ->
                // naviView 업데이트 예시
            }

            routeGuide?.let { guide ->
                // naviView 업데이트 예시
            }
        }
    )

    LaunchedEffect(naviView) {
        KNNAVEventBus.events.collect { event ->
            when (event) {
                is GuidanceEvent.RequestNavGuidance -> {
                    KNSDK.sharedGuidance()?.apply {
                        naviView.initWithGuidance(
                            this,
                            event.a,
                            event.b,
                            event.c
                        )
                    }
                }
                is GuidanceEvent.GuidanceCheckingRouteChange -> {
                    naviView.guidanceCheckingRouteChange(event.guidance)
                }

                is GuidanceEvent.GuidanceDidUpdateIndoorRoute -> {
                    naviView.guidanceDidUpdateIndoorRoute(event.guidance, event.route)
                }

                is GuidanceEvent.GuidanceDidUpdateRoutes -> {
                    naviView.guidanceDidUpdateRoutes(event.guidance, event.routes, event.multiRouteInfo)
                }

                is GuidanceEvent.GuidanceGuideStarted -> {
                    naviView.guidanceGuideStarted(event.guidance)
                }

                is GuidanceEvent.GuidanceGuideEnded -> {
                    naviView.guidanceGuideEnded(event.guidance)
                }

                is GuidanceEvent.GuidanceOutOfRoute -> {
                    naviView.guidanceOutOfRoute(event.guidance)
                }

                is GuidanceEvent.GuidanceRouteChanged -> {
                    naviView.guidanceRouteChanged(event.guidance)
                }

                is GuidanceEvent.GuidanceRouteUnchanged -> {
                    naviView.guidanceRouteUnchanged(event.guidance)
                }

                is GuidanceEvent.GuidanceRouteUnchangedWithError -> {
                    naviView.guidanceRouteUnchangedWithError(event.guidance, event.error)
                }

                is GuidanceEvent.DidUpdateCitsGuide -> {
                    naviView.didUpdateCitsGuide(event.guidance, event.citsGuide)
                }

                is GuidanceEvent.GuidanceDidUpdateLocation -> {
                    naviView.guidanceDidUpdateLocation(event.guidance, event.locationGuide)
                }

                is GuidanceEvent.GuidanceDidUpdateRouteGuide -> {
                    naviView.guidanceDidUpdateRouteGuide(event.guidance, event.routeGuide)
                }

                is GuidanceEvent.GuidanceDidUpdateSafetyGuide -> {
                    naviView.guidanceDidUpdateSafetyGuide(event.guidance, event.safetyGuide)
                }

                is GuidanceEvent.GuidanceDidUpdateAroundSafeties -> {
                    naviView.guidanceDidUpdateAroundSafeties(event.guidance, event.safeties)
                }

                is GuidanceEvent.ShouldPlayVoiceGuide -> {
                    naviView.shouldPlayVoiceGuide(event.guidance, event.voiceGuide, event.newData)
                }

                is GuidanceEvent.WillPlayVoiceGuide -> {
                    naviView.willPlayVoiceGuide(event.guidance, event.voiceGuide)
                }

                is GuidanceEvent.DidFinishPlayVoiceGuide -> {
                    naviView.didFinishPlayVoiceGuide(event.guidance, event.voiceGuide)
                }
            }
        }
    }
}
