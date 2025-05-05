package com.devidea.chevy.bluetooth

import com.devidea.chevy.service.SppClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GearTpmsSnapshot(
    val gear: Int = 0,
    val tpms: Map<Int, TpmsWheel> = emptyMap()  // key: wheel index 0..3
)

data class TpmsWheel(val pressureKpa: Int, val tempC: Int, val battery: Int)

class RawCanManager @Inject constructor(private val spp: SppClient) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _snap = MutableStateFlow(GearTpmsSnapshot())
    val snapshot: StateFlow<GearTpmsSnapshot> = _snap.asStateFlow()

    private var monitorJob: Job? = null

    fun start() {
        if (monitorJob != null) return
        monitorJob = scope.launch {
            // 0. 기본 세팅
            spp.query("ATE0"); spp.query("ATL0"); spp.query("ATS0")
            spp.query("ATSH000")          // raw monitor
            spp.query("ATCRA000")         // 모든 ID 수신
            spp.query("ATMA")             // 시작

            // 1. 프레임 소비
            spp.liveFrames.collect { parseFrame(it) }
        }
    }

    fun stop() { monitorJob?.cancel(); monitorJob = null }

    private fun parseFrame(f: String) {
        // 형식 "192 03 00 04 00 00 00 00 00"
        val parts = f.trim().split(Regex("\\s+"))
        if (parts.size < 3) return

        val id = parts[0].toInt(16)
        val bytes = parts.drop(1).map { it.toInt(16) }

        when (id) {
            0x192 -> {                     // Gear
                val gear = bytes[2]
                _snap.update { it.copy(gear = gear) }
            }
            in 0x2B0..0x2B3 -> {           // TPMS wheels 0..3
                val idx = id - 0x2B0
                val pressure = ((bytes[1] shl 8) + bytes[2]) / 2   // kPa
                val temp = bytes[3] - 40
                val batt = bytes[4]
                val wheel = TpmsWheel(pressure, temp, batt)
                _snap.update { it.copy(tpms = it.tpms + (idx to wheel)) }
            }
        }
    }
}