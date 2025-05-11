package com.devidea.chevy.drive

import com.devidea.chevy.service.SppClient
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicInteger

class PIDManager @Inject constructor(private val sppClient: SppClient) {

    companion object {
        private const val TAG = "GaugeManager"
    }

    private val liveFrames: SharedFlow<String> = sppClient.liveFrames
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null
    private var collectJob: Job? = null

    // Int 타입 PID 초기값
    private val initialIntValues: Map<PIDs, Int> = mapOf(
        PIDs.RPM      to 0,
        PIDs.SPEED    to 0,
        PIDs.ECT      to 0,
        PIDs.THROTTLE to 0,
        PIDs.LOAD     to 0,
        PIDs.IAT      to 0,
        PIDs.CURRENT_GEAR to 0,
        PIDs.OIL_TEMP to 0,
        PIDs.TRANS_FLUID_TEMP to 0
    )

    // Float 타입 PID 초기값
    private val initialFloatValues: Map<PIDs, Float> = mapOf(
        PIDs.MAF       to 0f,
        PIDs.BATT      to 0f,
        PIDs.FUEL_RATE to 0f,
        PIDs.OIL_PRESSURE to 0f
    )

    // Int PID → StateFlow<Int> 맵
    private val intFlows: Map<PIDs, StateFlow<Int>> = initialIntValues.mapValues { (pid, initial) ->
        liveFrames
            .filter { it.startsWith("41${pid.code}") }
            .map { frame -> Decoders.parsers.getValue(pid)(frame) as Int }
            .stateIn(scope, SharingStarted.Eagerly, initial)
    }

    // Float PID → StateFlow<Float> 맵
    private val floatFlows: Map<PIDs, StateFlow<Float>> = initialFloatValues.mapValues { (pid, initial) ->
        liveFrames
            .filter { it.startsWith("41${pid.code}") }
            .map { frame -> Decoders.parsers.getValue(pid)(frame) as Float }
            .stateIn(scope, SharingStarted.Eagerly, initial)
    }

    // 편의 프로퍼티
    val rpm: StateFlow<Int>       get() = intFlows.getValue(PIDs.RPM)
    val speed: StateFlow<Int>     get() = intFlows.getValue(PIDs.SPEED)
    val ect: StateFlow<Int>       get() = intFlows.getValue(PIDs.ECT)
    val throttle: StateFlow<Int>  get() = intFlows.getValue(PIDs.THROTTLE)
    val load: StateFlow<Int>      get() = intFlows.getValue(PIDs.LOAD)
    val iat: StateFlow<Int>       get() = intFlows.getValue(PIDs.IAT)

    val maf: StateFlow<Float>     get() = floatFlows.getValue(PIDs.MAF)
    val batt: StateFlow<Float>    get() = floatFlows.getValue(PIDs.BATT)
    val fuelRate: StateFlow<Float>get() = floatFlows.getValue(PIDs.FUEL_RATE)

    val currentGear: StateFlow<Int>      get() = intFlows.getValue(PIDs.CURRENT_GEAR)
    val oilPressure: StateFlow<Float>    get() = floatFlows.getValue(PIDs.OIL_PRESSURE)
    val oilTemp: StateFlow<Int>         get() = intFlows.getValue(PIDs.OIL_TEMP)
    val transFluidTemp: StateFlow<Int>   get() = intFlows.getValue(PIDs.TRANS_FLUID_TEMP)

    private val pidRefCnt: MutableMap<PIDs, AtomicInteger> =
        PIDs.entries.associateWith { AtomicInteger(0) }.toMutableMap()

    private val activeDefaultPids: MutableSet<PIDs> = mutableSetOf()

    /*private fun handleFrame(f: String) {
        if (f.length < 6) return
        val code = f.substring(2, 4)
        PIDs.fromCode(code)?.let { pid ->
            Decoders.parsers[pid]?.invoke(f)
                ?: Log.d(TAG, "[handleFrame] No parser for PID=${code}, frame=$f")
        } ?: Log.d(TAG, "[handleFrame] Unknown PID=$code, frame=$f")
    }*/

    fun registerPid(pid: PIDs) {
        val cnt = pidRefCnt.getValue(pid).incrementAndGet()
        if (cnt == 1) activeDefaultPids += pid        // 첫 등록 → 폴링 대상에 추가
    }

    fun unregisterPid(pid: PIDs) {
        val cnt = pidRefCnt.getValue(pid).decrementAndGet()
        if (cnt == 0) activeDefaultPids -= pid        // 마지막 구독 해제 → 제거
        check(cnt >= 0) { "unregisterPid() called too many times for $pid" }
    }

    fun pallStart(periodMs: Long = 500L) {
        Log.d(TAG, "[start] periodMs=$periodMs")

        if (pollJob?.isActive == true) {
            Log.d(TAG, "[start] already active, skipping")
            return
        }

        pollJob = scope.launch {
            Log.d(TAG, "[poll] loop started")
            while (isActive) {
                activeDefaultPids.forEach { pid ->
                    Log.d(TAG, "[poll] querying pid=${pid.name} : ${pid.code} / ${pid.header}")
                    try {
                        sppClient.query(cmd = "01${pid.code}", header = pid.header, timeoutMs = 1500)
                    } catch (e: Exception) {
                        Log.w(TAG, "[poll] Exception pid=${pid.code}: ${e.localizedMessage}")
                    }
                }
                delay(periodMs)
            }
        }
    }

    suspend fun startQuery(pid : PIDs): String?{
        try {
            val resp = sppClient.query(pid.code, header = pid.header, timeoutMs = 1500)
            Log.d(TAG, "[poll] resp=${resp}")
            return Decoders.parsers[pid]?.invoke(resp).toString()
        } catch (e: Exception) {
            Log.w(TAG, "[poll] NO DATA pid=${pid.code}: ${e.localizedMessage}")
            return null
        }
    }

    fun stop() {
        collectJob?.cancel()
    }
}