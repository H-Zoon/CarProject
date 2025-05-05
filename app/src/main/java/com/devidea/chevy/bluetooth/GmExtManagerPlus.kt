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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlin.math.roundToInt


/**
 * GmSnapshotPlus – 추가 GM 전용 항목 포함.
 */
data class GmSnapshotPlus(
    val oilLife: Int = 0,
    val transTemp: Int = 0,
    val oilTemp: Int = 0,
    val batt12v: Float = 0f,
    val fuelUsedMl: Int = 0,
    val chargeCurrent: Float = 0f,
    val gearPos: Int = 0,
    val outsideTemp: Int = 0,
    val transPressure: Int = 0
)

/**
 * GmExtManagerPlus – GM Mode 22 확장 PID 다수 수집.
 * 헤더 7E0 고정, PID 순차 쿼리.
 */
class GmExtManagerPlus @Inject constructor(private val sppClient: SppClient) {

    companion object {
        private const val TAG = "GmExtManagerPlus"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _snapshot = MutableStateFlow(GmSnapshotPlus())
    val snapshot: StateFlow<GmSnapshotPlus> = _snapshot.asStateFlow()

    private var pollJob: Job? = null

    // 요청 → 디코더 매핑
    private val decoders: Map<String, (String) -> Unit> = mapOf(
        "22F459" to ::decodeOilLife,
        /*"22F40C" to ::decodeTransTemp,
        "22F415" to ::decodeOilTemp,
        "22F42A" to ::decodeBattVoltage,
        "22F483" to ::decodeFuelUsed,
        "22F44F" to ::decodeChargeCurrent,
        "22F40F" to ::decodeGearPos,
        "22F45A" to ::decodeOutsideTemp,
        "22F40E" to ::decodeTransPressure*/
    )

    fun start(periodMs: Long = 3000L) {
        Log.d(TAG, "[start] called with periodMs=$periodMs")
        if (pollJob?.isActive == true) {
            Log.d(TAG, "[start] already active, skipping")
            return
        }
        pollJob = scope.launch {
            Log.d(TAG, "[poll] polling loop started")
            while (isActive) {
                for ((pid, decoder) in decoders) {
                    Log.d(TAG, "[poll] querying PID=$pid")
                    try {
                        val resp = sppClient.query(pid, header = null, timeoutMs = 1500)
                        Log.d(TAG, "[poll] received response for $pid: $resp")
                        decoder(resp)
                    } catch (e: Exception) {
                        Log.d(TAG, "[poll] NO DATA for PID=$pid: ${e.localizedMessage}")
                    }
                }
                delay(periodMs)
            }
        }
    }

    fun stop() {
        Log.d(TAG, "[stop] called - cancelling polling")
        pollJob?.cancel()
        pollJob = null
    }

    // ——— Decoders ———
    private fun decodeOilLife(f: String) {
        Log.d(TAG, "[decodeOilLife] raw=$f")
        // 62 11 9F A  ⇒ A는 0..255, 실제 값 = A/2.55 => 0..100%
        if (f.startsWith("62119F") && f.length >= 8) {
            val raw = f.substring(6, 8).toInt(16)
            val oilPct = (raw / 2.55f).roundToInt()
            Log.d(TAG, "[decodeOilLife] rawByte=$raw, oilLife=$oilPct%")
            _snapshot.update { it.copy(oilLife = oilPct) }
        }
    }

    private fun decodeTransTemp(f: String) {
        Log.d(TAG, "[decodeTransTemp] raw=$f")
        if (f.startsWith("62F40C") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val temp = (a shl 8 or b) - 40
            Log.d(TAG, "[decodeTransTemp] transTemp=$temp°C")
            _snapshot.update { it.copy(transTemp = temp) }
        }
    }

    private fun decodeOilTemp(f: String) {
        Log.d(TAG, "[decodeOilTemp] raw=$f")
        if (f.startsWith("62F415") && f.length >= 10) {
            val value = f.substring(8, 10).toInt(16) - 40
            Log.d(TAG, "[decodeOilTemp] oilTemp=$value°C")
            _snapshot.update { it.copy(oilTemp = value) }
        }
    }

    private fun decodeBattVoltage(f: String) {
        Log.d(TAG, "[decodeBattVoltage] raw=$f")
        if (f.startsWith("62F42A") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val volts = (a shl 8 or b) / 1000f
            Log.d(TAG, "[decodeBattVoltage] batt12v=$volts V")
            _snapshot.update { it.copy(batt12v = volts) }
        }
    }

    private fun decodeFuelUsed(f: String) {
        Log.d(TAG, "[decodeFuelUsed] raw=$f")
        if (f.startsWith("62F483") && f.length >= 10) {
            val ml = (f.substring(6, 8).toInt(16) shl 8) + f.substring(8, 10).toInt(16)
            Log.d(TAG, "[decodeFuelUsed] fuelUsedMl=$ml mL")
            _snapshot.update { it.copy(fuelUsedMl = ml) }
        }
    }

    private fun decodeChargeCurrent(f: String) {
        Log.d(TAG, "[decodeChargeCurrent] raw=$f")
        if (f.startsWith("62F44F") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val amps = (a shl 8 or b).toShort() / 10f
            Log.d(TAG, "[decodeChargeCurrent] chargeCurrent=$amps A")
            _snapshot.update { it.copy(chargeCurrent = amps) }
        }
    }

    private fun decodeGearPos(f: String) {
        Log.d(TAG, "[decodeGearPos] raw=$f")
        if (f.startsWith("62F40F") && f.length >= 8) {
            val pos = f.substring(6, 8).toInt(16)
            Log.d(TAG, "[decodeGearPos] gearPos=$pos")
            _snapshot.update { it.copy(gearPos = pos) }
        }
    }

    private fun decodeOutsideTemp(f: String) {
        Log.d(TAG, "[decodeOutsideTemp] raw=$f")
        if (f.startsWith("62F45A") && f.length >= 8) {
            val value = f.substring(6, 8).toInt(16) - 40
            Log.d(TAG, "[decodeOutsideTemp] outsideTemp=$value°C")
            _snapshot.update { it.copy(outsideTemp = value) }
        }
    }

    private fun decodeTransPressure(f: String) {
        Log.d(TAG, "[decodeTransPressure] raw=$f")
        if (f.startsWith("62F40E") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val pressure = a shl 8 or b
            Log.d(TAG, "[decodeTransPressure] transPressure=$pressure kPa")
            _snapshot.update { it.copy(transPressure = pressure) }
        }
    }
}
