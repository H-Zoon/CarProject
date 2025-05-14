package com.devidea.aicar.drive

import com.devidea.aicar.service.SppClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.devidea.aicar.storage.datastore.DataStoreRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PIDManager @Inject constructor(
    private val sppClient: SppClient,
    private val dataStoreRepository: DataStoreRepository
) {

    companion object {
        private const val TAG = "PIDManager"

        /** gaugeId(String) → PIDs?  변환 함수 */
        private fun gaugeIdToPid(id: String): PIDs? =
            PIDs.entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
                ?: PIDs.fromCode(id)
    }

    private val liveFrames: SharedFlow<String> = sppClient.liveFrames
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null
    private var pollPeriodMs = 500L
    private var gaugeJob: Job?    = null   // ① 게이지 목록 감시
    private var pollCtlJob: Job?  = null   // ② restartPollLoop 트리거

    // Int 타입 PID 초기값
    private val initialIntValues: Map<PIDs, Int> = mapOf(
        PIDs.RPM to 0,
        PIDs.SPEED to 0,
        PIDs.ECT to 0,
        PIDs.THROTTLE to 0,
        PIDs.LOAD to 0,
        PIDs.IAT to 0,
        PIDs.CURRENT_GEAR to 0,
        PIDs.OIL_TEMP to 0,
        PIDs.TRANS_FLUID_TEMP to 0
    )

    // Float 타입 PID 초기값
    private val initialFloatValues: Map<PIDs, Float> = mapOf(
        PIDs.MAF to 0f,
        PIDs.BATT to 0f,
        PIDs.FUEL_RATE to 0f,
        PIDs.OIL_PRESSURE to 0f
    )

    private val intFlows: Map<PIDs, StateFlow<Int>> = initialIntValues.mapValues { (pid, init) ->
        val prefix = pid.responsePrefixOrNull()        // ← 여기
        liveFrames
            .map { it.replace(" ", "").uppercase() }   // 프레임 정규화
            .filter { prefix != null && it.startsWith(prefix) }
            .map { Decoders.parsers.getValue(pid)(it) as Int }
            .stateIn(scope, SharingStarted.Eagerly, init)
    }

    private val floatFlows: Map<PIDs, StateFlow<Float>> =
        initialFloatValues.mapValues { (pid, init) ->
            val prefix = pid.responsePrefixOrNull()
            liveFrames
                .map { it.replace(" ", "").uppercase() }
                .filter { prefix != null && it.startsWith(prefix) }
                .map { Decoders.parsers.getValue(pid)(it) as Float }
                .stateIn(scope, SharingStarted.Eagerly, init)
        }

    // 편의 프로퍼티
    val rpm: StateFlow<Int> get() = intFlows.getValue(PIDs.RPM)
    val speed: StateFlow<Int> get() = intFlows.getValue(PIDs.SPEED)
    val ect: StateFlow<Int> get() = intFlows.getValue(PIDs.ECT)
    val throttle: StateFlow<Int> get() = intFlows.getValue(PIDs.THROTTLE)
    val load: StateFlow<Int> get() = intFlows.getValue(PIDs.LOAD)
    val iat: StateFlow<Int> get() = intFlows.getValue(PIDs.IAT)

    val maf: StateFlow<Float> get() = floatFlows.getValue(PIDs.MAF)
    val batt: StateFlow<Float> get() = floatFlows.getValue(PIDs.BATT)
    val fuelRate: StateFlow<Float> get() = floatFlows.getValue(PIDs.FUEL_RATE)

    val currentGear: StateFlow<Int> get() = intFlows.getValue(PIDs.CURRENT_GEAR)
    val oilPressure: StateFlow<Float> get() = floatFlows.getValue(PIDs.OIL_PRESSURE)
    val oilTemp: StateFlow<Int> get() = intFlows.getValue(PIDs.OIL_TEMP)
    val transFluidTemp: StateFlow<Int> get() = intFlows.getValue(PIDs.TRANS_FLUID_TEMP)

    private val _activePids = MutableStateFlow<Set<PIDs>>(emptySet())
    val activePids: StateFlow<Set<PIDs>> = _activePids.asStateFlow()

    private fun restartPollLoop(pidSet: Set<PIDs>) {
        pollJob?.cancel()
        if (pidSet.isEmpty()) {
            Log.d(TAG, "[poll] stopped (empty set)")
            return
        }

        Log.d(TAG, "[poll] start, period=$pollPeriodMs ms, pids=$pidSet")
        pollJob = scope.launch {
            while (isActive) {
                for (pid in pidSet) {
                    try {
                        sppClient.query(pid.code, header = pid.header, timeoutMs = 1_500)
                    } catch (e: Exception) {
                        Log.w(TAG, "[poll] ${pid.name} : ${e.localizedMessage}")
                    }
                }
                delay(pollPeriodMs)
            }
        }
    }

    fun startPall() {
        if (gaugeJob?.isActive == true && pollCtlJob?.isActive == true) return

        gaugeJob = scope.launch {
            dataStoreRepository.selectedGaugeIds            // Flow<List<String>>
                .map { it.mapNotNull(::gaugeIdToPid).toSet() }
                .distinctUntilChanged()
                .collect(_activePids::value::set)
        }

        pollCtlJob = scope.launch {
            _activePids
                .collect(::restartPollLoop)
        }

        // 초기 값으로 한 번 돌려서 즉시 폴링 시작
        restartPollLoop(_activePids.value)
        Log.d(TAG, "[obs] observers started")
    }

    fun stopAll() {
        gaugeJob?.cancel()
        pollCtlJob?.cancel()
        pollJob?.cancel()        // restartPollLoop 가 만든 실제 폴링 루프
        gaugeJob = null
        pollCtlJob = null
        pollJob  = null
        Log.d(TAG, "[obs] observers stopped")
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

    private fun PIDs.responsePrefixOrNull(): String? = runCatching {
        val clean = code.trim().uppercase()
        if (clean.length < 2) return null          // 최소 1바이트도 안 되면 탈락

        val service = clean.substring(0, 2).toInt(16)
        val respService = (service + 0x40) and 0xFF   // 0x40 더해 응답 서비스
        val respServiceHex = "%02X".format(respService)

        respServiceHex + clean.substring(2)           // 서비스+PID 바이트 결합
    }.getOrNull()
}