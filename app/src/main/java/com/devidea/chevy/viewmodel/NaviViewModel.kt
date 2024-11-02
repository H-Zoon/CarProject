package com.devidea.chevy.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.Logger
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class NaviViewModel @Inject constructor() : ViewModel() {

    private val _currentLocation = MutableStateFlow<KNLocation?>(null)
    val currentLocation: StateFlow<KNLocation?> = _currentLocation.asStateFlow()

    private val _routeGuide = MutableStateFlow<KNGuide_Route?>(null)
    val routeGuide: StateFlow<KNGuide_Route?> = _routeGuide.asStateFlow()

    private val _safetyGuide = MutableStateFlow<KNGuide_Safety?>(null)
    val safetyGuide: StateFlow<KNGuide_Safety?> = _safetyGuide.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()


    // 상태 업데이트 메서드
    fun updateCurrentLocation(location: KNLocation) {
        _currentLocation.value = location
    }

    fun updateRouteGuide(routeGuide: KNGuide_Route?) {
        _routeGuide.value = routeGuide
    }

    fun updateSafetyGuide(safetyGuide: KNGuide_Safety?) {
        _safetyGuide.value = safetyGuide
    }

    // 에러 상태 업데이트 메서드
    fun setError(errorMessage: String) {
        _errorState.value = errorMessage
    }

    // 이벤트를 전달하기 위한 SharedFlow
    private val _eventFlow = MutableSharedFlow<GuidanceEvent>()
    val eventFlow: SharedFlow<GuidanceEvent> = _eventFlow

    // 경로 변경 시 호출
    fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceCheckingRouteChange(aGuidance))
        }
    }

    // 실내 경로 업데이트 시 호출
    fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceDidUpdateIndoorRoute(aGuidance, aRoute))
        }
    }

    // 주행 중 경로 변경 시 호출
    fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceDidUpdateRoutes(aGuidance, aRoutes, aMultiRouteInfo))
        }
    }

    // 길 안내 시작 시 호출
    fun guidanceGuideStarted(aGuidance: KNGuidance) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceGuideStarted(aGuidance))
        }
    }

    // 길 안내 종료 시 호출
    fun guidanceGuideEnded(aGuidance: KNGuidance) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceGuideEnded(aGuidance))
        }
    }

    // 경로 이탈 시 호출
    fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceOutOfRoute(aGuidance))
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
            _eventFlow.emit(
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
            _eventFlow.emit(GuidanceEvent.GuidanceRouteUnchanged(aGuidance))
        }
    }

    // 경로 변경 오류 시 호출
    fun guidanceRouteUnchangedWithError(aGuidance: KNGuidance, aError: KNError) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceRouteUnchangedWithError(aGuidance, aError))
        }
    }

    // C-ITS 안내 업데이트 시 호출
    fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.DidUpdateCitsGuide(aGuidance, aCitsGuide))
        }
    }

    // 위치 정보 업데이트 시 호출
    fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceDidUpdateLocation(aGuidance, aLocationGuide))
        }
    }

    // 경로 안내 정보 업데이트 시 호출
    fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceDidUpdateRouteGuide(aGuidance, aRouteGuide))
        }
    }

    // 안전 운행 정보 업데이트 시 호출
    fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide))
        }
    }

    // 주변 안전 정보 업데이트 시 호출
    fun guidanceDidUpdateAroundSafeties(aGuidance: KNGuidance, aSafeties: List<KNSafety>?) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.GuidanceDidUpdateAroundSafeties(aGuidance, aSafeties))
        }
    }

    // 음성 안내 사용 여부 결정 시 호출
    fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.ShouldPlayVoiceGuide(aGuidance, aVoiceGuide, aNewData))
        }
    }

    // 음성 안내 시작 시 호출
    fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.WillPlayVoiceGuide(aGuidance, aVoiceGuide))
        }
    }

    // 음성 안내 종료 시 호출
    fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        viewModelScope.launch {
            _eventFlow.emit(GuidanceEvent.DidFinishPlayVoiceGuide(aGuidance, aVoiceGuide))
        }
    }


    // 경로 생성 메서드 예시
    fun createTrip(start: KNPOI, goal: KNPOI, routePriority: KNRoutePriority, avoidOptions: Int) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                makeTripSuspend(start, goal, routePriority, avoidOptions)
            }
            if (result.knError != null) {
                setError("경로 생성 에러(KNError: ${result.knError})")
            } else {
                // 경로 요청 성공 처리
                //updateRouteGuide(result.knTrip?.routeWithPriority(routePriority, avoidOptions))
            }
        }
    }

    private suspend fun makeTripSuspend(
        start: KNPOI,
        goal: KNPOI,
        priority: KNRoutePriority,
        avoid: Int
    ): TripResult = suspendCoroutine { cont ->
        KNSDK.makeTripWithStart(
            aStart = start,
            aGoal = goal,
            aVias = null,
            aCompletion = { knError, knTrip ->
                cont.resume(TripResult(knError, knTrip))
            }
        )
    }

    data class TripResult(val knError: KNError?, val knTrip: KNTrip?)
}