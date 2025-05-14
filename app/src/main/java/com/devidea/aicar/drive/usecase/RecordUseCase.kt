package com.devidea.aicar.drive.usecase

import android.location.Location
import android.util.Log
import com.devidea.aicar.LocationProvider
import com.devidea.aicar.drive.Decoders
import com.devidea.aicar.drive.FuelEconomyImproved
import com.devidea.aicar.drive.PIDs
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

class RecordUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val drivingRepository: DrivingRepository,
    private val repository: DataStoreRepository,
    private val sppClient: SppClient,
) {
    companion object{
        const val TAG = "RecordUseCase"
    }

    // 1) DataStore 에서 읽어오는 Boolean 플래그
    private val flagFlow: Flow<Boolean> =
        repository.getDrivingRecodeSetDate()
            .distinctUntilChanged()

    // 2) SppClient 의 연결 상태: Connected 이벤트만 true로
    private val connectedFlow: Flow<Boolean> =
        sppClient.connectionEvents
            .map { it is ConnectionEvent.Connected }
            .distinctUntilChanged()

    // 3) 둘을 묶어서 “기록을 실제로 활성화할 때”만 true
    val shouldRecord: Flow<Boolean> = combine(flagFlow, connectedFlow) { flag, connected ->
        flag && connected
    }
        .distinctUntilChanged()

    init {
        // 4) 변화가 생길 때마다 start/stop 호출
        shouldRecord
            .onEach { enabled ->
                if (enabled) {
                    // 세션 시작한 뒤에 실제 기록 로직
                    val sessionId = drivingRepository.startSession()
                    start(sessionId)
                } else {
                    stop()
                }
            }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }

    // 재사용 가능한 Scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // 위치 수집용 Job
    private var collectJob: Job? = null

    /**
     * 호출 시점부터 stop()이 호출될 때까지 위치 업데이트를 수집하여
     * 각 위치마다 데이터 포인트를 기록합니다.
     */
    fun start(sessionId: Long) {
        // 이미 수집 중이면 무시
        if (collectJob?.isActive == true) return

        collectJob = scope.launch {
            locationProvider.locationUpdates()
                .collect { location ->
                    // 각 위치마다 별도 코루틴으로 데이터 기록
                    launch {
                        recordDataPoint(sessionId, location)
                    }
                }
        }
    }

    /**
     * 모든 위치 수집 및 저장 작업을 중단합니다.
     */
    fun stop() {
        collectJob?.cancel()
        collectJob = null
    }

    /**
     * 위치와 세션 ID를 받아 센서값 조회 → 연비 계산 → DB 저장까지 처리
     */
    private suspend fun recordDataPoint(
        sessionId: Long,
        location: Location
    ) {
        val maf   = startQuery(PIDs.MAF)         ?: 0f
        val rpm   = startQuery(PIDs.RPM)         ?: 0
        val ect   = startQuery(PIDs.ECT)         ?: 0
        val speed = startQuery(PIDs.SPEED)       ?: 0
        val stft  = startQuery(PIDs.S_FUEL_TRIM) ?: 0f
        val ltft  = startQuery(PIDs.L_FUEL_TRIM) ?: 0f
        val baro  = startQuery(PIDs.BAROMETRIC)  ?: 0f

        val instantKPL = FuelEconomyImproved.calculateInstantKPL(
            mafGperS    = maf,
            speedKmh    = speed,
            stftPercent = stft,
            ltftPercent = ltft,
            baroKpa     = baro
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

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Number> startQuery(pid: PIDs): T? {
        return try {
            val resp = sppClient.query(pid.code, header = pid.header, timeoutMs = 1500)
            Log.d(TAG, "[poll] resp=$resp")
            Decoders.parsers[pid]?.invoke(resp) as? T
        } catch (e: Exception) {
            Log.w(TAG, "[poll] NO DATA pid=${pid.code}: ${e.localizedMessage}")
            null
        }
    }
}