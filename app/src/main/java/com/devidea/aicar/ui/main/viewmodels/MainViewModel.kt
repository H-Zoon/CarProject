package com.devidea.aicar.ui.main.viewmodels

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.PollingService
import com.devidea.aicar.service.PollingServiceCommand
import com.devidea.aicar.service.RecordState
import com.devidea.aicar.service.RecordStateHolder
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.drive.DrivingDao.MonthlyStats
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.notification.NotificationEntity
import com.devidea.aicar.storage.room.notification.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * 메인 화면의 ViewModel로서 다음 기능을 처리합니다:
 *  - 게이지 선택 및 순서 관리(DataStore)
 *  - 블루투스 기기 스캔 및 연결(SPP Client)
 *  - PID 데이터 업데이트(엔진 파라미터)
 *  - 주행 세션 조회 및 재생
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        @ApplicationContext
        private val context: Context,
        private val repository: DataStoreRepository,
        private val notificationRepository: NotificationRepository,
        private val sppClient: SppClient,
        recordStateHolder: RecordStateHolder,
        drivingRepository: DrivingRepository,
    ) : ViewModel() {
        /** 자동 연결에 사용할 블루투스 기기를 노출합니다.. */
        private val _savedDevice = MutableStateFlow<ScannedDevice?>(null)
        val savedDevice: StateFlow<ScannedDevice?> = _savedDevice

        /** 검색된 블루투스 기기 목록을 LiveData로 제공합니다. */
        private val _devices = MutableLiveData<List<ScannedDevice>>()
        val devices: LiveData<List<ScannedDevice>> = _devices

        /** 블루투스 연결/해제/오류 이벤트를 발행합니다. */
        private val _bluetoothState: MutableStateFlow<ConnectionEvent> =
            MutableStateFlow(ConnectionEvent.Idle)
        val bluetoothState: StateFlow<ConnectionEvent> = _bluetoothState.asStateFlow()

        /** 블루투스 기기 스캔을 시작합니다. */
        fun startScan() = viewModelScope.launch { sppClient.requestStartScan() }

        /** 블루투스 기기 스캔을 중지합니다. */
        fun stopScan() = viewModelScope.launch { sppClient.requestStopScan() }

        /** 선택한 블루투스 기기에 연결 요청을 보냅니다. */
        fun connectTo(device: ScannedDevice) = viewModelScope.launch { sppClient.requestConnect(device) }

        /** 현재 블루투스 연결을 해제합니다. */
        fun disconnect() = viewModelScope.launch { sppClient.requestDisconnect() }

        /** 현재 블루투스 기기를 저장합니다.. */
        fun saveDevice() =
            viewModelScope.launch {
                sppClient.getCurrentConnectedDevice()?.let {
                    repository.saveDevice(it)
                }
            }

        //endregion

        //region 마지막 연결 및 주행 거리 데이터

        /** 마지막 블루투스 연결 이후 경과 일수, 알 수 없으면 "-"로 표시합니다. */
        private val _lastConnectDate = MutableStateFlow<String>("")
        val lastConnectDate: StateFlow<String> = _lastConnectDate

        /** 마지막 기록된 주행 거리(Km), 없으면 "-"로 표시합니다. */
        private val _recentMileage = MutableStateFlow<String>("")
        val recentMileage: StateFlow<String> = _recentMileage

        /** 오늘 날짜를 DataStore에 마지막 연결 일자로 저장합니다. */
        fun setConnectTime() {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            viewModelScope.launch { repository.saveConnectData(today) }
        }

        /** 주행 기록상태 관찰 */
        val recordState: MutableStateFlow<RecordState> = MutableStateFlow<RecordState>(RecordState.Stopped)

        /** 주행 기록 저장 여부 플래그를 나타냅니다. */
        private val _driveHistoryEnable = MutableStateFlow(false)
        val driveHistoryEnable: StateFlow<Boolean> = _driveHistoryEnable

        /** 자동 주행 기록 저장 기능을 활성화/비활성화합니다. */
        fun setAutoDrivingRecordEnable(enabled: Boolean) =
            viewModelScope.launch {
                repository.setDrivingRecode(enabled)
            }

        /** 수동 주행 기록 저장 기능을 활성화/비활성화합니다. */
        fun setManualDrivingRecordToggle() =
            viewModelScope.launch {
                if (recordState.value == RecordState.Stopped) {
                    startManualRecordingService()
                } else {
                    stopManualRecordingService()
                }
            }

        // 자동 기록이 설정된 경우 서비스 시작
        fun startAutoRecordingService() {
        /*val intent = Intent(context, PollingService::class.java).apply {
            putExtra(PollingServiceCommand.EXTRA_MODE, PollingServiceCommand.MODE_AUTO)
        }
        ContextCompat.startForegroundService(context, intent)*/
            val intent = Intent(context, PollingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        // 수동 기록이 시작된 경우 서비스 시작
        fun startManualRecordingService() {
            val intent =
                Intent(context, PollingService::class.java).apply {
                    putExtra(PollingServiceCommand.EXTRA_MODE, PollingServiceCommand.MODE_MANUAL_START)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        // 수동 기록이 종료된 경우 서비스 종료
        fun stopManualRecordingService() {
            val intent =
                Intent(context, PollingService::class.java).apply {
                    putExtra(PollingServiceCommand.EXTRA_MODE, PollingServiceCommand.MODE_MANUAL_STOP)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        //endregion

        //region 알림 관련 데이터

        // 알림 목록 Flow
        val notifications: StateFlow<List<NotificationEntity>> =
            notificationRepository
                .observeAllNotifications()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        // 새 알림 삽입
        fun insertNotification(notification: NotificationEntity) =
            viewModelScope.launch {
                notificationRepository.insertNotification(notification)
            }

        // 개별 알림 읽음 처리
        fun markAsRead(id: Long) =
            viewModelScope.launch {
                notificationRepository.markAsRead(id)
            }

        // 전체 알림 읽음 처리
        fun markAllAsRead() =
            viewModelScope.launch {
                notificationRepository.markAllAsRead()
            }

        // 개별 삭제 / 전체 삭제
        fun deleteNotification(id: Long) =
            viewModelScope.launch {
                notificationRepository.deleteById(id)
            }

        fun clearAllNotifications() =
            viewModelScope.launch {
                notificationRepository.clearAllNotifications()
            }
        //endregion

        //region 특정 달의 주행 요약 데이터

        private val monthRange: Pair<Long, Long> =
            run {
                val cal = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())

                // 이번 달 1일 00:00:00.000
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startMillis = cal.timeInMillis

                // 이번 달 마지막 날 23:59:59.999
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endMillis = cal.timeInMillis

                Pair(startMillis, endMillis)
            }

        private val _monthlyStatsFlow: StateFlow<MonthlyStats> =
            drivingRepository
                .getSessionSummariesInRange(monthRange.first, monthRange.second)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue =
                        MonthlyStats(
                            totalDistanceKm = 0.0f,
                            averageKPL = 0.0f,
                            totalFuelCost = 0,
                        ),
                )

        val monthlyStatsFlow: StateFlow<MonthlyStats>
            get() = _monthlyStatsFlow

        init {
            viewModelScope.launch {
                launch { recordStateHolder.recordState.collect { recordState.value = it } }

                launch { repository.getDevice.collect { _savedDevice.value = it } }
                // 기기 목록 관찰
                launch { sppClient.deviceList.collect { _devices.postValue(it) } }
                // 연결 이벤트 관찰
                launch { sppClient.connectionEvents.collect { _bluetoothState.value = it } }
                // 마지막 연결 일수 관찰
                launch {
                    repository.getConnectDate().collect { diff ->
                        _lastConnectDate.value = if (diff.isBlank()) "-" else diff
                    }
                }

                // 주행 기록 저장 여부 관찰
                launch {
                    repository.getDrivingRecodeSetDate().collect { enabled ->
                        _driveHistoryEnable.value = enabled

                        if (enabled) startAutoRecordingService()
                    }
                }
            }
        }
    }
