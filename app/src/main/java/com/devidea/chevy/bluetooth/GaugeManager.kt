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
import kotlinx.coroutines.flow.*

data class GaugeSnapshot(
    val rpm: Int = 0,
    val speed: Int = 0,
    val ect: Int = 0,
    val throttle: Int = 0,
    val load: Int = 0,
    val iat: Int = 0,
    val maf: Float = 0f,
    val batt: Float = 0f,
    val fuelRate: Float = 0f
)

class GaugeManager @Inject constructor(private val sppClient: SppClient) {

    companion object {
        private const val TAG = "GaugeManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _snap = MutableStateFlow(GaugeSnapshot())
    val snapshot: StateFlow<GaugeSnapshot> = _snap.asStateFlow()

    private var pollJob: Job? = null
    private var collectJob: Job? = null

    private val pids = listOf("0C", "0D", "05", "11", "04", "0F", "10", "42", "5E")

    fun start(periodMs: Long = 500L) {
        Log.d(TAG, "[start] called with periodMs=$periodMs")
        if (pollJob?.isActive == true) {
            Log.d(TAG, "[start] already active, skipping")
            return
        }

        collectJob = scope.launch {
            Log.d(TAG, "[collect] subscribing to liveFrames")
            sppClient.liveFrames
                .filter { it.startsWith("41") }
                .onEach { f -> Log.d(TAG, "[collect] frame received: $f") }
                .collect { f -> parse(f) }
        }

        pollJob = scope.launch {
            Log.d(TAG, "[poll] started polling loop")
            while (isActive) {
                Log.d(TAG, "[poll] iteration start")
                pids.forEach { pid ->
                    Log.d(TAG, "[poll] querying PID=$pid")
                    try {
                        sppClient.query(cmd = "01$pid", header = "7E0", timeoutMs = 1500)
                        Log.d(TAG, "[poll] query for PID=$pid sent")
                    } catch (e: Exception) {
                        Log.d(TAG, "[poll] NO DATA for PID=$pid (exception: ${e.localizedMessage})")
                    }
                }
                delay(periodMs)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "[stop] called")
        pollJob?.cancel()
        collectJob?.cancel()
    }

    private fun parse(f: String) {
        Log.d(TAG, "[parse] processing frame: $f")
        when {
            f.startsWith("410C") && f.length >= 8 -> {
                val a = f.substring(4,6).toInt(16)
                val b = f.substring(6,8).toInt(16)
                val rpm = (a shl 8 or b) / 4
                Log.d(TAG, "[parse] RPM raw=(${a shl 8 or b}), rpm=$rpm")
                _snap.update { it.copy(rpm = rpm) }
            }
            f.startsWith("410D") && f.length >= 6 -> {
                val speed = f.substring(4,6).toInt(16)
                Log.d(TAG, "[parse] Speed=$speed km/h")
                _snap.update { it.copy(speed = speed) }
            }
            f.startsWith("4105") && f.length >= 6 -> {
                val raw = f.substring(4,6).toInt(16)
                val temp = raw - 40
                Log.d(TAG, "[parse] ECT raw=$raw, ect=$temp°C")
                _snap.update { it.copy(ect = temp) }
            }
            f.startsWith("4111") && f.length >= 6 -> {
                val raw = f.substring(4,6).toInt(16)
                val throttle = raw * 100 / 255
                Log.d(TAG, "[parse] Throttle raw=$raw, throttle=$throttle%")
                _snap.update { it.copy(throttle = throttle) }
            }
            f.startsWith("4104") && f.length >= 6 -> {
                val raw = f.substring(4,6).toInt(16)
                val load = raw * 100 / 255
                Log.d(TAG, "[parse] Load raw=$raw, load=$load%")
                _snap.update { it.copy(load = load) }
            }
            f.startsWith("410F") && f.length >= 6 -> {
                val raw = f.substring(4,6).toInt(16)
                val iat = raw - 40
                Log.d(TAG, "[parse] IAT raw=$raw, iat=$iat°C")
                _snap.update { it.copy(iat = iat) }
            }
            f.startsWith("4110") && f.length >= 8 -> {
                val a = f.substring(4,6).toInt(16)
                val b = f.substring(6,8).toInt(16)
                val maf = (a shl 8 or b) / 100f
                Log.d(TAG, "[parse] MAF raw=(${a shl 8 or b}), maf=$maf g/s")
                _snap.update { it.copy(maf = maf) }
            }
            f.startsWith("4142") && f.length >= 8 -> {
                val a = f.substring(4,6).toInt(16)
                val b = f.substring(6,8).toInt(16)
                val batt = (a shl 8 or b) / 1000f
                Log.d(TAG, "[parse] Batt raw=(${a shl 8 or b}), batt=$batt V")
                _snap.update { it.copy(batt = batt) }
            }
            f.startsWith("415E") && f.length >= 8 -> {
                val a = f.substring(4,6).toInt(16)
                val b = f.substring(6,8).toInt(16)
                val fuelRate = (a shl 8 or b) / 20f
                Log.d(TAG, "[parse] FuelRate raw=(${a shl 8 or b}), fuelRate=$fuelRate L/h")
                _snap.update { it.copy(fuelRate = fuelRate) }
            }
            else -> Log.d(TAG, "[parse] Unhandled frame: $f")
        }
    }
}