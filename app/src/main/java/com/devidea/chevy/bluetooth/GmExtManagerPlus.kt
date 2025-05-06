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
import java.util.Collections
import kotlin.math.roundToInt

enum class GmPidPlus(val code: String) {
    OIL_LIFE("221030"),       // A/2.55 → %
    TRANS_TEMP("22F40C"),     // (AB)-40°C
    OIL_TEMP("22F415"),       // B-40°C
    BATT_VOLTAGE("22F42A"),   // AB/1000 V
    FUEL_USED("22F483"),      // AB mL
    CHARGE_CURRENT("22F44F"), // signed AB/10 A
    GEAR_POS("22F40F"),       // B
    OUTSIDE_TEMP("22F45A"),   // B-40°C
    TRANS_PRESSURE("22F40E"); // AB kPa

    companion object {
        private val map = GmPidPlus.entries.associateBy(GmPidPlus::code)
        fun fromRequest(req: String): GmPidPlus? = map[req]
    }
}

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

class GmExtManagerPlus @Inject constructor(private val sppClient: SppClient) {

    companion object {
        private const val TAG = "GmExtManagerPlus"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _snapshot = MutableStateFlow(GmSnapshotPlus())
    val snapshot: StateFlow<GmSnapshotPlus> = _snapshot.asStateFlow()

    private var pollJob: Job? = null

    // 기본 활성화된 PID 세트
    private val activePids: MutableSet<GmPidPlus> = Collections.synchronizedSet(
        mutableSetOf<GmPidPlus>().apply { addAll(GmPidPlus.entries.toTypedArray()) }
    )

    // PID별 디코더 매핑
    private val decoders: Map<GmPidPlus, (String) -> Unit> = mapOf(
        GmPidPlus.OIL_LIFE to ::decodeOilLife,
        GmPidPlus.TRANS_TEMP to ::decodeTransTemp,
        GmPidPlus.OIL_TEMP to ::decodeOilTemp,
        GmPidPlus.BATT_VOLTAGE to ::decodeBattVoltage,
        GmPidPlus.FUEL_USED to ::decodeFuelUsed,
        GmPidPlus.CHARGE_CURRENT to ::decodeChargeCurrent,
        GmPidPlus.GEAR_POS to ::decodeGearPos,
        GmPidPlus.OUTSIDE_TEMP to ::decodeOutsideTemp,
        GmPidPlus.TRANS_PRESSURE to ::decodeTransPressure
    )

    /** 활성 PID 추가 */
    fun enablePid(pid: GmPidPlus) {
        if (activePids.add(pid)) Log.d(TAG, "[enablePid] enabled=${pid.code}")
        else Log.d(TAG, "[enablePid] already active=${pid.code}")
    }

    /** 활성 PID 제거 */
    fun disablePid(pid: GmPidPlus) {
        if (activePids.remove(pid)) Log.d(TAG, "[disablePid] disabled=${pid.code}")
        else Log.d(TAG, "[disablePid] not active=${pid.code}")
    }

    /** 폴링 시작 */
    fun start(periodMs: Long = 3000L) {
        Log.d(TAG, "[start] periodMs=$periodMs")
        if (pollJob?.isActive == true) {
            Log.d(TAG, "[start] already active, skipping")
            return
        }

        pollJob = scope.launch {
            while (isActive) {
                activePids.forEach { pid ->
                    Log.d(TAG, "[poll] querying pid=${pid.code}")
                    try {
                        //TODO 1회성 되도록 설계 수정
                        val resp = sppClient.query(pid.code, header = "7E0", timeoutMs = 1500)
                        Log.d(TAG, "[poll] resp=${resp}")
                        decoders[pid]?.invoke(resp)
                    } catch (e: Exception) {
                        Log.w(TAG, "[poll] NO DATA pid=${pid.code}: ${e.localizedMessage}")
                    }
                }
            }
            delay(periodMs)
        }
    }

    fun stop() {
        Log.d(TAG, "[stop] cancelling")
        pollJob?.cancel()
        pollJob = null
    }

    private fun decodeOilLife(f: String) {
        Log.d(TAG, "[decodeOilLife] raw=$f")
        if (f.startsWith("62F459") && f.length >= 8) {
            val raw = f.substring(6, 8).toInt(16)
            val pct = (raw / 2.55f).roundToInt()
            Log.d(TAG, "[decodeOilLife] oilLife=${pct}%")
            _snapshot.update { it.copy(oilLife = pct) }
        }
    }

    private fun decodeTransTemp(f: String) {
        Log.d(TAG, "[decodeTransTemp] raw=$f")
        if (f.startsWith("62F40C") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val temp = (a shl 8 or b) - 40
            Log.d(TAG, "[decodeTransTemp] transTemp=${temp}°C")
            _snapshot.update { it.copy(transTemp = temp) }
        }
    }

    private fun decodeOilTemp(f: String) {
        Log.d(TAG, "[decodeOilTemp] raw=$f")
        if (f.startsWith("62F415") && f.length >= 10) {
            val value = f.substring(8, 10).toInt(16) - 40
            Log.d(TAG, "[decodeOilTemp] oilTemp=${value}°C")
            _snapshot.update { it.copy(oilTemp = value) }
        }
    }

    private fun decodeBattVoltage(f: String) {
        Log.d(TAG, "[decodeBattVoltage] raw=$f")
        if (f.startsWith("62F42A") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val volts = (a shl 8 or b) / 1000f
            Log.d(TAG, "[decodeBattVoltage] batt12v=${volts}V")
            _snapshot.update { it.copy(batt12v = volts) }
        }
    }

    private fun decodeFuelUsed(f: String) {
        Log.d(TAG, "[decodeFuelUsed] raw=$f")
        if (f.startsWith("62F483") && f.length >= 10) {
            val ml = (f.substring(6, 8).toInt(16) shl 8) + f.substring(8, 10).toInt(16)
            Log.d(TAG, "[decodeFuelUsed] fuelUsedMl=${ml}mL")
            _snapshot.update { it.copy(fuelUsedMl = ml) }
        }
    }

    private fun decodeChargeCurrent(f: String) {
        Log.d(TAG, "[decodeChargeCurrent] raw=$f")
        if (f.startsWith("62F44F") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val amps = (a shl 8 or b).toShort() / 10f
            Log.d(TAG, "[decodeChargeCurrent] chargeCurrent=${amps}A")
            _snapshot.update { it.copy(chargeCurrent = amps) }
        }
    }

    private fun decodeGearPos(f: String) {
        Log.d(TAG, "[decodeGearPos] raw=$f")
        if (f.startsWith("62F40F") && f.length >= 8) {
            val pos = f.substring(6, 8).toInt(16)
            Log.d(TAG, "[decodeGearPos] gearPos=${pos}")
            _snapshot.update { it.copy(gearPos = pos) }
        }
    }

    private fun decodeOutsideTemp(f: String) {
        Log.d(TAG, "[decodeOutsideTemp] raw=$f")
        if (f.startsWith("62F45A") && f.length >= 8) {
            val value = f.substring(6, 8).toInt(16) - 40
            Log.d(TAG, "[decodeOutsideTemp] outsideTemp=${value}°C")
            _snapshot.update { it.copy(outsideTemp = value) }
        }
    }

    private fun decodeTransPressure(f: String) {
        Log.d(TAG, "[decodeTransPressure] raw=$f")
        if (f.startsWith("62F40E") && f.length >= 10) {
            val a = f.substring(6, 8).toInt(16)
            val b = f.substring(8, 10).toInt(16)
            val pressure = a shl 8 or b
            Log.d(TAG, "[decodeTransPressure] transPressure=${pressure}kPa")
            _snapshot.update { it.copy(transPressure = pressure) }
        }
    }
}

