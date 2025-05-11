package com.devidea.aicar.ui.navi

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.devidea.aicar.Logger
import com.devidea.aicar.Trip
import com.devidea.aicar.k10s.navi.NavigationIconType
import com.devidea.aicar.k10s.navi.findGuideAsset
import com.devidea.aicar.k10s.navi.isCameraType
import com.devidea.aicar.k10s.obd.protocol.codec.Msgs.sendCameraDistance
import com.devidea.aicar.k10s.obd.protocol.codec.Msgs.sendLaneInfo
import com.devidea.aicar.k10s.obd.protocol.codec.Msgs.sendLimitSpeed
import com.devidea.aicar.k10s.obd.protocol.codec.Msgs.sendNextInfo
import com.devidea.aicar.k10s.BleServiceManager
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

@HiltViewModel
class NaviViewModel @Inject constructor(
    context: Context,
    private val serviceManager: BleServiceManager
) :
    AndroidViewModel(context as Application),
    KNGuidance_GuideStateDelegate,
    KNGuidance_SafetyGuideDelegate,
    KNGuidance_CitsGuideDelegate,
    KNGuidance_LocationGuideDelegate,
    KNGuidance_RouteGuideDelegate,
    KNGuidance_VoiceGuideDelegate,
    KNNaviView_GuideStateDelegate {

    var _navView: KNNaviView? = null

    fun provideNavView(context: Context): KNNaviView {
        val view = _navView ?: KNNaviView(context).apply {
            sndVolume = 1f
            useDarkMode = true
            fuelType = KNCarFuel.KNCarFuel_Gasoline
            carType = KNCarType.KNCarType_1
        }.also { _navView = it }
        view.guideStateDelegate = this@NaviViewModel

        KNSDK.sharedGuidance()?.apply {
            guideStateDelegate = this@NaviViewModel
            locationGuideDelegate = this@NaviViewModel
            routeGuideDelegate = this@NaviViewModel
            safetyGuideDelegate = this@NaviViewModel
            voiceGuideDelegate = this@NaviViewModel
            citsGuideDelegate = this@NaviViewModel
        }
        requestNavigate()
        return view
    }

    fun requestNavigate() {
        KNSDK.sharedGuidance()?.apply {
            _navView?.initWithGuidance(
                this,
                Trip.knTrip,
                Trip.curRoutePriority,
                Trip.curAvoidOptions
            )
        }
        //startLocationCollection()
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var collectionJob: Job? = null

    private val bleService by lazy { serviceManager.getService() }

    private val _currentLocation = MutableSharedFlow<KNLocation?>()
    val currentLocation: SharedFlow<KNLocation?> = _currentLocation

    private val _routeGuide = MutableStateFlow<KNGuide_Route?>(null)
    val routeGuide: StateFlow<KNGuide_Route?> = _routeGuide.asStateFlow()

    private val _safetyGuide = MutableStateFlow<KNGuide_Safety?>(null)
    val safetyGuide: StateFlow<KNGuide_Safety?> = _safetyGuide.asStateFlow()

    private val _naviGuideState = MutableStateFlow<KNGuideState?>(null)
    val naviGuideState: StateFlow<KNGuideState?> = _naviGuideState.asStateFlow()

    fun startLocationCollection() {
        if (collectionJob?.isActive == true) return

        collectionJob = scope.launch {
            currentLocation
                .sample(1000)
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


    // Delegate Methods
    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        _navView?.guidanceCheckingRouteChange(aGuidance)
    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        _navView?.guidanceDidUpdateIndoorRoute(aGuidance, aRoute)
    }

    override fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        _navView?.guidanceDidUpdateRoutes(
            aGuidance,
            aRoutes,
            aMultiRouteInfo
        )
    }

    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        _navView?.guidanceGuideEnded(aGuidance)
    }

    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        _navView?.guidanceGuideStarted(aGuidance)
    }

    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        _navView?.guidanceOutOfRoute(aGuidance)
    }

    override fun guidanceRouteChanged(
        aGuidance: KNGuidance,
        aFromRoute: KNRoute,
        aFromLocation: KNLocation,
        aToRoute: KNRoute,
        aToLocation: KNLocation,
        aChangeReason: KNGuideRouteChangeReason
    ) {
        _navView?.guidanceRouteChanged(aGuidance)
    }

    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        _navView?.guidanceRouteUnchanged(aGuidance)
    }

    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        _navView?.guidanceRouteUnchangedWithError(aGuidnace, aError)
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        _navView?.didUpdateCitsGuide(aGuidance, aCitsGuide)
    }

    override fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        _navView?.guidanceDidUpdateLocation(aGuidance, aLocationGuide)
        CoroutineScope(Dispatchers.IO).launch {
            aLocationGuide.location?.let { updateCurrentLocation(it) }
        }
    }

    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        _navView?.guidanceDidUpdateRouteGuide(aGuidance, aRouteGuide)
        updateRouteGuide(aRouteGuide)
    }

    override fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        _navView?.guidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide)
        updateSafetyGuide(aSafetyGuide)
    }

    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        _navView?.guidanceDidUpdateAroundSafeties(guidance, safeties)
    }

    override fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ): Boolean {
        _navView?.shouldPlayVoiceGuide(aGuidance, aVoiceGuide, aNewData)
        return true
    }

    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        _navView?.willPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        _navView?.didFinishPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun naviViewGuideEnded() {
        _navView?.guideCancel()
        _navView?.guidance?.stop()
    }

    override fun naviViewGuideState(state: KNGuideState) {
        //startLocationCollection()
        lastGuideState = state
        Logger.i { "$state" }
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


    var lastSafetyState: Boolean = false
    var lastGuideState: KNGuideState? = null

    private suspend fun sendGuide(currentLocation: KNLocation?) {
        val distance =
            routeGuide.value?.curDirection?.location?.let { currentLocation?.distToLocation(it) }
                ?: 0
        val guidanceAsset =
            routeGuide.value?.curDirection?.let { findGuideAsset(it.rgCode) }
                ?: NavigationIconType.NONE

        if (!lastSafetyState) {
            bleService?.sendNaviMsg(sendNextInfo(icon = guidanceAsset.value, distance = distance))
            Logger.w(shouldUpdate = true) { "안내전송 : ${guidanceAsset.value}, 거리:$distance" }
        }

    }

    private suspend fun sendLane() {
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


    //TODO Test
    private suspend fun sendSafety(currentLocation: KNLocation?) {
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

    private suspend fun sendSafetyMessages(currentLocation: KNLocation, safety: KNSafety_Camera) {
        val speedLimit = safety.speedLimit
        val cameraDistance = currentLocation.distToLocation(safety.location)

        Logger.w(shouldUpdate = true) { "가장 가까운 카메라 - 속도 제한: $speedLimit, 거리: $cameraDistance" }

        bleService?.let { service ->
            service.sendNaviMsg(sendCameraDistance(cameraDistance, 1, 1))
            //service.sendNaviMsg(sendLimitSpeed(cameraDistance, speedLimit))
            //service.sendNaviMsg(sendNextInfo(icon = 0, distance = cameraDistance))
        } ?: run {
            Logger.e { "BLE 서비스가 초기화되지 않았습니다." }
        }
        lastSafetyState = true
    }

    private suspend fun sendDefaultSafetyState() {
        if (lastSafetyState) {
            Logger.w(shouldUpdate = true) { "기본값을 전송합니다." }
            bleService?.let { service ->
                service.sendNaviMsg(sendCameraDistance(0, 0, 0))
                delay(500L)
                service.sendNaviMsg(sendLimitSpeed(0, 0))
            } ?: run {
                Logger.e { "BLE 서비스가 초기화되지 않았습니다." }
            }
            lastSafetyState = false
        }
    }
}
