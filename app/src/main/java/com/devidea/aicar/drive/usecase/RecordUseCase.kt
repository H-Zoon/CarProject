package com.devidea.aicar.drive.usecase

import android.location.Location
import android.util.Log
import com.devidea.aicar.LocationProvider
import com.devidea.aicar.drive.Decoders
import com.devidea.aicar.drive.FuelEconomyUtil.calculateInstantFuelEconomy
import com.devidea.aicar.drive.PIDs
import com.devidea.aicar.module.AppModule
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.notification.NotificationEntity
import com.devidea.aicar.storage.room.notification.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

sealed class RecordState {
    object Recording : RecordState()
    object Stopped   : RecordState()
    object Pending   : RecordState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecordUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val drivingRepository: DrivingRepository,
    private val repository: DataStoreRepository,
    private val sppClient: SppClient,
    private val notificationRepository: NotificationRepository,
    @AppModule.ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        const val TAG = "RecordUseCase"
    }

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
    private val manualFlow = MutableStateFlow(false)

    /** 수동 기록 시작 */
    fun startManualRecording() {
        manualFlow.value = true
    }

    /** 수동 기록 중지 */
    fun stopManualRecording() {
        manualFlow.value = false
    }

    // 상태 결정: 수동 OR (자동 + 연결)
    val recordState: StateFlow<RecordState> = combine(
        manualFlow,
        flagFlow,
        connectedFlow
    ) { manual, autoEnabled, connected ->
        when {
            manual -> RecordState.Recording
            autoEnabled && !connected -> RecordState.Pending
            autoEnabled && connected    -> RecordState.Recording
            else -> RecordState.Stopped
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = RecordState.Stopped
        )

    // 세션 ID 흐름 및 시작/종료 핸들링
    private val sessionFlow: StateFlow<Long?> = recordState
        .map { state ->
            if (state == RecordState.Recording) drivingRepository.startSession()
            else null
        }
        .distinctUntilChanged()
        .onEach { newSessionId -> handleSessionTransition(newSessionId) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private var currentSessionId: Long? = null

    private suspend fun handleSessionTransition(newSessionId: Long?) {
        val previous = currentSessionId
        if (previous == null && newSessionId != null) {
            // 세션 시작
            startSession(newSessionId)
        } else if (previous != null && newSessionId == null) {
            // 세션 종료
            stopSession(previous)
        }
        currentSessionId = newSessionId
    }

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
        // 위치 수집: 세션 ID 있을 때만
        sessionFlow
            .filterNotNull()
            .flatMapLatest { id ->
                locationProvider.locationUpdates()
                    .onEach { loc -> recordDataPoint(id, loc) }
            }
            .launchIn(scope)
    }

    private suspend fun recordDataPoint(
        sessionId: Long,
        location: Location
    ) {
        val maf = safeQuery(PIDs.MAF)?.toFloat() ?: 0f
        val rpm = safeQuery(PIDs.RPM)?.toInt() ?: 0
        val ect = safeQuery(PIDs.ECT)?.toInt() ?: 0
        val speed = safeQuery(PIDs.SPEED)?.toInt() ?: 0
        val stft = safeQuery(PIDs.S_FUEL_TRIM)?.toFloat() ?: 0f
        val ltft = safeQuery(PIDs.L_FUEL_TRIM)?.toFloat() ?: 0f

        val instantKPL = calculateInstantFuelEconomy(
            maf = maf,
            speedKmh = speed,
            stft = stft,
            ltft = ltft
        )

        val dataPoint = DrivingDataPoint(
            sessionOwnerId = sessionId,
            timestamp = Instant.now(),
            latitude = location.latitude,
            longitude = location.longitude,
            rpm = rpm,
            speed = speed,
            engineTemp = ect,
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