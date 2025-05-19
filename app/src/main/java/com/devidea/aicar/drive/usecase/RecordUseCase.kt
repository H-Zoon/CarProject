package com.devidea.aicar.drive.usecase

import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.devidea.aicar.LocationProvider
import com.devidea.aicar.drive.Decoders
import com.devidea.aicar.drive.FuelEconomyUtil.calculateInstantFuelEconomy
import com.devidea.aicar.drive.PIDs
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.notification.NotificationEntity
import com.devidea.aicar.storage.room.notification.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

sealed class RecodeState {
    object Recoding : RecodeState()
    object UnRecoding : RecodeState()
    object Waiting : RecodeState()
}

class RecordUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val drivingRepository: DrivingRepository,
    private val repository: DataStoreRepository,
    private val sppClient: SppClient,
    private val notificationRepository: NotificationRepository,
) {
    companion object {
        const val TAG = "RecordUseCase"
    }

    // 1) 플래그 플로우: DataStore에서 가져오는 자동 기록 설정
    private val flagFlow: Flow<Boolean> = repository
        .getDrivingRecodeSetDate()
        .distinctUntilChanged()

    // 2) 연결 플로우: SPP 클라이언트의 연결 이벤트를 Boolean으로 변환
    private val connectedFlow: Flow<Boolean> = sppClient.connectionEvents
        .map { it is ConnectionEvent.Connected }
        .distinctUntilChanged()
        .onStart { emit(false) }

    // 3) 수동 요청 플로우: MutableStateFlow로 사용자 토글을 받음
    private val _requestRecode = MutableStateFlow(false)
    val requestRecode: StateFlow<Boolean> = _requestRecode.asStateFlow()

    fun setRequestRecode() {
        _requestRecode.value = requestRecode.value != true
    }

    // 4) 세 플로우를 합쳐서 RecodeState 결정
    private val _recordStateFlow: Flow<RecodeState> = combine(
        flagFlow,
        connectedFlow,
        _requestRecode
    ) { autoEnabled, connected, manualRequested ->
        val shouldRecord = autoEnabled || manualRequested
        when {
            !shouldRecord ->
                RecodeState.UnRecoding

            connected ->
                RecodeState.Recoding

            else ->
                RecodeState.Waiting
        }
    }.distinctUntilChanged()

    val recordStateFlow: Flow<RecodeState> = _recordStateFlow

    var sessionId: Long? = null

    init {
        _recordStateFlow
            .onEach { newState ->
                when (newState) {
                    is RecodeState.Recoding -> {
                        sessionId = drivingRepository.startSession()
                        if (sessionId != null) start(sessionId!!)

                        val now = Instant.now()
                        notificationRepository.insertNotification(
                            NotificationEntity(
                                title = "주행 기록 시작",
                                body = "세션 $sessionId 이(가) ${now}에 시작되었습니다.",
                                timestamp = now
                            )
                        )
                    }

                    is RecodeState.UnRecoding -> {
                        if (sessionId != null) {
                            drivingRepository.stopSession(sessionId!!)
                            val now = Instant.now()
                            notificationRepository.insertNotification(
                                NotificationEntity(
                                    title = "주행 기록 종료",
                                    body = "세션 $sessionId 이(가) ${now}에 종료되었습니다.",
                                    timestamp = now
                                )
                            )
                            sessionId = null
                            stop()
                        }
                    }

                    RecodeState.Waiting -> {

                    }
                }
            }.launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectJob: Job? = null

    fun start(sessionId: Long) {
        if (collectJob?.isActive == true) return

        collectJob = scope.launch {
            locationProvider.locationUpdates()
                .collect { location ->
                    launch {
                        recordDataPoint(sessionId, location)
                    }
                }
        }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
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