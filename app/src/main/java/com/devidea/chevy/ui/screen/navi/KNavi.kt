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
import com.devidea.chevy.datas.navi.NavigationIconType
import com.devidea.chevy.datas.navi.isCameraType
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
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import com.kakaomobility.knsdk.ui.view.KNNaviView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

suspend fun sendToCameraInfo(safetyGuide: List<KNSafety>?, currentLocation: KNLocation?) {
    withContext(Dispatchers.IO) {
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

                } else {
                    ToDeviceCodec.sendCameraDistance(0, 0, 0)
                    ToDeviceCodec.sendLimitSpeed(0, 0)
                }
            }
        }
    }
}

suspend fun updateToSafetyInfo(routeGuide: KNGuide_Route, currentLocation: KNLocation?) {
    withContext(Dispatchers.IO) {
        // 목표의 거리와 진행 방향 계산
        val distance = routeGuide.curDirection?.location?.let { currentLocation?.distToLocation(it) } ?: 0
        val guidanceAsset = routeGuide.curDirection?.let { findGuideAsset(it.rgCode) } ?: NavigationIconType.NONE

        // 결과 전송
        ToDeviceCodec.sendNextInfo(icon = guidanceAsset.value, distance = distance)

        //차선 정보가 있다면 추천 차선 계산
        routeGuide.lane?.laneInfos?.let {
            val recommendArray = IntArray(it.size)

            // 추천 차선이면 1, 아니면 0을 배열에 저장
            for (i in it.indices) {
                recommendArray[i] = if (it[i].suggest == 1.toByte()) 1 else 0
            }
            //결과 전송
            ToDeviceCodec.sendLaneInfo(recommendArray)
        }?.run {
            ToDeviceCodec.sendLineInfo(distance, 1)
        }
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
    val currentLocation by viewModel.currentLocation.collectAsState()
    val safetyGuide by viewModel.safetyGuide.collectAsState()
    val routeGuide by viewModel.routeGuide.collectAsState()
    val naviGuideState by viewModel.naviGuideState.collectAsState()
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

    // LaunchedEffect는 currentLocation이 변경될 때마다 실행됩니다.
    LaunchedEffect(currentLocation) {
        val currentTime = System.currentTimeMillis()

        // 마지막 호출 시점으로부터 1초(1000ms)가 경과했는지 확인
        if (currentTime - lastCallTime >= 1000) {
            mutex.withLock {
                try {
                    // 쓰로틀 조건을 만족하는 경우 함수 호출
                    /*safetyGuide?.let {
                        sendToCameraInfo(it.safetiesOnGuide, currentLocation)
                    }*/
                    routeGuide?.let {
                        updateToSafetyInfo(it, currentLocation)
                    }

                    // 마지막 호출 시간을 현재 시간으로 업데이트
                    lastCallTime = currentTime
                } catch (e: Exception) {
                    // 에러 처리 로직 (예: 로그 출력, 사용자에게 알림 등)
                    e.printStackTrace()
                }
            }
        }
    }

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
                        knNaviView?.guidanceDidUpdateRoutes(event.guidance, event.routes, event.multiRouteInfo)
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
                        knNaviView?.shouldPlayVoiceGuide(event.guidance, event.voiceGuide, event.newData)
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
                    }

                    is GuidanceEvent.NaviViewGuideState -> {
                        Toast.makeText(activity, "value : ${event.state}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(knNaviView){
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
