package com.devidea.chevy.ui.screen.navi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.datas.navi.NavigationIconType
import com.devidea.chevy.datas.navi.isCameraType
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec.sendLaneInfo
import com.devidea.chevy.eventbus.GuidanceEvent
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.eventbus.KNNAVEventBus
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.viewmodel.NaviViewModel
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNRGCode
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import com.kakaomobility.knsdk.ui.view.KNNaviView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun sendToCameraInfo(safetyGuide: List<KNSafety>?, currentLocation: KNLocation?) {
    // safetiesOnGuide가 비어있거나 null인지 확인합니다.
    if (safetyGuide.isNullOrEmpty()) {
        ToDeviceCodec.sendLimitSpeed(0, 0)
        ToDeviceCodec.sendCameraDistance(0, 0, 0)
        //ToDeviceCodec.sendCameraDistanceEx(0, 0, 0);
    } else {
        // safetiesOnGuide 리스트를 순회합니다.
        for (safety in safetyGuide) {
            // 과속 단속 카메라 (코드 81, 82, 86, 100, 102, 103) 처리
            if (isCameraType(safety.code.value)) {
                val speedLimit = (safety as KNSafety_Camera).speedLimit
                val cameraDistance = currentLocation?.distToLocation(safety.location) ?: 0
                ToDeviceCodec.sendLimitSpeed(cameraDistance, speedLimit)
                ToDeviceCodec.sendCameraDistance(cameraDistance, speedLimit, 1)
                ToDeviceCodec.sendNextInfo(
                    NavigationIconType.NONE.value,
                    cameraDistance
                )

            } else {
                ToDeviceCodec.sendCameraDistance(0, 0, 0)
                ToDeviceCodec.sendLimitSpeed(0, 0)
            }
        }
    }
}

fun updateToSafetyInfo(routeGuide: KNGuide_Route, currentLocation: KNLocation?) {
    // 목표의 거리와 진행 방향 계산
    val distance = routeGuide.curDirection?.location?.let { currentLocation?.distToLocation(it) } ?: 0
    val guidanceAsset = routeGuide.curDirection?.let { findGuideAsset(it.rgCode) } ?: NavigationIconType.NONE

    // 결과 전송
    ToDeviceCodec.sendNextInfo(guidanceAsset.value, distance)

    //차선 정보가 있다면 추천 차선 계산
    routeGuide.lane?.laneInfos?.let {
        val recommendArray = IntArray(it.size)

        // 추천 차선이면 1, 아니면 0을 배열에 저장
        for (i in it.indices) {
            recommendArray[i] = if (it[i].suggest == 1.toByte()) 1 else 0
        }
        //결과 전송
        sendLaneInfo(recommendArray)
    }
}

private fun findGuideAsset(code: KNRGCode): NavigationIconType {
    return when (code) {
        KNRGCode.KNRGCode_Start -> NavigationIconType.NONE
        KNRGCode.KNRGCode_Goal -> NavigationIconType.ARRIVED_DESTINATION
        KNRGCode.KNRGCode_Via -> NavigationIconType.ARRIVED_WAYPOINT
        KNRGCode.KNRGCode_Straight -> NavigationIconType.STRAIGHT

        // 좌회전 관련 방향
        KNRGCode.KNRGCode_LeftTurn -> NavigationIconType.LEFT

        KNRGCode.KNRGCode_LeftDirection,
        KNRGCode.KNRGCode_LeftOutHighway,
        KNRGCode.KNRGCode_LeftInHighway,
        KNRGCode.KNRGCode_LeftOutCityway,
        KNRGCode.KNRGCode_LeftInCityway,
        KNRGCode.KNRGCode_LeftStraight,
        KNRGCode.KNRGCode_ChangeLeftHighway -> NavigationIconType.LEFT_FRONT

        // 우회전 관련 방향
        KNRGCode.KNRGCode_RightTurn -> NavigationIconType.RIGHT

        KNRGCode.KNRGCode_RightDirection,
        KNRGCode.KNRGCode_RightOutHighway,
        KNRGCode.KNRGCode_RightInHighway,
        KNRGCode.KNRGCode_RightOutCityway,
        KNRGCode.KNRGCode_RightInCityway,
        KNRGCode.KNRGCode_RightStraight,
        KNRGCode.KNRGCode_ChangeRightHighway -> NavigationIconType.RIGHT_FRONT

        // 후방 방향
        KNRGCode.KNRGCode_Direction_7 -> NavigationIconType.LEFT_BACK
        KNRGCode.KNRGCode_Direction_5 -> NavigationIconType.RIGHT_BACK

        // U턴
        KNRGCode.KNRGCode_UTurn -> NavigationIconType.TURN_AROUND

        /*
        // 고속도로 출입구
        KNRGCode.KNRGCode_OutHighway -> NavigationIconType.OUT_ROUNDABOUT
        KNRGCode.KNRGCode_InHighway -> NavigationIconType.ENTER_ROUNDABOUT

        // 페리 항로
        KNRGCode.KNRGCode_InFerry -> NavigationIconType.ENTER_ROUNDABOUT
        KNRGCode.KNRGCode_OutFerry -> NavigationIconType.OUT_ROUNDABOUT
         */

        // 터널 및 톨게이트
        KNRGCode.KNRGCode_OverPath -> NavigationIconType.ARRIVED_TUNNEL
        KNRGCode.KNRGCode_Tollgate -> NavigationIconType.ARRIVED_TOLLGATE
        KNRGCode.KNRGCode_NonstopTollgate -> NavigationIconType.ARRIVED_TOLLGATE
        KNRGCode.KNRGCode_JoinAfterBranch -> NavigationIconType.ARRIVED_SERVICE_AREA

        // 로터리 방향
        KNRGCode.KNRGCode_RotaryDirection_1,
        KNRGCode.KNRGCode_RotaryDirection_2,
        KNRGCode.KNRGCode_RotaryDirection_3,
        KNRGCode.KNRGCode_RotaryDirection_4,
        KNRGCode.KNRGCode_RotaryDirection_5,
        KNRGCode.KNRGCode_RotaryDirection_6,
        KNRGCode.KNRGCode_RotaryDirection_7,
        KNRGCode.KNRGCode_RotaryDirection_8,
        KNRGCode.KNRGCode_RotaryDirection_9,
        KNRGCode.KNRGCode_RotaryDirection_10,
        KNRGCode.KNRGCode_RotaryDirection_11,
        KNRGCode.KNRGCode_RotaryDirection_12 -> NavigationIconType.ENTER_ROUNDABOUT

        // 기본적으로 매핑되지 않은 경우 null 반환
        else -> NavigationIconType.NONE
    }
}

@Composable
fun NaviScreen(
    activity: MainActivity,
    viewModel: NaviViewModel = hiltViewModel(),
    guidanceEvent: GuidanceStartEvent.RequestNavGuidance?,
    modifier: Modifier = Modifier,
) {
    // 상태 수집
    val currentLocation by viewModel.currentLocation.collectAsState()
    val safetyGuide by viewModel.safetyGuide.collectAsState()
    val routeGuide by viewModel.routeGuide.collectAsState()

    LaunchedEffect(currentLocation) {
        safetyGuide?.let { sendToCameraInfo(it.safetiesOnGuide, currentLocation) }
        routeGuide?.let { updateToSafetyInfo(it, currentLocation) }
    }

    AndroidView(
        factory = {
            KNNaviView(activity).apply {
                sndVolume = 1f
                useDarkMode = true
                fuelType = KNCarFuel.KNCarFuel_Gasoline
                carType = KNCarType.KNCarType_1
                guideStateDelegate = activity
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            KNSDK.sharedGuidance()?.apply {
                if (guidanceEvent != null) {
                    view.initWithGuidance(
                        this,
                        guidanceEvent.knTrip,
                        guidanceEvent.knRoutePriority,
                        guidanceEvent.curAvoidOptions
                    )
                } else {
                    view.initWithGuidance(
                        this,
                        null,
                        KNRoutePriority.KNRoutePriority_Recommand,
                        KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value
                    )
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                KNNAVEventBus.events.collect { event ->
                    when (event) {
                        is GuidanceEvent.GuidanceCheckingRouteChange -> {
                            view.guidanceCheckingRouteChange(event.guidance)
                        }

                        is GuidanceEvent.GuidanceDidUpdateIndoorRoute -> {
                            view.guidanceDidUpdateIndoorRoute(event.guidance, event.route)
                        }

                        is GuidanceEvent.GuidanceDidUpdateRoutes -> {
                            view.guidanceDidUpdateRoutes(event.guidance, event.routes, event.multiRouteInfo)
                        }

                        is GuidanceEvent.GuidanceGuideStarted -> {
                            view.guidanceGuideStarted(event.guidance)
                        }

                        is GuidanceEvent.GuidanceGuideEnded -> {
                            view.guidanceGuideEnded(event.guidance)
                        }

                        is GuidanceEvent.GuidanceOutOfRoute -> {
                            view.guidanceOutOfRoute(event.guidance)
                        }

                        is GuidanceEvent.GuidanceRouteChanged -> {
                            view.guidanceRouteChanged(event.guidance)
                        }

                        is GuidanceEvent.GuidanceRouteUnchanged -> {
                            view.guidanceRouteUnchanged(event.guidance)
                        }

                        is GuidanceEvent.GuidanceRouteUnchangedWithError -> {
                            view.guidanceRouteUnchangedWithError(event.guidance, event.error)
                        }

                        is GuidanceEvent.DidUpdateCitsGuide -> {
                            view.didUpdateCitsGuide(event.guidance, event.citsGuide)
                        }

                        is GuidanceEvent.GuidanceDidUpdateLocation -> {
                            view.guidanceDidUpdateLocation(event.guidance, event.locationGuide)
                            event.locationGuide.location?.let { viewModel.updateCurrentLocation(it) }
                        }

                        is GuidanceEvent.GuidanceDidUpdateRouteGuide -> {
                            view.guidanceDidUpdateRouteGuide(event.guidance, event.routeGuide)
                            viewModel.updateRouteGuide(event.routeGuide)
                        }

                        is GuidanceEvent.GuidanceDidUpdateSafetyGuide -> {
                            view.guidanceDidUpdateSafetyGuide(event.guidance, event.safetyGuide)
                            viewModel.updateSafetyGuide(event.safetyGuide)
                        }

                        is GuidanceEvent.GuidanceDidUpdateAroundSafeties -> {
                            view.guidanceDidUpdateAroundSafeties(event.guidance, event.safeties)
                        }

                        is GuidanceEvent.ShouldPlayVoiceGuide -> {
                            view.shouldPlayVoiceGuide(event.guidance, event.voiceGuide, event.newData)
                        }

                        is GuidanceEvent.WillPlayVoiceGuide -> {
                            view.willPlayVoiceGuide(event.guidance, event.voiceGuide)
                        }

                        is GuidanceEvent.DidFinishPlayVoiceGuide -> {
                            view.didFinishPlayVoiceGuide(event.guidance, event.voiceGuide)
                        }
                    }
                }
            }
        }
    )
}
