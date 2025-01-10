package com.devidea.chevy.ui.screen.navi

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.App
import com.devidea.chevy.Logger
import com.devidea.chevy.datas.navi.NavigationIconType
import com.devidea.chevy.datas.navi.isCameraType
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendCameraDistance
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendLaneInfo
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendLimitSpeed
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendNextInfo
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.service.BleService
import com.devidea.chevy.service.BleServiceManager
import com.devidea.chevy.viewmodel.MainViewModel
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNRGCode
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_CitsGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_GuideStateDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_LocationGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_RouteGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_SafetyGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_VoiceGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideState
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import com.kakaomobility.knsdk.ui.view.KNNaviView
import com.kakaomobility.knsdk.ui.view.KNNaviView_GuideStateDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
class KNavi @Inject constructor(
    private val serviceManager: BleServiceManager
) : KNGuidance_GuideStateDelegate,
    KNGuidance_SafetyGuideDelegate,
    KNGuidance_CitsGuideDelegate,
    KNGuidance_LocationGuideDelegate,
    KNGuidance_RouteGuideDelegate,
    KNGuidance_VoiceGuideDelegate,
    KNNaviView_GuideStateDelegate {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var collectionJob: Job? = null

    fun startLocationCollection() {
        if (collectionJob?.isActive == true) return

        collectionJob = scope.launch {
            currentLocation
                .sample(1500) // 1500ms마다 샘플링
                .collect { location ->
                    sendSafety(location)
                    // sendLane()
                    sendGuide(location)
                }
        }
    }

    fun stopLocationCollection() {
        collectionJob?.cancel()
        collectionJob = null
    }

    var lastSafetyState: Boolean = false

    var knNaviView: KNNaviView? = null
    private val bleService by lazy { serviceManager.getService() }

    private val _currentLocation = MutableSharedFlow<KNLocation?>()
    val currentLocation: SharedFlow<KNLocation?> = _currentLocation

    private val _routeGuide = MutableStateFlow<KNGuide_Route?>(null)
    val routeGuide: StateFlow<KNGuide_Route?> = _routeGuide.asStateFlow()

    private val _safetyGuide = MutableStateFlow<KNGuide_Safety?>(null)
    val safetyGuide: StateFlow<KNGuide_Safety?> = _safetyGuide.asStateFlow()

    private val _naviGuideState = MutableStateFlow<KNGuideState?>(null)
    val naviGuideState: StateFlow<KNGuideState?> = _naviGuideState.asStateFlow()

    @Composable
    fun NaviScreen(
        guidanceEvent: GuidanceStartEvent.RequestNavGuidance?
    ) {
        AndroidView(
            factory = {
                KNNaviView(it).apply {
                    sndVolume = 1f
                    useDarkMode = true
                    fuelType = KNCarFuel.KNCarFuel_Gasoline
                    carType = KNCarType.KNCarType_1
                    guideStateDelegate = this@KNavi
                }.also { view ->
                    knNaviView = view
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(Unit) {
            KNSDK.sharedGuidance()?.apply {
                guideStateDelegate = this@KNavi
                locationGuideDelegate = this@KNavi
                routeGuideDelegate = this@KNavi
                safetyGuideDelegate = this@KNavi
                voiceGuideDelegate = this@KNavi
                citsGuideDelegate = this@KNavi
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

    // Delegate Methods
    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        knNaviView?.guidanceCheckingRouteChange(aGuidance)
    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        knNaviView?.guidanceDidUpdateIndoorRoute(aGuidance, aRoute)
    }

    override fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        knNaviView?.guidanceDidUpdateRoutes(
            aGuidance,
            aRoutes,
            aMultiRouteInfo
        )
    }

    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        knNaviView?.guidanceGuideEnded(aGuidance)
    }

    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        knNaviView?.guidanceGuideStarted(aGuidance)
    }

    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        knNaviView?.guidanceOutOfRoute(aGuidance)
    }

    override fun guidanceRouteChanged(
        aGuidance: KNGuidance,
        aFromRoute: KNRoute,
        aFromLocation: KNLocation,
        aToRoute: KNRoute,
        aToLocation: KNLocation,
        aChangeReason: KNGuideRouteChangeReason
    ) {
        knNaviView?.guidanceRouteChanged(aGuidance)
    }

    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        knNaviView?.guidanceRouteUnchanged(aGuidance)
    }

    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        knNaviView?.guidanceRouteUnchangedWithError(aGuidnace, aError)
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        knNaviView?.didUpdateCitsGuide(aGuidance, aCitsGuide)
    }

    override fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        knNaviView?.guidanceDidUpdateLocation(aGuidance, aLocationGuide)
        CoroutineScope(Dispatchers.IO).launch {
            aLocationGuide.location?.let { updateCurrentLocation(it) }
        }
    }

    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        knNaviView?.guidanceDidUpdateRouteGuide(aGuidance, aRouteGuide)
        updateRouteGuide(aRouteGuide)
    }

    override fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        knNaviView?.guidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide)
        updateSafetyGuide(aSafetyGuide)
    }

    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        knNaviView?.guidanceDidUpdateAroundSafeties(guidance, safeties)
    }

    override fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ): Boolean {
        knNaviView?.shouldPlayVoiceGuide(aGuidance, aVoiceGuide, aNewData)
        return true
    }

    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        knNaviView?.willPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        knNaviView?.didFinishPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun naviViewGuideEnded() {
        knNaviView?.guideCancel()
        knNaviView?.guidance?.stop()
    }

    override fun naviViewGuideState(state: KNGuideState) {
        startLocationCollection()
        Logger.i { "$state" }
    }

    private fun sendGuide(currentLocation: KNLocation?) {
        val distance =
            routeGuide.value?.curDirection?.location?.let { currentLocation?.distToLocation(it) }
                ?: 0
        val guidanceAsset =
            routeGuide.value?.curDirection?.let { findGuideAsset(it.rgCode) }
                ?: NavigationIconType.NONE

        if(!lastSafetyState) {
            bleService?.sendNaviMsg(sendNextInfo(icon = guidanceAsset.value, distance = distance))
            Logger.w(shouldUpdate = true) { "안내전송 : ${guidanceAsset.value}, 거리:$distance" }
        }

    }

    private fun sendLane() {
        // laneInfos가 존재하고 비어있지 않은지 확인
        val laneInfos = routeGuide.value?.lane?.laneInfos

        if (laneInfos.isNullOrEmpty()) {
            // 요구조건 1 & 2: routeGuide가 null이거나 laneInfos가 비어있는 경우
            Logger.w(shouldUpdate = true) { "routeGuide가 null이거나 laneInfos가 비어있습니다. 기본값을 전송합니다." }
            bleService?.sendNaviMsg(sendLaneInfo(intArrayOf(0)))
            return
        } else {
            // laneInfos가 존재하고 비어있지 않은 경우
            val recommendArray =
                laneInfos.map { if (it.suggest == 1.toByte()) 1 else 0 }.toIntArray()
            Logger.w(shouldUpdate = true) { "추천차선 : ${recommendArray[0]}" }
            bleService?.sendNaviMsg(sendLaneInfo(recommendArray))
        }
    }

    // 상태 업데이트 메서드
    suspend fun updateCurrentLocation(location: KNLocation) {
        _currentLocation.emit(location)
    }

    fun updateRouteGuide(routeGuide: KNGuide_Route?) {
        _routeGuide.value = routeGuide
    }

    fun updateSafetyGuide(safetyGuide: KNGuide_Safety?) {
        _safetyGuide.value = safetyGuide
    }


    //TODO Test
    private fun sendSafety(currentLocation: KNLocation?) {
        if (safetyGuide.value == null || currentLocation == null) {
            sendDefaultSafetyState()
            return
        }

        val cameraSafeties = safetyGuide.value?.safetiesOnGuide
            ?.filterIsInstance<KNSafety_Camera>()
            ?.filter { it.isCameraType() }
            ?: emptyList()

        if (cameraSafeties.isNotEmpty()) {
            val closestSafety = findClosestSafety(cameraSafeties, currentLocation)
            if (closestSafety != null) {
                sendSafetyMessages(currentLocation, closestSafety)
                return
            } else {
                sendDefaultSafetyState()
                return
            }
        } else {
            sendDefaultSafetyState()
            return
        }
    }

    private fun findClosestSafety(
        safeties: List<KNSafety_Camera>,
        currentLocation: KNLocation
    ): KNSafety_Camera? {
        return safeties.minByOrNull { safety ->
            currentLocation.distToLocation(safety.location)
        }
    }

    private fun sendSafetyMessages(currentLocation: KNLocation, safety: KNSafety_Camera) {
        val speedLimit = safety.speedLimit
        val cameraDistance = currentLocation.distToLocation(safety.location)

        Logger.w(shouldUpdate = true) { "가장 가까운 카메라 - 속도 제한: $speedLimit, 거리: $cameraDistance" }

        bleService?.let { service ->
            service.sendNaviMsg(sendCameraDistance(cameraDistance, 1, 1))
            service.sendNaviMsg(sendLimitSpeed(cameraDistance, speedLimit))
            service.sendNaviMsg(sendNextInfo(icon = 0, distance = cameraDistance))
        } ?: run {
            Logger.e { "BLE 서비스가 초기화되지 않았습니다." }
        }
        lastSafetyState = true
    }

    private fun sendDefaultSafetyState() {
        if (lastSafetyState) {
            Logger.w(shouldUpdate = true) { "기본값을 전송합니다." }
            bleService?.let { service ->
                service.sendNaviMsg(sendCameraDistance(0, 0, 0))
                service.sendNaviMsg(sendLimitSpeed(0, 0))
            } ?: run {
                Logger.e { "BLE 서비스가 초기화되지 않았습니다." }
            }
            lastSafetyState = false
        }
    }
}

