package com.devidea.chevy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.eventbus.GuidanceEvent
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.eventbus.KNNAVEventBus
import com.devidea.chevy.eventbus.KNAVStartEventBus
import com.devidea.chevy.eventbus.UIEventBus
import com.devidea.chevy.eventbus.UIEvents
import com.devidea.chevy.repository.device.DataStoreRepository
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideState
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataStoreRepository
) : ViewModel() {

    // NavRoutes를 sealed class로 변경
    sealed class NavRoutes(val route: String) {
        object Home: NavRoutes("home")
        object Details: NavRoutes("details")
        object Map: NavRoutes("map")
        object Nav: NavRoutes("nav")
        object Logs : NavRoutes("log")
    }

    private val _bluetoothStatus = MutableStateFlow(BTState.DISCONNECTED)
    val bluetoothStatus: StateFlow<BTState> get() = _bluetoothStatus

    private val _navigationEvent = MutableStateFlow<NavRoutes>(NavRoutes.Home)
    val navigationEvent: StateFlow<NavRoutes> get() = _navigationEvent

    private val _requestNavGuidance = MutableStateFlow<GuidanceStartEvent.RequestNavGuidance?>(null)
    val requestNavGuidance: StateFlow<GuidanceStartEvent.RequestNavGuidance?> get() = _requestNavGuidance

    init {
        viewModelScope.launch {
            launch {
                repository.getConnectDate().collect { difference ->
                    _lastConnectDate.value =
                        when (difference) {
                            (-1).toLong() -> "-"
                            (0).toLong() -> "방금전"
                            else -> "$difference 일 전"
                        }
                }
            }

            launch {
                repository.getMileageDate().collect { difference ->
                    _recentMileage.value = when (difference) {
                        -1 -> "-"
                        else -> "$difference Km"
                    }
                }
            }

            launch {
                repository.getFuelDate().collect { difference ->
                    _fullEfficiency.value = when (difference) {
                        (-1).toFloat() -> "-"
                        else -> "$difference Km/L"
                    }
                }
            }

            launch {
                UIEventBus.events.collect {
                    when(it){
                        is UIEvents.reuestNavHost -> {
                            _navigationEvent.value = it.value
                        }
                        is UIEvents.reuestBluetooth -> {
                            _bluetoothStatus.value = it.value
                        }
                    }
                }
            }

            launch {
                KNAVStartEventBus.events.collect{
                    when(it){
                        is GuidanceStartEvent.RequestNavGuidance -> {
                            _requestNavGuidance.value = it
                            _navigationEvent.value = NavRoutes.Nav
                        }
                    }
                }
            }
        }
    }

    private val _lastConnectDate = MutableStateFlow<String>("")
    val lastConnectDate: StateFlow<String> get() = _lastConnectDate

    private val _recentMileage = MutableStateFlow<String>("")
    val recentMileage: StateFlow<String> get() = _recentMileage

    private val _fullEfficiency = MutableStateFlow<String>("")
    val fullEfficiency: StateFlow<String> get() = _fullEfficiency

    fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceCheckingRouteChange(aGuidance))
        }
    }

    // 실내 경로 업데이트 시 호출
    fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceDidUpdateIndoorRoute(aGuidance, aRoute))
        }
    }

    // 주행 중 경로 변경 시 호출
    fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceDidUpdateRoutes(aGuidance, aRoutes, aMultiRouteInfo))
        }
    }

    // 길 안내 시작 시 호출
    fun guidanceGuideStarted(aGuidance: KNGuidance) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceGuideStarted(aGuidance))
        }
    }

    // 길 안내 종료 시 호출
    fun guidanceGuideEnded(aGuidance: KNGuidance) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceGuideEnded(aGuidance))
        }
    }

    // 경로 이탈 시 호출
    fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceOutOfRoute(aGuidance))
        }
    }

    // 경로 변경 시 호출
    fun guidanceRouteChanged(
        aGuidance: KNGuidance,
        aFromRoute: KNRoute,
        aFromLocation: KNLocation,
        aToRoute: KNRoute,
        aToLocation: KNLocation,
        aChangeReason: KNGuideRouteChangeReason
    ) {
        viewModelScope.launch {
            KNNAVEventBus.post(
                GuidanceEvent.GuidanceRouteChanged(
                    aGuidance,
                    aFromRoute,
                    aFromLocation,
                    aToRoute,
                    aToLocation,
                    aChangeReason
                )
            )
        }
    }

    // 경로가 변경되지 않았을 때 호출
    fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceRouteUnchanged(aGuidance))
        }
    }

    // 경로 변경 오류 시 호출
    fun guidanceRouteUnchangedWithError(aGuidance: KNGuidance, aError: KNError) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceRouteUnchangedWithError(aGuidance, aError))
        }
    }

    // C-ITS 안내 업데이트 시 호출
    fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.DidUpdateCitsGuide(aGuidance, aCitsGuide))
        }
    }

    // 위치 정보 업데이트 시 호출
    fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceDidUpdateLocation(aGuidance, aLocationGuide))
        }
    }

    // 경로 안내 정보 업데이트 시 호출
    fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceDidUpdateRouteGuide(aGuidance, aRouteGuide))
        }
    }

    // 안전 운행 정보 업데이트 시 호출
    fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide))
        }
    }

    // 주변 안전 정보 업데이트 시 호출
    fun guidanceDidUpdateAroundSafeties(aGuidance: KNGuidance, aSafeties: List<KNSafety>?) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.GuidanceDidUpdateAroundSafeties(aGuidance, aSafeties))
        }
    }

    // 음성 안내 사용 여부 결정 시 호출
    fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.ShouldPlayVoiceGuide(aGuidance, aVoiceGuide, aNewData))
        }
    }

    // 음성 안내 시작 시 호출
    fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.WillPlayVoiceGuide(aGuidance, aVoiceGuide))
        }
    }

    // 음성 안내 종료 시 호출
    fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.DidFinishPlayVoiceGuide(aGuidance, aVoiceGuide))
        }
    }

    fun naviViewGuideEnded() {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.NaviViewGuideEnded)
        }
    }

    fun naviViewGuideState(state: KNGuideState) {
        viewModelScope.launch {
            KNNAVEventBus.post(GuidanceEvent.NaviViewGuideState(state))
        }
    }
}
