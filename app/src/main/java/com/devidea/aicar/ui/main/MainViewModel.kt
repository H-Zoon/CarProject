package com.devidea.aicar.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.drive.PIDManager
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.devidea.aicar.ui.main.components.GaugeItem
import com.devidea.aicar.ui.main.components.gaugeItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 메인 화면의 ViewModel로서 다음 기능을 처리합니다:
 *  - 게이지 선택 및 순서 관리(DataStore)
 *  - 블루투스 기기 스캔 및 연결(SPP Client)
 *  - PID 데이터 업데이트(엔진 파라미터)
 *  - 주행 세션 조회 및 재생
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataStoreRepository,
    private val sppClient: SppClient,
    private val drivingRepository: DrivingRepository,
    private val pidManager: PIDManager
) : ViewModel() {

    //region 게이지 관리

    /**
     * 현재 선택된 게이지 항목 리스트 (표시 순서 유지)
     */
    val gauges: StateFlow<List<GaugeItem>> = repository.selectedGaugeIds
        .map { ids ->
            ids.mapNotNull { id -> gaugeItems.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 지정된 ID의 게이지를 토글합니다. */
    fun onGaugeToggle(id: String) = viewModelScope.launch {
        repository.toggleGauge(id)
    }

    /** 두 게이지 위치를 교체합니다. */
    suspend fun swap(from: Int, to: Int) {
        repository.swapGauge(from = from, to = to)
    }

    /** 모든 게이지를 기본 상태로 초기화합니다. */
    fun onReset() = viewModelScope.launch {
        repository.resetAllGauges()
    }

    //endregion

    //region 블루투스 처리

    private val _devices = MutableLiveData<List<ScannedDevice>>()
    /** 검색된 블루투스 기기 목록을 LiveData로 제공합니다. */
    val devices: LiveData<List<ScannedDevice>> = _devices

    private val _bluetoothEvent = MutableSharedFlow<ConnectionEvent>()
    /** 블루투스 연결/해제/오류 이벤트를 발행합니다. */
    val bluetoothEvent: SharedFlow<ConnectionEvent> = _bluetoothEvent.asSharedFlow()

    /** 블루투스 기기 스캔을 시작합니다. */
    fun startScan() = viewModelScope.launch { sppClient.requestStartScan() }

    /** 블루투스 기기 스캔을 중지합니다. */
    fun stopScan() = viewModelScope.launch { sppClient.requestStopScan() }

    /** 선택한 블루투스 기기에 연결 요청을 보냅니다. */
    fun connectTo(device: ScannedDevice) = viewModelScope.launch { sppClient.requestConnect(device) }

    /** 현재 블루투스 연결을 해제합니다. */
    fun disconnect() = viewModelScope.launch { sppClient.requestDisconnect() }

    //endregion

    //region 엔진 파라미터 스트림

    val rpm: SharedFlow<Int>         = pidManager.rpm
    val speed: SharedFlow<Int>       = pidManager.speed
    val ect: SharedFlow<Int>         = pidManager.ect
    val throttle: SharedFlow<Int>    = pidManager.throttle
    val load: SharedFlow<Int>        = pidManager.load
    val iat: SharedFlow<Int>         = pidManager.iat

    val maf: SharedFlow<Float>       = pidManager.maf
    val batt: SharedFlow<Float>      = pidManager.batt
    val fuelRate: SharedFlow<Float>  = pidManager.fuelRate

    val currentGear: SharedFlow<Int> = pidManager.currentGear
    val oilPressure: SharedFlow<Float> = pidManager.oilPressure
    val oilTemp: SharedFlow<Int>     = pidManager.oilTemp
    val transFluidTemp: SharedFlow<Int> = pidManager.transFluidTemp


    /** 연속 PID 데이터 폴링을 시작합니다. */
    fun startPalling() = pidManager.startPall()

    /** PID 데이터 폴링을 중지합니다. */
    fun stopPalling() = pidManager.stopAll()

    //endregion

    //region 마지막 연결 및 주행 거리 데이터

    private val _lastConnectDate = MutableStateFlow<String>("")
    /** 마지막 블루투스 연결 이후 경과 일수, 알 수 없으면 "-"로 표시합니다. */
    val lastConnectDate: StateFlow<String> = _lastConnectDate

    private val _recentMileage = MutableStateFlow<String>("")
    /** 마지막 기록된 주행 거리(Km), 없으면 "-"로 표시합니다. */
    val recentMileage: StateFlow<String> = _recentMileage

    private val _driveHistoryEnable = MutableStateFlow(false)
    /** 주행 기록 저장 여부 플래그를 나타냅니다. */
    val driveHistoryEnable: StateFlow<Boolean> = _driveHistoryEnable

    /** 오늘 날짜를 DataStore에 마지막 연결 일자로 저장합니다. */
    fun setConnectTime() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        viewModelScope.launch { repository.saveConnectData(today) }
    }

    /** 주행 기록 저장 기능을 활성화/비활성화합니다. */
    fun setDrivingHistory(enabled: Boolean) = viewModelScope.launch {
        repository.setDrivingRecode(enabled)
    }

    //endregion

    //region 주행 세션 캘린더 및 기록

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    /** 세션 조회를 위한 현재 선택된 날짜입니다. */
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _month = MutableStateFlow(YearMonth.now())
    /** 캘린더 UI를 위한 현재 월 정보입니다. */
    val month: StateFlow<YearMonth> = _month

    /** 캘린더 월을 오프셋만큼 변경합니다. */
    fun changeMonth(delta: Int) {
        _month.value = _month.value.plusMonths(delta.toLong())
    }

    /** 선택된 날짜에 해당하는 세션 목록을 스트림으로 제공합니다. 날짜가 null이면 전체 세션을 반환합니다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val sessions: StateFlow<List<DrivingSession>> = _selectedDate
        .flatMapLatest { date ->
            if (date != null) drivingRepository.getSessionsByDate(date)
            else drivingRepository.getAllSessions()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 현재 월에 세션이 있는 날짜들의 집합입니다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val markedDates: StateFlow<Set<LocalDate>> = _month
        .flatMapLatest { ym ->
            val startMillis = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = ym.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            drivingRepository.getSessionsInRange(startMillis, endMillis)
                .map { sessions ->
                    sessions.map { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** 모든 주행 세션을 가져옵니다. */
    fun getAllSessions(): Flow<List<DrivingSession>> = drivingRepository.getAllSessions()
    /** 저장된 모든 주행 세션을 삭제합니다. */
    fun deleteAllSessions() = viewModelScope.launch { drivingRepository.deleteAllSessions() }
    /** 지정한 ID의 세션을 삭제합니다. */
    fun deleteSession(sessionId: Long) = viewModelScope.launch { drivingRepository.deleteSession(sessionId) }
    /** 삭제된 세션을 복원합니다. */
    fun restoreSession(session: DrivingSession) = viewModelScope.launch { drivingRepository.insertSession(session) }
    /** 여러 세션을 한 번에 복원합니다. */
    fun restoreAllSessions(sessions: List<DrivingSession>) = viewModelScope.launch {
        sessions.forEach { drivingRepository.insertSession(it) }
    }
    /** 특정 세션의 상세 데이터 포인트를 가져옵니다. */
    fun getSessionData(sessionId: Long): Flow<List<DrivingDataPoint>> =
        drivingRepository.getSessionData(sessionId)

    //endregion

    //region 재생 컨트롤

    private val _sliderPosition = MutableStateFlow(0f)
    /** 재생 슬라이더의 현재 위치 (0f..1f) 입니다. */
    val sliderPosition: StateFlow<Float> = _sliderPosition

    private val _isPlaying = MutableStateFlow(false)
    /** 재생 애니메이션이 실행 중이면 true입니다. */
    val isPlaying: StateFlow<Boolean> = _isPlaying

    /** 사용자가 슬라이더를 드래그할 때 위치를 업데이트합니다. */
    fun updateSlider(position: Float) { _sliderPosition.value = position }

    /** 재생/일시정지 상태를 토글합니다. */
    fun togglePlay() { _isPlaying.value = !_isPlaying.value }

    /** 캘린더에서 특정 날짜를 선택합니다. */
    fun selectDate(date: LocalDate?) { _selectedDate.value = date ?: LocalDate.now() }

    //endregion

    init {
        viewModelScope.launch {
            // 기기 목록 관찰
            launch { sppClient.deviceList.collect { _devices.postValue(it) } }
            // 연결 이벤트 관찰
            launch { sppClient.connectionEvents.collect { _bluetoothEvent.emit(it) } }
            // 마지막 연결 일수 관찰
            launch { repository.getConnectDate().collect { diff ->
                _lastConnectDate.value = if (diff.isBlank()) "-" else diff
            } }
            // 최근 주행 거리(Km) 관찰
            launch { repository.getMileageDate().collect { km ->
                _recentMileage.value = if (km < 0) "-" else "$km Km"
            } }
            // 주행 기록 저장 여부 관찰
            launch { repository.getDrivingRecodeSetDate().collect { enabled ->
                _driveHistoryEnable.value = enabled
            } }
        }
    }
}

