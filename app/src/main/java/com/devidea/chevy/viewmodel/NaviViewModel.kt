package com.devidea.chevy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.Logger
import com.devidea.chevy.datas.navi.NavigationIconType
import com.devidea.chevy.datas.navi.isCameraType
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec
import com.devidea.chevy.ui.screen.navi.findGuideAsset
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideState
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class NaviViewModel @Inject constructor() : ViewModel() {

    private val _currentLocation = MutableSharedFlow<KNLocation>()
    val currentLocation: SharedFlow<KNLocation> = _currentLocation

    private val _routeGuide = MutableStateFlow<KNGuide_Route?>(null)
    val routeGuide: StateFlow<KNGuide_Route?> = _routeGuide.asStateFlow()

    private val _safetyGuide = MutableStateFlow<KNGuide_Safety?>(null)
    val safetyGuide: StateFlow<KNGuide_Safety?> = _safetyGuide.asStateFlow()

    private val _naviGuideState = MutableStateFlow<KNGuideState?>(null)
    val naviGuideState: StateFlow<KNGuideState?> = _naviGuideState.asStateFlow()

    init {
        viewModelScope.launch {
            currentLocation
                .debounce(1000L) // 1초 동안 대기, 마지막 값만 방출
                .collect { location ->
                    sendSafety(location)
                    delay(500L)
                    sendLane()
                    delay(500L)
                    sendGuide(location)
                }
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

    // 에러 상태 업데이트 메서드
    fun setNaviGuideState(knGuideState: KNGuideState?) {
        _naviGuideState.value = knGuideState
    }

    private fun sendGuide(currentLocation: KNLocation?) {
        val distance =
            routeGuide.value?.curDirection?.location?.let { currentLocation?.distToLocation(it) }
                ?: 0
        val guidanceAsset =
            routeGuide.value?.curDirection?.let { findGuideAsset(it.rgCode) }
                ?: NavigationIconType.NONE

        ToDeviceCodec.sendNextInfo(icon = guidanceAsset.value, distance = distance)
        Logger.w(shouldUpdate = true) { "안내전송 : ${guidanceAsset.value}, 거리:$distance" }
    }

    private fun sendLane() {
        // laneInfos가 존재하고 비어있지 않은지 확인
        val laneInfos = routeGuide.value?.lane?.laneInfos

        if (laneInfos.isNullOrEmpty()) {
            // 요구조건 1 & 2: routeGuide가 null이거나 laneInfos가 비어있는 경우
            Logger.w(shouldUpdate = true) { "routeGuide가 null이거나 laneInfos가 비어있습니다. 기본값을 전송합니다." }
            ToDeviceCodec.sendLaneInfo(intArrayOf(0))
            return
        } else {
            // laneInfos가 존재하고 비어있지 않은 경우
            val recommendArray =
                laneInfos.map { if (it.suggest == 1.toByte()) 1 else 0 }.toIntArray()
            Logger.w(shouldUpdate = true) { "추천차선 : ${recommendArray[0]}" }
            ToDeviceCodec.sendLaneInfo(recommendArray)
        }
    }

    private fun sendSafety(currentLocation: KNLocation?) {
        // 요구조건 1: safetyGuide 또는 currentLocation이 null인 경우 기본값 전송 후 종료
        if (safetyGuide.value == null || currentLocation == null) {
            Logger.w(shouldUpdate = true) { "safetyGuide 또는 currentLocation이 null입니다. 기본값을 전송합니다." }
            ToDeviceCodec.sendCameraDistance(0, 0, 0)
            ToDeviceCodec.sendLimitSpeed(0, 0)
            return
        }

        // 카메라 유형의 안전 정보 필터링 및 KNSafety_Camera 타입으로 캐스팅
        val cameraSafeties = safetyGuide.value?.safetiesOnGuide
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
                if(naviGuideState.value == KNGuideState.KNGuideState_OnSafetyGuide){
                    ToDeviceCodec.sendNextInfo(
                        icon = 0,
                        distance = cameraDistance
                    )
                }
                return
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
    }
}