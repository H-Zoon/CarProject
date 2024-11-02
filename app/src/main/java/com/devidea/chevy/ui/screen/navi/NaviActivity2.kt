package com.devidea.chevy.ui.screen.navi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.devidea.chevy.Logger
import com.devidea.chevy.datas.navi.NavigateDocument
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.viewmodel.GuidanceEvent
import com.devidea.chevy.viewmodel.NaviViewModel
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.ui.view.KNNaviView

@Composable
fun NaviScreen(
    activity: MainActivity,
    viewModel: NaviViewModel,
    modifier: Modifier = Modifier,
    document: NavigateDocument
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
        // 경로 설정 로직
        document.let {
            val startKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = it.startX, aWgs84Lat = it.startY)
            val goalKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = it.goalX, aWgs84Lat = it.goalY)

            val startKNPOI = KNPOI("", startKATEC.x.toInt(), startKATEC.y.toInt(), aAddress = null)
            val goalKNPOI = KNPOI("", goalKATEC.x.toInt(), goalKATEC.y.toInt(), it.address_name)

            val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
            val curAvoidOptions =
                KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value

            KNSDK.makeTripWithStart(aStart = startKNPOI, aGoal = goalKNPOI, aVias = null) { knError, knTrip ->
                if (knError != null) {
                    Logger.e { "경로 생성 에러(KNError: $knError" }
                }
                knTrip?.routeWithPriority(curRoutePriority, curAvoidOptions) { error, _ ->
                    if (error != null) {
                        Logger.e { "경로 요청 실패 : $error" }
                    } else {
                        KNSDK.sharedGuidance()?.apply {
                            naviView.initWithGuidance(
                                this,
                                knTrip,
                                curRoutePriority,
                                curAvoidOptions
                            )
                        }
                    }
                }
            }
        }


        viewModel.eventFlow.collect { event ->
            when (event) {
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
