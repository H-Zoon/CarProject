package com.devidea.aicar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devidea.aicar.LocationProvider
import com.devidea.aicar.drive.FuelEconomyUtil.calculateInstantFuelEconomy
import com.devidea.aicar.drive.PollingManager
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingRepository
import com.devidea.aicar.storage.room.notification.NotificationEntity
import com.devidea.aicar.storage.room.notification.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Instant
import android.content.IntentFilter
import com.devidea.aicar.drive.PollingManager.PollingSource
import com.devidea.aicar.drive.SessionSummaryAccumulator
import com.devidea.aicar.storage.room.drive.DrivingSessionSummary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

sealed class RecordState {
    object Recording : RecordState()
    object Stopped : RecordState()
    object Pending : RecordState()
}

object PollingServiceCommand {
    const val EXTRA_MODE = "extra_mode"

    const val MODE_AUTO = "mode_auto"
    const val MODE_MANUAL_START = "mode_manual_start"
    const val MODE_MANUAL_STOP = "mode_manual_stop"
}

@Singleton
class RecordStateHolder @Inject constructor() {
    private val _recordState = MutableStateFlow<RecordState>(RecordState.Stopped)
    val recordState: StateFlow<RecordState> = _recordState.asStateFlow()

    fun update(state: RecordState) {
        _recordState.value = state
    }
}

@AndroidEntryPoint
class PollingService : Service() {

    companion object {
        private const val TAG = "[PollingService]"
        private const val NOTIF_ID = 101
        private const val CHANNEL_ID = "record_channel"
    }

    private var summaryAccumulator: SessionSummaryAccumulator? = null
    private var locationJob: Job? = null
    private var fuelPricePerLiter: Int = 0

    @Inject
    lateinit var sppClient: SppClient

    @Inject
    lateinit var pollingManager: PollingManager

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var drivingRepository: DrivingRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var dataStoreRepository: DataStoreRepository

    @Inject
    lateinit var recordStateHolder: RecordStateHolder

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val currentState = sppClient.connectionEvents.value
            Log.d(TAG, "[PowerReceiver] Received action=$action")

            if (action == Intent.ACTION_POWER_CONNECTED && currentState == ConnectionEvent.Idle) {
                serviceScope.launch {
                    val isAutoConnect = dataStoreRepository.isAutoConnectOnCharge.first()
                    if (isAutoConnect) {
                        Log.d(TAG, "[PowerReceiver] Auto connect 조건 충족, 연결 시도")
                        sppClient.requestAutoConnect()
                    } else {
                        Log.d(TAG, "[PowerReceiver] 자동 연결 설정이 꺼져 있음")
                    }
                }
            }
        }
    }

    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionId: Long? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        registerPowerReceiver()
        monitorRecordingConditions()
        serviceScope.launch {
            dataStoreRepository.fuelCostFlow.collect { cost ->
                fuelPricePerLiter = cost
            }
        }
    }

    private fun registerPowerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
        }
        registerReceiver(powerReceiver, filter)
        Log.d(TAG, "[PollingService] PowerReceiver registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(PollingServiceCommand.EXTRA_MODE)
        //startForegroundService()
        when (mode) {
            PollingServiceCommand.MODE_MANUAL_START -> {
                serviceScope.launch {
                    recordStateHolder.update(RecordState.Recording)
                    startRecording()
                }
            }

            PollingServiceCommand.MODE_MANUAL_STOP -> {
                serviceScope.launch {
                    recordStateHolder.update(RecordState.Stopped)
                    stopRecording()
                }
            }

            PollingServiceCommand.MODE_AUTO, null -> {
                monitorRecordingConditions()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        // 1) 서비스를 위한 별도 스코프 (여기서는 Dispatchers.IO)
        val cleanupScope = CoroutineScope(Dispatchers.IO)

        // 2) cleanupScope.launch로 stopRecordingSuspend()를 순차 실행하게 하고,
        //    끝난 뒤에만 serviceScope.cancel()과 super.onDestroy()를 호출
        runBlocking {
            cleanupScope.launch {
                stopRecording() // suspend 함수
            }.join()  // launch된 코루틴이 끝날 때까지 블록킹

            // 완료되면 리시버 해제 및 스코프 취소
            try {
                unregisterReceiver(powerReceiver)
            } catch (_: Exception) {
            }

            serviceScope.cancel()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun monitorRecordingConditions() {
        serviceScope.launch {
            val autoRecordFlow = dataStoreRepository.getDrivingRecodeSetDate().distinctUntilChanged()

            val connectedFlow = sppClient.connectionEvents
                .map { it is ConnectionEvent.Connected }
                .onStart { emit(false) }
                .distinctUntilChanged()

            combine(autoRecordFlow, connectedFlow) { autoEnabled, connected ->
                when {
                    autoEnabled && connected -> RecordState.Recording
                    autoEnabled && !connected -> RecordState.Pending
                    else -> RecordState.Stopped
                }
            }.distinctUntilChanged()
                .collectLatest { newState ->
                    Log.d(TAG, "[State] 업데이트: $newState")
                    recordStateHolder.update(newState)

                    when (newState) {
                        is RecordState.Recording -> startRecording()
                        is RecordState.Stopped -> {
                            stopRecording()
                            stopSelf() // 서비스 종료
                        }

                        is RecordState.Pending -> {
                            // 대기 중 → 기록은 하지 않음
                            stopRecording() // 필요 시 안전하게 정지
                        }
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun startRecording() {
        if (sessionId != null) return // 이미 기록 중이면 무시

        serviceScope.launch {
            drivingRepository.getOngoingSessionId()?.let {
                sessionId = it
            } ?: run {
                sessionId = drivingRepository.startSession()
            }
            Log.d(TAG, "[Record] 세션 시작: ID = $sessionId")

            notify("주행 기록 시작", "세션 $sessionId 이 시작되었습니다.")

            summaryAccumulator = SessionSummaryAccumulator(fuelPricePerLiter)

            // OBD 수집 시작
            pollingManager.startPall(PollingSource.SERVICE)

            locationProvider.startLocationUpdates()
            // 위치 수집 시작
            locationJob = locationProvider.locationUpdates
                .sample(1_000L)
                .onEach { loc -> sessionId?.let { saveDataPoint(it, loc) } }
                .launchIn(serviceScope)  // ← serviceScope를 직접 넘김

        }
    }

    private suspend fun stopRecording() {
        locationProvider.stopLocationUpdates()
        locationJob?.cancel()
        locationJob = null
        pollingManager.stopAll(PollingSource.SERVICE)

        sessionId?.let {
            Log.d(TAG, "[Record] 세션 종료: ID = $it")
            drivingRepository.stopSession(it)

            notify("주행 기록 종료", "세션 $it 이 종료되었습니다.")
            sessionId = null
        } ?: run {
            drivingRepository.getOngoingSessionId()?.let {
                drivingRepository.stopSession(it)
            }
        }
    }

    private suspend fun saveDataPoint(sessionId: Long, location: Location) {
        try {
            val dataPoint = DrivingDataPoint(
                sessionOwnerId = sessionId,
                timestamp = Instant.now(),
                latitude = location.latitude,
                longitude = location.longitude,
                rpm = pollingManager.rpm.value,
                speed = pollingManager.speed.value,
                engineTemp = pollingManager.ect.value,
                instantKPL = calculateInstantFuelEconomy(
                    maf = pollingManager.maf.value,
                    speedKmh = pollingManager.speed.value,
                    stft = pollingManager.sFuelTrim.value,
                    ltft = pollingManager.lFuelTrim.value
                )
            )

            drivingRepository.saveDataPoint(dataPoint)

            // summaryAccumulator가 null이 아니라면 add() 호출
            summaryAccumulator?.let { acc ->
                val summary = acc.add(dataPoint)
                drivingRepository.saveSessionSummary(
                    DrivingSessionSummary(
                        sessionId = sessionId,
                        totalDistanceKm = summary.distance,
                        averageSpeedKmh = summary.avgSpeed,
                        averageKPL = summary.avgKPL,
                        fuelCost = summary.fuelPrice,
                        accelEvent = summary.accelEvent,
                        brakeEvent = summary.brakeEvent
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Record] 데이터 저장 실패", e)
        }
    }

    private fun notify(title: String, body: String) {
        serviceScope.launch {
            notificationRepository.insertNotification(
                NotificationEntity(
                    title = title,
                    body = body,
                    timestamp = Instant.now()
                )
            )
        }
    }

    private fun startForegroundService() {
        val channel = NotificationChannel(
            CHANNEL_ID, "자동 주행 기록", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "차량 상태 기록을 위한 백그라운드 서비스"
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("자동 주행 기록 활성화됨")
            .setContentText("차량 상태 기록을 위한 서비스를 실행 중입니다.")
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }
}