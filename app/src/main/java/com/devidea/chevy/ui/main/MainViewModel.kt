package com.devidea.chevy.ui.main

import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.App.Companion.instance
import com.devidea.chevy.LocationProvider
import com.devidea.chevy.drive.PIDManager
import com.devidea.chevy.drive.PIDs
import com.devidea.chevy.drive.RecordDrivingUseCase
import com.devidea.chevy.storage.datastore.DataStoreRepository
import com.devidea.chevy.service.ConnectionEvent
import com.devidea.chevy.service.ScannedDevice
import com.devidea.chevy.service.SppClient
import com.devidea.chevy.storage.room.drive.DrivingDataPoint
import com.devidea.chevy.storage.room.drive.DrivingRepository
import com.devidea.chevy.storage.room.drive.DrivingSession
import com.devidea.chevy.ui.main.compose.gauge.gaugeItems
import com.kakao.vectormap.LatLng
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
    private val locationProvider: LocationProvider,
    private val pidManager: PIDManager,
    private val startUseCase: RecordDrivingUseCase,
) : ViewModel() {

    val mockDataPoints = listOf(
        DrivingDataPoint(
            sessionOwnerId = 1L,
            timestamp = Instant.parse("2025-05-11T10:00:00Z"),
            latitude = 37.5665,
            longitude = 126.9780,
            rpm = 1500,
            speed = 40,
            engineTemp = 85
        ),
        DrivingDataPoint(
            sessionOwnerId = 1L,
            timestamp = Instant.parse("2025-05-11T10:00:10Z"),
            latitude = 37.5670,
            longitude = 126.9790,
            rpm = 1600,
            speed = 45,
            engineTemp = 86
        ),
        DrivingDataPoint(
            sessionOwnerId = 1L,
            timestamp = Instant.parse("2025-05-11T10:00:20Z"),
            latitude = 37.5675,
            longitude = 126.9800,
            rpm = 1700,
            speed = 50,
            engineTemp = 88
        ),
        DrivingDataPoint(
            sessionOwnerId = 1L,
            timestamp = Instant.parse("2025-05-11T10:00:30Z"),
            latitude = 37.5680,
            longitude = 126.9810,
            rpm = 1800,
            speed = 55,
            engineTemp = 90
        ),
        DrivingDataPoint(
            sessionOwnerId = 1L,
            timestamp = Instant.parse("2025-05-11T10:00:40Z"),
            latitude = 37.5685,
            longitude = 126.9820,
            rpm = 1900,
            speed = 60,
            engineTemp = 92
        )
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

    // 사용자 위치를 관리하는 Flow
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

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

            /*launch {
                repository.getMileageDate().collect { difference ->
                    _recentMileage.value = when (difference) {
                        -1 -> "-"
                        else -> "$difference Km"
                    }
                }
            }*/

            launch {
                repository.getFuelDate().collect { difference ->
                    _fullEfficiency.value = when (difference) {
                        (-1).toFloat() -> "-"
                        else -> "$difference Km/L"
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

    //위치 업데이트 시작
    fun startLocationUpdates() {
        locationProvider.requestLocationUpdates { location ->
            _userLocation.value = LatLng.from(location.latitude, location.longitude)
        }
    }

    //위치 업데이트 중단
    fun stopLocationUpdate(){
        locationProvider.stopLocationUpdates()
    }

    //주행기록 시작
    fun onStartButtonClicked() {
        viewModelScope.launch {
            val sessionId = drivingRepository.startSession()
            startUseCase.start(sessionId)
        }
    }

    //주행기록 종료
    fun onStopButtonClicked(currentSessionId: Long) {
        startUseCase.stop()
        viewModelScope.launch { drivingRepository.stopSession(currentSessionId) }
    }

    /**
     * Returns a Flow of all driving sessions.
     */
    fun getAllSessions(): Flow<List<DrivingSession>> =
        drivingRepository.getAllSessions()

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
