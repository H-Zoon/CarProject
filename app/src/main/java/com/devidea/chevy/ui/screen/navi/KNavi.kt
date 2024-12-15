package com.devidea.chevy.ui.screen.navi

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.Logger
import com.devidea.chevy.datas.navi.NavigationIconType
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec
import com.devidea.chevy.eventbus.GuidanceEvent
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.eventbus.KNNAVEventBus
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.viewmodel.NaviViewModel
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNRGCode
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import com.kakaomobility.knsdk.ui.view.KNNaviView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/*fun sendGuide(routeGuide: KNGuide_Route?, currentLocation: KNLocation?) {
    val distance =
        routeGuide?.curDirection?.location?.let { currentLocation?.distToLocation(it) } ?: 0
    val guidanceAsset =
        routeGuide?.curDirection?.let { findGuideAsset(it.rgCode) } ?: NavigationIconType.NONE

    ToDeviceCodec.sendNextInfo(icon = guidanceAsset.value, distance = distance)
    Logger.w(shouldUpdate = true) { "안내전송 : ${guidanceAsset.value}, 거리:$distance" }
}

fun sendLane(routeGuide: KNGuide_Route?) {
    // laneInfos가 존재하고 비어있지 않은지 확인
    val laneInfos = routeGuide?.lane?.laneInfos

    if (laneInfos.isNullOrEmpty()) {
        // 요구조건 1 & 2: routeGuide가 null이거나 laneInfos가 비어있는 경우
        Logger.w(shouldUpdate = true) { "routeGuide가 null이거나 laneInfos가 비어있습니다. 기본값을 전송합니다." }
        ToDeviceCodec.sendLaneInfo(intArrayOf(0))
        return
    }

    // laneInfos가 존재하고 비어있지 않은 경우
    val recommendArray = laneInfos.map { if (it.suggest == 1.toByte()) 1 else 0 }.toIntArray()
    Logger.w(shouldUpdate = true) { "추천차선 : ${recommendArray[0]}" }

    ToDeviceCodec.sendLaneInfo(recommendArray)
}

fun sendSafety(safetyGuide: KNGuide_Safety?, currentLocation: KNLocation?) {
    // 요구조건 1: safetyGuide 또는 currentLocation이 null인 경우 기본값 전송 후 종료
    if (safetyGuide == null || currentLocation == null) {
        Logger.w(shouldUpdate = true) { "safetyGuide 또는 currentLocation이 null입니다. 기본값을 전송합니다." }
        ToDeviceCodec.sendCameraDistance(0, 0, 0)
        ToDeviceCodec.sendLimitSpeed(0, 0)
        return
    }

    // 카메라 유형의 안전 정보 필터링 및 KNSafety_Camera 타입으로 캐스팅
    val cameraSafeties = safetyGuide.safetiesOnGuide
        ?.filter { it.isCameraType() }
        ?.filterIsInstance<KNSafety_Camera>()
        ?: emptyList()

    if (cameraSafeties.isNotEmpty()) {
        // 요구조건 2: 가장 가까운 카메라 하나만 선택
        val closestSafety = cameraSafeties.minByOrNull { safety ->
            currentLocation.distToLocation(safety.location)
        }

        if (closestSafety != null) {
            val speedLimit = closestSafety.speedLimit
            val cameraDistance = currentLocation.distToLocation(closestSafety.location)

            Logger.w(shouldUpdate = true) { "가장 가까운 카메라 - 속도 제한: $speedLimit, 거리: $cameraDistance" }

            ToDeviceCodec.sendCameraDistance(cameraDistance, 1, 1)
            ToDeviceCodec.sendLimitSpeed(cameraDistance, speedLimit)
        } else {
            // 예상하지 못한 경우 기본값 전송
            Logger.w(shouldUpdate = true) { "가장 가까운 안전 정보가 존재하지 않습니다. 기본값을 전송합니다." }
            ToDeviceCodec.sendCameraDistance(0, 0, 0)
            ToDeviceCodec.sendLimitSpeed(0, 0)
        }
    } else {
        //  모든 isCameraType()이 false인 경우 기본값 전송
        Logger.w(shouldUpdate = true) { "카메라 유형의 안전 정보가 없습니다. 기본값을 전송합니다." }
        ToDeviceCodec.sendCameraDistance(0, 0, 0)
        ToDeviceCodec.sendLimitSpeed(0, 0)
    }
}*/

fun findGuideAsset(code: KNRGCode): NavigationIconType {
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
    viewModel: NaviViewModel = hiltViewModel(),
    guidanceEvent: GuidanceStartEvent.RequestNavGuidance?,
    modifier: Modifier = Modifier,
) {
    // Mutex를 remember를 사용하여 선언
    val mutex = remember { Mutex() }
    // 마지막 호출 시간을 추적하기 위한 상태 변수
    var lastCallTime by remember { mutableStateOf(0L) }

    val activity = LocalContext.current as MainActivity
    // 상태 수집
    val coroutineScope = rememberCoroutineScope()
    // knNaviView를 상태로 관리
    var knNaviView by remember { mutableStateOf<KNNaviView?>(null) }

    AndroidView(
        factory = {
            KNNaviView(it).apply {
                sndVolume = 1f
                useDarkMode = true
                fuelType = KNCarFuel.KNCarFuel_Gasoline
                carType = KNCarType.KNCarType_1
                guideStateDelegate = activity
            }.also { view ->
                knNaviView = view
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(Unit) {
        KNSDK.sharedGuidance()?.apply {
            guideStateDelegate = activity
            locationGuideDelegate = activity
            routeGuideDelegate = activity
            safetyGuideDelegate = activity
            voiceGuideDelegate = activity
            citsGuideDelegate = activity
        }

        //ToDeviceCodec.notifyIsNaviRunning(1)

        coroutineScope.launch {
            KNNAVEventBus.events.collect { event ->
                when (event) {
                    is GuidanceEvent.GuidanceCheckingRouteChange -> {
                        knNaviView?.guidanceCheckingRouteChange(event.guidance)
                    }

                    is GuidanceEvent.GuidanceDidUpdateIndoorRoute -> {
                        knNaviView?.guidanceDidUpdateIndoorRoute(event.guidance, event.route)
                    }

                    is GuidanceEvent.GuidanceDidUpdateRoutes -> {
                        knNaviView?.guidanceDidUpdateRoutes(
                            event.guidance,
                            event.routes,
                            event.multiRouteInfo
                        )
                    }

                    is GuidanceEvent.GuidanceGuideStarted -> {
                        knNaviView?.guidanceGuideStarted(event.guidance)
                    }

                    is GuidanceEvent.GuidanceGuideEnded -> {
                        knNaviView?.guidanceGuideEnded(event.guidance)
                    }

                    is GuidanceEvent.GuidanceOutOfRoute -> {
                        knNaviView?.guidanceOutOfRoute(event.guidance)
                    }

                    is GuidanceEvent.GuidanceRouteChanged -> {
                        knNaviView?.guidanceRouteChanged(event.guidance)
                    }

                    is GuidanceEvent.GuidanceRouteUnchanged -> {
                        knNaviView?.guidanceRouteUnchanged(event.guidance)
                    }

                    is GuidanceEvent.GuidanceRouteUnchangedWithError -> {
                        knNaviView?.guidanceRouteUnchangedWithError(event.guidance, event.error)
                    }

                    is GuidanceEvent.DidUpdateCitsGuide -> {
                        knNaviView?.didUpdateCitsGuide(event.guidance, event.citsGuide)
                    }

                    is GuidanceEvent.GuidanceDidUpdateLocation -> {
                        knNaviView?.guidanceDidUpdateLocation(event.guidance, event.locationGuide)
                        event.locationGuide.location?.let { viewModel.updateCurrentLocation(it) }
                    }

                    is GuidanceEvent.GuidanceDidUpdateRouteGuide -> {
                        knNaviView?.guidanceDidUpdateRouteGuide(event.guidance, event.routeGuide)
                        viewModel.updateRouteGuide(event.routeGuide)
                    }

                    is GuidanceEvent.GuidanceDidUpdateSafetyGuide -> {
                        knNaviView?.guidanceDidUpdateSafetyGuide(event.guidance, event.safetyGuide)
                        viewModel.updateSafetyGuide(event.safetyGuide)
                    }

                    is GuidanceEvent.GuidanceDidUpdateAroundSafeties -> {
                        knNaviView?.guidanceDidUpdateAroundSafeties(event.guidance, event.safeties)
                    }

                    is GuidanceEvent.ShouldPlayVoiceGuide -> {
                        knNaviView?.shouldPlayVoiceGuide(
                            event.guidance,
                            event.voiceGuide,
                            event.newData
                        )
                    }

                    is GuidanceEvent.WillPlayVoiceGuide -> {
                        knNaviView?.willPlayVoiceGuide(event.guidance, event.voiceGuide)
                    }

                    is GuidanceEvent.DidFinishPlayVoiceGuide -> {
                        knNaviView?.didFinishPlayVoiceGuide(event.guidance, event.voiceGuide)
                    }

                    GuidanceEvent.NaviViewGuideEnded -> {
                        Toast.makeText(activity, "naviViewGuideEnded", Toast.LENGTH_SHORT).show()
                        knNaviView?.guideCancel()
                        knNaviView?.guidance?.stop()
                        KNSDK.sharedGuidance()?.stop()
                        ToDeviceCodec.sendCameraDistance(0, 0, 0)
                        ToDeviceCodec.sendLimitSpeed(0, 0)
                        ToDeviceCodec.sendNextInfo(icon = 0, distance = 0)
                        ToDeviceCodec.sendLaneInfo(intArrayOf(0))
                    }

                    is GuidanceEvent.NaviViewGuideState -> {
                        viewModel.setNaviGuideState(event.state)
                        Toast.makeText(activity, "value : ${event.state}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    LaunchedEffect(knNaviView) {
        KNSDK.sharedGuidance()?.apply {
            knNaviView?.initWithGuidance(
                this,
                guidanceEvent?.knTrip,
                guidanceEvent!!.knRoutePriority,
                guidanceEvent.curAvoidOptions
            )
        }
    }
}
