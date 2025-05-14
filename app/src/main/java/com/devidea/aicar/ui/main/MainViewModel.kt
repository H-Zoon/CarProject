package com.devidea.aicar.ui.main

import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.App.Companion.instance
import com.devidea.aicar.drive.PIDManager
import com.devidea.aicar.drive.PIDs
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.devidea.aicar.ui.main.components.gaugeItems
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataStoreRepository,
    private val sppClient: SppClient,
    private val drivingRepository: DrivingRepository,
    private val pidManager: PIDManager
) : ViewModel() {

    val enhancedMockDataPoints = listOf(
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T09:59:50Z"), latitude = 37.5660, longitude = 126.9775, rpm = 0,    speed = 0,  engineTemp = 25, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T09:59:55Z"), latitude = 37.5662, longitude = 126.9776, rpm = 800,  speed = 0,  engineTemp = 35, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:00Z"), latitude = 37.5665, longitude = 126.9780, rpm = 1500, speed = 10, engineTemp = 45, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:05Z"), latitude = 37.5668, longitude = 126.9785, rpm = 2000, speed = 25, engineTemp = 50, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:10Z"), latitude = 37.5672, longitude = 126.9790, rpm = 2500, speed = 40, engineTemp = 60, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:15Z"), latitude = 37.5676, longitude = 126.9795, rpm = 3000, speed = 55, engineTemp = 70, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:20Z"), latitude = 37.5680, longitude = 126.9800, rpm = 3500, speed = 70, engineTemp = 80, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:25Z"), latitude = 37.5684, longitude = 126.9805, rpm = 3800, speed = 65, engineTemp = 85, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:30Z"), latitude = 37.5688, longitude = 126.9810, rpm = 4000, speed = 60, engineTemp = 90, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:35Z"), latitude = 37.5692, longitude = 126.9815, rpm = 4200, speed = 55, engineTemp = 95, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:40Z"), latitude = 37.5696, longitude = 126.9820, rpm = 4400, speed = 50, engineTemp = 100, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:45Z"), latitude = 37.5700, longitude = 126.9825, rpm = 3000, speed = 75, engineTemp = 105, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:50Z"), latitude = 37.5704, longitude = 126.9830, rpm = 2500, speed = 80, engineTemp = 110, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:00:55Z"), latitude = 37.5708, longitude = 126.9835, rpm = 2000, speed = 85, engineTemp = 115, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:00Z"), latitude = 37.5712, longitude = 126.9840, rpm = 1500, speed = 90, engineTemp = 120, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:05Z"), latitude = 37.5715, longitude = 126.9842, rpm = 1000, speed = 30, engineTemp = 118, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:06Z"), latitude = 37.5716, longitude = 126.9843, rpm = 900,  speed = 15, engineTemp = 117, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:07Z"), latitude = 37.5717, longitude = 126.9844, rpm = 800,  speed = 0,  engineTemp = 115, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:12Z"), latitude = 37.5717, longitude = 126.9844, rpm = 750,  speed = 0,  engineTemp = 116, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:17Z"), latitude = 37.5718, longitude = 126.9845, rpm = 1200, speed = 10, engineTemp = 118, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:22Z"), latitude = 37.5720, longitude = 126.9847, rpm = 2000, speed = 30, engineTemp = 120, instantKPL = 10f),
        DrivingDataPoint(sessionOwnerId = 1L, timestamp = Instant.parse("2025-05-11T10:01:27Z"), latitude = 37.5722, longitude = 126.9849, rpm = 2500, speed = 35, engineTemp = 130, instantKPL = 10f)
    )

    val gauges = repository.selectedGaugeIds
        .map { ids ->
            ids.mapNotNull { id ->
                gaugeItems.firstOrNull { it.id == id }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onGaugeToggle(id: String) = viewModelScope.launch {
        repository.toggleGauge(id)
    }

    suspend fun swap(from: Int, to: Int) {
        repository.swapGauge(from = from, to = to)
    }

    fun onReset() = viewModelScope.launch {
        repository.resetAllGauges()
    }

    //검색된 블루투스 디바이스 목록
    private val _devices = MutableLiveData<List<ScannedDevice>>()
    val devices: LiveData<List<ScannedDevice>> = _devices

    //블루투스 이벤트 (연결, 해제, 오류 등)
    private val _bluetoothEvent = MutableSharedFlow<ConnectionEvent>()
    val bluetoothEvent: SharedFlow<ConnectionEvent> = _bluetoothEvent.asSharedFlow()

    //카카오 내비 인증 성공 여부를 관리하는 Flow
    private val _authenticationSuccess = MutableStateFlow(false)
    val authenticationSuccess: StateFlow<Boolean> = _authenticationSuccess.asStateFlow()

    //카카오 내비 인증 중 상태를 관리하는 Flow
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    //카카오 내비 인증 에러 메시지 상태를 관리하는 Flow
    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

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

    fun startObserving(pid: PIDs) = pidManager.registerPid(pid)
    fun stopObserving(pid: PIDs) = pidManager.unregisterPid(pid)

    init {
        viewModelScope.launch {
            launch {
                val sessionId = drivingRepository.startSession()

                // 3. mockDataPoints 리스트를 순회하며 saveDataPoint() 호출
                enhancedMockDataPoints.forEach { point ->
                    // point.copy를 써서 sessionOwnerId만 방금 생성된 sessionId로 바꿔줍니다.
                    val toInsert = point.copy(sessionOwnerId = sessionId)
                    drivingRepository.saveDataPoint(toInsert)
                }

                // 4. 세션을 종료(EndTime 업데이트)
                drivingRepository.stopSession(sessionId)

                sppClient.deviceList.collect { list ->
                    _devices.postValue(list)
                }
            }

            launch {
                sppClient.connectionEvents.collect { evt ->
                    _bluetoothEvent.emit(evt)
                }
            }

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
                repository.getDrivingRecodeSetDate().collect { value ->
                    _driveHistoryEnable.value = value
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

    private val _driveHistoryEnable = MutableStateFlow<Boolean>(false)
    val driveHistoryEnable: StateFlow<Boolean> get() = _driveHistoryEnable

    fun setDrivingHistory(value: Boolean){
        viewModelScope.launch {
            repository.setDrivingRecode(value)
        }
    }

    fun setConnectTime(){
        viewModelScope.launch {
            repository.saveConnectData()
        }
    }

    fun setAvrEfficiency(value: Float){
        viewModelScope.launch {
            repository.saveFuelData(value)
        }
    }



    fun startInfo() {
        pidManager.pallStart()
    }

    fun stopInfo() {
        pidManager.stop()
    }

    fun startScan() {
        viewModelScope.launch {
            sppClient.requestStartScan()
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            sppClient.requestStopScan()
        }
    }

    fun connectTo(scannedDevice: ScannedDevice) {
        viewModelScope.launch {
            sppClient.requestConnect(scannedDevice)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            sppClient.requestDisconnect()
        }
    }

    /**
     * Returns a Flow of all driving sessions.
     */
    fun getAllSessions(): Flow<List<DrivingSession>> =
        drivingRepository.getAllSessions()

    fun deleteAllSessions(){
        viewModelScope.launch {
            drivingRepository.deleteAllSessions()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            drivingRepository.deleteSession(sessionId)
        }
    }

    fun restoreSession(session: DrivingSession) {
        viewModelScope.launch { drivingRepository.insertSession(session) }
    }

    fun restoreAllSessions(sessions: List<DrivingSession>) {
        viewModelScope.launch {
            sessions.forEach { drivingRepository.insertSession(it) }
        }
    }
    /**
     * Returns a Flow of data points for the given session ID.
     */
    fun getSessionData(sessionId: Long): Flow<List<DrivingDataPoint>> =
        drivingRepository.getSessionData(sessionId)

    // Slider position for playback control
    private val _sliderPosition = MutableStateFlow(0f)
    val sliderPosition: StateFlow<Float> = _sliderPosition.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /**
     * Update slider position manually (e.g., user drag).
     */
    fun updateSlider(position: Float) {
        _sliderPosition.value = position
    }

    /**
     * Toggle play/pause for record playback animation.
     */
    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }
    // 인증 시작
    fun authenticateUser() {
        CoroutineScope(Dispatchers.IO).launch {
            _isAuthenticating.value = true
            KNSDK.apply {
                initializeWithAppKey(
                    aAppKey = "e31e85ed66b03658041340618628e93f",
                    aClientVersion = "1.0.0",
                    aAppUserId = Settings.Secure.getString(
                        instance.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ),
                    aLangType = KNLanguageType.KNLanguageType_KOREAN,
                    aCompletion = { result ->
                        result?.let {
                            _isAuthenticating.value = false
                            when (it.code) {
                                KNError_Code_C103 -> {
                                    // 인증 실패 처리
                                    _authErrorMessage.value = "인증 실패: 코드 C103"
                                }

                                KNError_Code_C302 -> {
                                    // 권한 오류 처리
                                    _authErrorMessage.value = "권한 오류: 코드 C302"
                                }

                                else -> {
                                    // 기타 오류 처리
                                    _authErrorMessage.value = "초기화 실패: 알 수 없는 오류"
                                }
                            }
                        } ?: run {
                            _authenticationSuccess.value = true
                        }
                    }
                )
            }
        }
    }

    // 에러 메시지 초기화
    fun clearError() {
        _authErrorMessage.value = null
    }
}
