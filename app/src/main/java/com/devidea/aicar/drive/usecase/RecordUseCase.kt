package com.devidea.aicar.drive.usecase

import android.location.Location
import android.util.Log
import com.devidea.aicar.LocationProvider
import com.devidea.aicar.drive.Decoders
import com.devidea.aicar.drive.FuelEconomyUtil.calculateInstantFuelEconomy
import com.devidea.aicar.drive.ObdPollingManager
import com.devidea.aicar.drive.PIDs
import com.devidea.aicar.module.AppModule
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.devidea.aicar.storage.room.notification.NotificationEntity
import com.devidea.aicar.storage.room.notification.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

sealed class RecordState {
    object Recording : RecordState()
    object Stopped : RecordState()
    object Pending : RecordState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecordUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val drivingRepository: DrivingRepository,
    private val repository: DataStoreRepository,
    private val sppClient: SppClient,
    private val obdPollingManager: ObdPollingManager,
    private val notificationRepository: NotificationRepository,
    @AppModule.ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        const val TAG = "RecordUseCase"
    }

    private var locationJob: Job? = null

    // 기록중인 세션이 있는지 확인
    private val onGoingFlow: StateFlow<DrivingSession?> =
        drivingRepository.getOngoingSession()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = null
            )


    // 자동 기록 설정
    private val flagFlow: Flow<Boolean> = repository
        .getDrivingRecodeSetDate()
        .distinctUntilChanged()

    // 장치 연결 상태
    private val connectedFlow: Flow<Boolean> = sppClient.connectionEvents
        .map { it is ConnectionEvent.Connected }
        .distinctUntilChanged()
        .onStart { emit(false) }

    // 수동 기록 제어
    private val manualFlow: Flow<Boolean> = repository
        .getManualDrivingRecodeSetDate()
        .distinctUntilChanged()

    /** 수동 기록 시작 */
    suspend fun startManualRecording() {
        repository.setManualDrivingRecode(true)
    }

    /** 수동 기록 중지 */
    suspend fun stopManualRecording() {
        repository.setManualDrivingRecode(false)
    }

    // 상태 결정: 수동 OR (자동 + 연결)
    val recordState: StateFlow<RecordState> = combine(
        manualFlow,
        flagFlow,
        connectedFlow
    ) { manual, autoEnabled, connected ->
        val isActive = manual || autoEnabled
        when {
            isActive && connected -> RecordState.Recording
            isActive -> RecordState.Pending
            else -> RecordState.Stopped
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = RecordState.Stopped
        )

    private var currentSessionId: Long? = null

    private suspend fun startSession(sessionId: Long) {
        val now = Instant.now()
        notificationRepository.insertNotification(
            NotificationEntity(
                title = "주행 기록 시작",
                body = "세션 $sessionId 이(가) $now 에 시작되었습니다.",
                timestamp = now
            )
        )
    }

    private suspend fun stopSession(sessionId: Long) {
        drivingRepository.stopSession(sessionId)
        val now = Instant.now()
        notificationRepository.insertNotification(
            NotificationEntity(
                title = "주행 기록 종료",
                body = "세션 $sessionId 이(가) $now 에 종료되었습니다.",
                timestamp = now
            )
        )
    }

    init {
        // 자동 기록 설정이 true(활성화)일 때마다 수동 기록 모드를 false 로 변경
        flagFlow.filter { it }  // true 일 때만
            .onEach {
                // 자동 기록이 켜졌으니 수동 기록은 꺼준다
                scope.launch {
                    repository.setManualDrivingRecode(false)
                }
            }
            .launchIn(scope)

        scope.launch {
            recordState.collect { state ->
                when(state){
                    is RecordState.Recording -> {
                        if(onGoingFlow.value?.sessionId == null){
                            currentSessionId = drivingRepository.startSession()
                            startSession(currentSessionId!!)
                        } else {
                            currentSessionId = onGoingFlow.value?.sessionId!!
                        }
                        Log.e("sesstion start", currentSessionId.toString())
                        obdPollingManager.startPall()
                        locationJob = scope.launch {
                            locationProvider.locationUpdates()
                                .collect { loc -> recordDataPoint(currentSessionId!!, loc) }
                        }
                    }
                    is RecordState.Stopped ->{
                        locationJob?.cancel()
                        obdPollingManager.stopAll()
                        if(currentSessionId != null){
                            stopSession(currentSessionId!!)
                        } else {
                            onGoingFlow.value?.sessionId?.let {
                                stopSession(it)
                            }
                        }
                        currentSessionId = null
                    }

                    RecordState.Pending -> {

                    }
                }
            }
        }
    }

    private suspend fun recordDataPoint(
        sessionId: Long,
        location: Location
    ) {
        /*val maf = safeQuery(PIDs.MAF)?.toFloat() ?: 0f
        val rpm = safeQuery(PIDs.RPM)?.toInt() ?: 0
        val ect = safeQuery(PIDs.ECT)?.toInt() ?: 0
        val speed = safeQuery(PIDs.SPEED)?.toInt() ?: 0
        val stft = safeQuery(PIDs.S_FUEL_TRIM)?.toFloat() ?: 0f
        val ltft = safeQuery(PIDs.L_FUEL_TRIM)?.toFloat() ?: 0f*/
        val maf = obdPollingManager.maf
        val rpm = obdPollingManager.rpm
        val ect = obdPollingManager.ect
        val speed = obdPollingManager.speed
        val stft = obdPollingManager.sFuelTrim
        val ltft = obdPollingManager.lFuelTrim

        Log.e("rpm", rpm.value.toString())

        val instantKPL = calculateInstantFuelEconomy(
            maf = maf.value,
            speedKmh = speed.value,
            stft = stft.value,
            ltft = ltft.value
        )

        val dataPoint = DrivingDataPoint(
            sessionOwnerId = sessionId,
            timestamp = Instant.now(),
            latitude = location.latitude,
            longitude = location.longitude,
            rpm = rpm.value,
            speed = speed.value,
            engineTemp = ect.value,
            instantKPL = instantKPL
        )

        try {
            drivingRepository.saveDataPoint(dataPoint)
        } catch (e: Exception) {
            Log.e("RecordUseCase", "saveDataPoint error", e)
        }
    }

    private val queryMutex = Mutex()

    suspend fun safeQuery(pid: PIDs): Number? =
        withTimeoutOrNull(1500) {
            queryMutex.withLock {
                val resp = sppClient.query(pid.code, header = pid.header)
                Decoders.parsers[pid]?.invoke(resp)
            }
        }
}