package com.devidea.chevy.bluetooth

import com.devidea.chevy.service.SppClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

enum class DefaultPid(val code: String) {
    RPM("0C"),
    SPEED("0D"),
    ECT("05"),
    THROTTLE("11"),
    LOAD("04"),
    IAT("0F"),
    MAF("10"),
    BATT("42"),
    FUEL_RATE("5E");

    companion object {
        private val map = DefaultPid.entries.associateBy(DefaultPid::code)
        fun fromCode(code: String): DefaultPid? = map[code.uppercase()]
    }
}

/*data class GaugeSnapshot(
    val rpm: Int = 0,
    val speed: Int = 0,
    val ect: Int = 0,
    val throttle: Int = 0,
    val load: Int = 0,
    val iat: Int = 0,
    val maf: Float = 0f,
    val batt: Float = 0f,
    val fuelRate: Float = 0f
)*/

class GaugeManager @Inject constructor(private val sppClient: SppClient) {

    companion object {
        private const val TAG = "GaugeManager"
    }

    private val _rpm       = MutableStateFlow(0)     ; val rpm       = _rpm.asStateFlow()
    private val _speed     = MutableStateFlow(0)     ; val speed     = _speed.asStateFlow()
    private val _ect       = MutableStateFlow(0)     ; val ect       = _ect.asStateFlow()
    private val _throttle  = MutableStateFlow(0)     ; val throttle  = _throttle.asStateFlow()
    private val _load      = MutableStateFlow(0)     ; val load      = _load.asStateFlow()
    private val _iat       = MutableStateFlow(0)     ; val iat       = _iat.asStateFlow()
    private val _maf       = MutableStateFlow(0f)    ; val maf       = _maf.asStateFlow()
    private val _batt      = MutableStateFlow(0f)    ; val batt      = _batt.asStateFlow()
    private val _fuelRate  = MutableStateFlow(0f)    ; val fuelRate  = _fuelRate.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
   /* private val _snap = MutableStateFlow(GaugeSnapshot())
    val snapshot: StateFlow<GaugeSnapshot> = _snap.asStateFlow()*/

    private var pollJob: Job? = null
    private var collectJob: Job? = null

    private val pidRefCnt: MutableMap<DefaultPid, AtomicInteger> =
        DefaultPid.entries.associateWith { AtomicInteger(0) }.toMutableMap()

    /** *실제로 폴링 중*인 PID 집합 (내부용, 동기화 필요 없음: 단일 코루틴 접근) */
    private val activeDefaultPids: MutableSet<DefaultPid> = mutableSetOf()

    // PID별 파싱 함수를 담은 맵
    private val decoders: Map<DefaultPid, (String) -> Unit> = DefaultPid.entries.associateWith { pid ->
        when (pid) {
            DefaultPid.RPM -> ::parseRpm
            DefaultPid.SPEED -> ::parseSpeed
            DefaultPid.ECT -> ::parseEct
            DefaultPid.THROTTLE -> ::parseThrottle
            DefaultPid.LOAD -> ::parseLoad
            DefaultPid.IAT -> ::parseIat
            DefaultPid.MAF -> ::parseMaf
            DefaultPid.BATT -> ::parseBatt
            DefaultPid.FUEL_RATE -> ::parseFuelRate
        }
    }

    fun registerPid(pid: DefaultPid) {
        val cnt = pidRefCnt.getValue(pid).incrementAndGet()
        if (cnt == 1) activeDefaultPids += pid        // 첫 등록 → 폴링 대상에 추가
    }

    fun unregisterPid(pid: DefaultPid) {
        val cnt = pidRefCnt.getValue(pid).decrementAndGet()
        if (cnt == 0) activeDefaultPids -= pid        // 마지막 구독 해제 → 제거
        check(cnt >= 0) { "unregisterPid() called too many times for $pid" }
    }

    fun start(periodMs: Long = 500L) {
        Log.d(TAG, "[start] periodMs=$periodMs")

        if (pollJob?.isActive == true) {
            Log.d(TAG, "[start] already active, skipping")
            return
        }

        collectJob = scope.launch {
            sppClient.liveFrames
                .filter { it.startsWith("41") }
                .collect { frame ->
                    handleFrame(frame)
                }
        }

        pollJob = scope.launch {
            Log.d(TAG, "[poll] loop started")
            while (isActive) {
                activeDefaultPids.forEach{ pid ->
                    Log.d(TAG, "[poll] querying pid=${pid.name} : ${pid.code}")
                    try {
                        sppClient.query(cmd = "01${pid.code}", header = "7E0", timeoutMs = 1500)
                    } catch (e: Exception) {


                    }
                }
                delay(periodMs)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        collectJob?.cancel()
    }

    private fun handleFrame(f: String) {
        if (f.length < 6) return
        val code = f.substring(2, 4)
        DefaultPid.fromCode(code)?.let { pid ->
            decoders[pid]?.invoke(f)
                ?: Log.d(TAG, "[handleFrame] No parser for PID=${code}, frame=$f")
        } ?: Log.d(TAG, "[handleFrame] Unknown PID=$code, frame=$f")
    }

    private fun parseRpm(f: String) {
        Log.d(TAG, "[parseRpm] raw=$f")
        if (f.length < 8) return
        val a = f.substring(4, 6).toInt(16)
        val b = f.substring(6, 8).toInt(16)
        val rpmValue = (a shl 8 or b) / 4
        Log.d(TAG, "[parseRpm] rpm=$rpmValue")
        _rpm.value = rpmValue
    }

    private fun parseSpeed(f: String) {
        Log.d(TAG, "[parseSpeed] raw=$f")
        if (f.length < 6) return
        val raw = f.substring(4, 6).toInt(16)
        Log.d(TAG, "[parseSpeed] speed=${raw} km/h")
        _speed.value = raw
    }

    private fun parseEct(f: String) {
        Log.d(TAG, "[parseEct] raw=$f")
        if (f.length < 6) return
        val raw = f.substring(4, 6).toInt(16)
        val temp = raw - 40
        Log.d(TAG, "[parseEct] ect=${temp}°C")
        _ect.value = temp
    }

    private fun parseThrottle(f: String) {
        Log.d(TAG, "[parseThrottle] raw=$f")
        if (f.length < 6) return
        val raw = f.substring(4, 6).toInt(16)
        val pct = raw * 100 / 255
        Log.d(TAG, "[parseThrottle] throttle=${pct}%")
        _throttle.value = pct
    }

    private fun parseLoad(f: String) {
        Log.d(TAG, "[parseLoad] raw=$f")
        if (f.length < 6) return
        val raw = f.substring(4, 6).toInt(16)
        val pct = raw * 100 / 255
        Log.d(TAG, "[parseLoad] load=${pct}%")
        _load.value = pct
    }

    private fun parseIat(f: String) {
        Log.d(TAG, "[parseIat] raw=$f")
        if (f.length < 6) return
        val raw = f.substring(4, 6).toInt(16)
        val temp = raw - 40
        Log.d(TAG, "[parseIat] iat=${temp}°C")
        _iat.value = temp
    }

    private fun parseMaf(f: String) {
        Log.d(TAG, "[parseMaf] raw=$f")
        if (f.length < 8) return
        val a = f.substring(4, 6).toInt(16)
        val b = f.substring(6, 8).toInt(16)
        val mafValue = (a shl 8 or b) / 100f
        Log.d(TAG, "[parseMaf] maf=${mafValue} g/s")
        _maf.value = mafValue
    }

    private fun parseBatt(f: String) {
        Log.d(TAG, "[parseBatt] raw=$f")
        if (f.length < 8) return
        val a = f.substring(4, 6).toInt(16)
        val b = f.substring(6, 8).toInt(16)
        val battValue = (a shl 8 or b) / 1000f
        Log.d(TAG, "[parseBatt] batt=${battValue} V")
        _batt.value = battValue
    }

    private fun parseFuelRate(f: String) {
        Log.d(TAG, "[parseFuelRate] raw=$f")
        if (f.length < 8) return
        val a = f.substring(4, 6).toInt(16)
        val b = f.substring(6, 8).toInt(16)
        val fuelRateValue = (a shl 8 or b) / 20f
        Log.d(TAG, "[parseFuelRate] fuelRate=${fuelRateValue} L/h")
        _fuelRate.value  = fuelRateValue
    }
}