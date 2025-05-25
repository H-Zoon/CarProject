package com.devidea.aicar.drive

import android.util.Log
import com.devidea.aicar.service.SppClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ObdPollingManager @Inject constructor(private val sppClient: SppClient) {

    companion object {
        private const val TAG = "PIDManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null
    private var pollPeriodMs = 1000L


    // — 백잉 프로퍼티 선언 —
    private val _sFuelTrim = MutableStateFlow(0f)
    private val _lFuelTrim = MutableStateFlow(0f)
    private val _ect = MutableStateFlow(0)
    private val _enginLoad = MutableStateFlow(0)
    private val _rpm = MutableStateFlow(0)
    private val _speed = MutableStateFlow(0)
    private val _intakeTemp = MutableStateFlow(0)
    private val _maf = MutableStateFlow(0f)
    private val _throttle = MutableStateFlow(0f)
    private val _batt = MutableStateFlow(0f)
    private val _ambientAirTemp = MutableStateFlow(0)
    private val _catalystTemp = MutableStateFlow(0)
    private val _equivalence = MutableStateFlow(0f)
    private val _fuelLevel = MutableStateFlow(0f)
    private val _intakePressure = MutableStateFlow(0f)
    private val _currentGear = MutableStateFlow(0)
    private val _oilPressure = MutableStateFlow(0f)
    private val _oilTemp = MutableStateFlow(0)
    private val _transFluidTemp = MutableStateFlow(0)

    //private val _barometric = MutableStateFlow(0f)

    // — 외부 노출용 StateFlow —
    val sFuelTrim: StateFlow<Float> get() = _sFuelTrim.asStateFlow()
    val lFuelTrim: StateFlow<Float> get() = _lFuelTrim.asStateFlow()
    val ect: StateFlow<Int> get() = _ect.asStateFlow()
    val enginLoad: StateFlow<Int> get() = _enginLoad.asStateFlow()
    val rpm: StateFlow<Int> get() = _rpm.asStateFlow()
    val speed: StateFlow<Int> get() = _speed.asStateFlow()
    val intakeTemp: StateFlow<Int> get() = _intakeTemp.asStateFlow()
    val maf: StateFlow<Float> get() = _maf.asStateFlow()
    val throttle: StateFlow<Float> get() = _throttle.asStateFlow()
    val batt: StateFlow<Float> get() = _batt.asStateFlow()
    val ambientAirTemp: StateFlow<Int> get() = _ambientAirTemp.asStateFlow()
    val catalystTemp: StateFlow<Int> get() = _catalystTemp.asStateFlow()
    val commandedEquivalence: StateFlow<Float> get() = _equivalence.asStateFlow()
    val fuelLevel: StateFlow<Float> get() = _fuelLevel.asStateFlow()
    val intakePressure: StateFlow<Float> get() = _intakePressure.asStateFlow()
    val currentGear: StateFlow<Int> get() = _currentGear.asStateFlow()
    val oilPressure: StateFlow<Float> get() = _oilPressure.asStateFlow()
    val oilTemp: StateFlow<Int> get() = _oilTemp.asStateFlow()
    val transFluidTemp: StateFlow<Int> get() = _transFluidTemp.asStateFlow()

    //val barometric: StateFlow<Float> get() = _barometric.asStateFlow()

    // — PID ↔ MutableStateFlow 매핑 —
    @Suppress("UNCHECKED_CAST")
    private val defaultPidFlow: Map<PIDs, MutableStateFlow<out Number>> = mapOf(
        PIDs.S_FUEL_TRIM to _sFuelTrim,
        PIDs.L_FUEL_TRIM to _lFuelTrim,
        PIDs.ECT to _ect,
        PIDs.ENGIN_LOAD to _enginLoad,
        PIDs.RPM to _rpm,
        PIDs.SPEED to _speed,
        PIDs.INTAKE_TEMP to _intakeTemp,
        PIDs.MAF to _maf,
        PIDs.THROTTLE to _throttle,
        PIDs.BATT to _batt,
        PIDs.AMBIENT_AIR_TEMP to _ambientAirTemp,
        PIDs.CATALYST_TEMP_BANK1 to _catalystTemp,
        PIDs.COMMANDED_EQUIVALENCE_RATIO to _equivalence,
        PIDs.INTAKE_PRESSURE to _intakePressure,
        PIDs.FUEL_LEVEL to _fuelLevel,
    )
    private val extendPidFlow: Map<PIDs, MutableStateFlow<out Number>> = mapOf(
        PIDs.OIL_PRESSURE to _oilPressure,
        PIDs.OIL_TEMP to _oilTemp,
        PIDs.TRANS_FLUID_TEMP to _transFluidTemp,
        PIDs.CURRENT_GEAR to _currentGear
    )


    private fun startPolling() {
        // 이미 폴링 중이면 무시
        if (pollJob?.isActive == true) {
            Log.d(TAG, "[poll] already polling, ignore startPolling()")
            return
        }

        val chunks = defaultPidFlow.keys.chunked(MultiPidUtils.MAX_PIDS)
        val extChunks = extendPidFlow.keys.chunked(ExtendedPidUtils.MAX_PIDS)

        pollJob = scope.launch {
            while (isActive) {
                for (chunk in chunks) {
                    try {
                        // Multi-PID 명령어 생성
                        val command = MultiPidUtils.buildCommand(chunk)
                        // 헤더는 같은 그룹 내 첫 PID의 header 사용
                        val header = chunk.first().header
                        // 요청 및 응답 수신
                        val response = sppClient.query(command, header = header, timeoutMs = pollPeriodMs)
                        // 응답 파싱
                        val values = MultiPidUtils.parseResponse(response)
                        // Flow 업데이트
                        values.forEach { (pid, value) ->
                            (defaultPidFlow[pid] as? MutableStateFlow<Number>)?.value = value
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[poll] Multi-PID chunk $chunk error: ${e.message}")
                    }
                }

                for (chunk in extChunks) {
                    try {
                        val cmd = ExtendedPidUtils.buildCommand(chunk)
                        val header = chunk.first().header
                        val resp =
                            sppClient.query(cmd, header = header, timeoutMs = pollPeriodMs)
                        val vals = ExtendedPidUtils.parseResponse(resp)
                        vals.forEach { (pid, v) ->
                            // exFlow 맵에서 바로 꺼내서 업데이트
                            (extendPidFlow[pid] as MutableStateFlow<Number>).value = v
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[poll] ext chunk $chunk err", e)
                    }
                }

                // 전체 사이클 후 딜레이
                delay(pollPeriodMs)

            }
        }
    }


    fun startPall() {
        startPolling()
        Log.d(TAG, "[obs] observers started")
    }

    fun stopAll() {
        pollJob?.cancel()
        pollJob = null
        Log.d(TAG, "[obs] observers stopped")
    }
}