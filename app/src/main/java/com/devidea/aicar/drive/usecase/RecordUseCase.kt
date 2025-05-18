package com.devidea.aicar.drive.usecase

import android.location.Location
import android.util.Log
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

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

    private val flagFlow: Flow<Boolean> =
        repository.getDrivingRecodeSetDate()
            .distinctUntilChanged()

    private val connectedFlow: Flow<Boolean> =
        sppClient.connectionEvents
            .map { it is ConnectionEvent.Connected }
            .distinctUntilChanged()

    val shouldRecord: Flow<Boolean> = combine(flagFlow, connectedFlow) { flag, connected ->
        flag && connected
    }.distinctUntilChanged()

    var sessionId: Long? = null

    init {
        shouldRecord
            .onEach { enabled ->
                if (enabled) {
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
                } else {
                    if (sessionId != null) drivingRepository.stopSession(sessionId!!)

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
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Default))
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