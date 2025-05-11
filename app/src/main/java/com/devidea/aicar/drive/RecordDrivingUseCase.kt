package com.devidea.aicar.drive

import android.util.Log
import com.devidea.aicar.LocationProvider
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

// 3) Use-Case / ViewModel 레이어: Start/Stop 제어 + 저장
class RecordDrivingUseCase @Inject constructor(
    private val drivingRepository: DrivingRepository,
    private val pidManager: PIDManager,
    private val locationProvider: LocationProvider
) {

    val rpm: StateFlow<Int> = pidManager.rpm
    val speed: StateFlow<Int> = pidManager.speed
    val ect: StateFlow<Int> = pidManager.ect
    val throttle: StateFlow<Int> = pidManager.throttle
    val load: StateFlow<Int> = pidManager.load
    val iat: StateFlow<Int> = pidManager.iat
    val maf: StateFlow<Float> = pidManager.maf
    val batt: StateFlow<Float> = pidManager.batt
    val fuelRate: StateFlow<Float> = pidManager.fuelRate
    val currentGear: StateFlow<Int> = pidManager.currentGear
    val oilPressure: StateFlow<Float> = pidManager.oilPressure
    val oilTemp: StateFlow<Int> = pidManager.oilTemp
    val transFluidTemp: StateFlow<Int> = pidManager.transFluidTemp

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    //위치 업데이트 시작
    fun startLocationUpdates() {
        locationProvider.requestLocationUpdates { location ->
            _userLocation.value = LatLng.from(location.latitude, location.longitude)
        }
    }

    //위치 업데이트 중단
    fun stopLocationUpdate() {
        locationProvider.stopLocationUpdates()
    }

    private var job: Job? = null

    fun start(sessionId: Long) {
        // 1) Job 을 Scope.launch 로 바로 만들어 주면 그 안은 모두 suspend 컨텍스트
        job = CoroutineScope(Dispatchers.IO).launch {
            combine(
                userLocation.filterNotNull(),
                rpm,
                speed,
                ect
            ) { loc, rpm, spd, tmp ->
                DrivingDataPoint(
                    sessionOwnerId = sessionId,
                    timestamp = Instant.now(),
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    rpm = rpm,
                    speed = spd,
                    engineTemp = tmp
                )
            }
                .catch { e ->
                    // 저장 중 에러 로깅
                    Log.e("RecordUseCase", "save error", e)
                }
                .collect { data ->
                    // 이 블록은 suspend 함수 안이므로 saveDataPoint 호출 OK
                    drivingRepository.saveDataPoint(data)
                }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
