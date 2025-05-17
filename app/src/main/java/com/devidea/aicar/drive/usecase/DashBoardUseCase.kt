package com.devidea.aicar.drive.usecase

import android.util.Log
import com.devidea.aicar.drive.Decoders
import com.devidea.aicar.drive.PIDs
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.datastore.DataStoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class DashBoardUseCase @Inject constructor(
    private val sppClient: SppClient,
    private val dataStoreRepository: DataStoreRepository
) {

    companion object {
        private const val TAG = "PIDManager"

        /** gaugeId(String) → PIDs?  변환 함수 */
        private fun gaugeIdToPid(id: String): PIDs? =
            PIDs.entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
                ?: PIDs.Companion.fromCode(id)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pollJob: Job? = null
    private var pollPeriodMs = 500L
    private var gaugeJob: Job?    = null   // ① 게이지 목록 감시
    private var pollCtlJob: Job?  = null   // ② restartPollLoop 트리거


    // — 백잉 프로퍼티 선언 —
    private val _sFuelTrim      = MutableStateFlow(0f)
    private val _lFuelTrim      = MutableStateFlow(0f)
    private val _barometric     = MutableStateFlow(0f)
    private val _ect            = MutableStateFlow(0)
    private val _enginLoad      = MutableStateFlow(0)
    private val _rpm            = MutableStateFlow(0)
    private val _speed          = MutableStateFlow(0)
    private val _intakeTemp     = MutableStateFlow(0)
    private val _maf            = MutableStateFlow(0f)
    private val _throttle       = MutableStateFlow(0f)
    private val _batt           = MutableStateFlow(0f)
    private val _fuelRate       = MutableStateFlow(0f)
    private val _ambientAirTemp = MutableStateFlow(0)
    private val _catalystTemp   = MutableStateFlow(0)
    private val _equivalence    = MutableStateFlow(0f)
    private val _fuelLevel      = MutableStateFlow(0f)
    private val _intakePressure = MutableStateFlow(0f)
    private val _currentGear    = MutableStateFlow(0)
    private val _oilPressure    = MutableStateFlow(0f)
    private val _oilTemp        = MutableStateFlow(0)
    private val _transFluidTemp = MutableStateFlow(0)

    // — 외부 노출용 StateFlow —
    val sFuelTrim: StateFlow<Float>      get() = _sFuelTrim.asStateFlow()
    val lFuelTrim: StateFlow<Float>      get() = _lFuelTrim.asStateFlow()
    val barometric: StateFlow<Float>     get() = _barometric.asStateFlow()
    val ect: StateFlow<Int>              get() = _ect.asStateFlow()
    val enginLoad: StateFlow<Int>        get() = _enginLoad.asStateFlow()
    val rpm: StateFlow<Int>              get() = _rpm.asStateFlow()
    val speed: StateFlow<Int>            get() = _speed.asStateFlow()
    val intakeTemp: StateFlow<Int>       get() = _intakeTemp.asStateFlow()
    val maf: StateFlow<Float>            get() = _maf.asStateFlow()
    val throttle: StateFlow<Float>       get() = _throttle.asStateFlow()
    val batt: StateFlow<Float>           get() = _batt.asStateFlow()
    val fuelRate: StateFlow<Float>       get() = _fuelRate.asStateFlow()
    val ambientAirTemp: StateFlow<Int>   get() = _ambientAirTemp.asStateFlow()
    val catalystTemp: StateFlow<Int>     get() = _catalystTemp.asStateFlow()
    val commandedEquivalence: StateFlow<Float> get() = _equivalence.asStateFlow()
    val fuelLevel: StateFlow<Float>      get() = _fuelLevel.asStateFlow()
    val intakePressure: StateFlow<Float> get() = _intakePressure.asStateFlow()
    val currentGear: StateFlow<Int>      get() = _currentGear.asStateFlow()
    val oilPressure: StateFlow<Float>    get() = _oilPressure.asStateFlow()
    val oilTemp: StateFlow<Int>          get() = _oilTemp.asStateFlow()
    val transFluidTemp: StateFlow<Int>   get() = _transFluidTemp.asStateFlow()

    // — PID ↔ MutableStateFlow 매핑 —
    @Suppress("UNCHECKED_CAST")
    private val pidToFlow: Map<PIDs, MutableStateFlow<out Number>> = mapOf(
        PIDs.S_FUEL_TRIM               to _sFuelTrim,
        PIDs.L_FUEL_TRIM               to _lFuelTrim,
        PIDs.BAROMETRIC                to _barometric,
        PIDs.ECT                       to _ect,
        PIDs.ENGIN_LOAD                to _enginLoad,
        PIDs.RPM                       to _rpm,
        PIDs.SPEED                     to _speed,
        PIDs.INTAKE_TEMP               to _intakeTemp,
        PIDs.MAF                       to _maf,
        PIDs.THROTTLE                  to _throttle,
        PIDs.BATT                      to _batt,
        PIDs.FUEL_RATE                 to _fuelRate,
        PIDs.AMBIENT_AIR_TEMP          to _ambientAirTemp,
        PIDs.CATALYST_TEMP_BANK1       to _catalystTemp,
        PIDs.COMMANDED_EQUIVALENCE_RATIO to _equivalence,
        PIDs.FUEL_LEVEL                to _fuelLevel,
        PIDs.INTAKE_PRESSURE           to _intakePressure,
        PIDs.CURRENT_GEAR              to _currentGear,
        PIDs.OIL_PRESSURE              to _oilPressure,
        PIDs.OIL_TEMP                  to _oilTemp,
        PIDs.TRANS_FLUID_TEMP          to _transFluidTemp
    )

    private val activePids = MutableStateFlow<Set<PIDs>>(emptySet())

    private fun restartPollLoop(pidSet: Set<PIDs>) {
        pollJob?.cancel()
        if (pidSet.isEmpty()) return
        pollJob = scope.launch {
            while (isActive) {
                pidSet.forEach { pid ->
                    safeQuery(pid)?.let { value ->
                        (pidToFlow[pid] as? MutableStateFlow<Number>)?.value = value
                    }
                }
                delay(pollPeriodMs)
            }
        }
    }


    fun startPall() {
        if (gaugeJob?.isActive == true && pollCtlJob?.isActive == true) return

        gaugeJob = scope.launch {
            dataStoreRepository.selectedGaugeIds // Flow<List<String>>
                .map { it.mapNotNull(::gaugeIdToPid).toSet() }
                .distinctUntilChanged()
                .collect(activePids::value::set)
        }

        pollCtlJob = scope.launch {
            activePids
                .collect(::restartPollLoop)
        }

        // 초기 값으로 한 번 돌려서 즉시 폴링 시작
        restartPollLoop(activePids.value)
        Log.d(TAG, "[obs] observers started")
    }

    fun stopAll() {
        gaugeJob?.cancel()
        pollCtlJob?.cancel()
        pollJob?.cancel()
        gaugeJob = null
        pollCtlJob = null
        pollJob  = null
        Log.d(TAG, "[obs] observers stopped")
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