package com.devidea.chevy.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.AddressRepository
import com.devidea.chevy.LocationProvider
import com.devidea.chevy.repository.DataStoreRepository
import com.devidea.chevy.response.Document
import com.devidea.chevy.response.KakaoAddressResponse
import com.kakao.vectormap.LatLng
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class NaviViewModel @Inject constructor(): ViewModel() {

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