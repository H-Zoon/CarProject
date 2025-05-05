package com.devidea.chevy.k10s.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.k10s.obd.model.CarEventModel
import com.devidea.chevy.k10s.obd.protocol.pid.PIDListData
import com.devidea.chevy.k10s.eventbus.CarEvents
import com.devidea.chevy.k10s.eventbus.CarEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarViewModel @Inject constructor(private val repository: CarEventModel) : ViewModel() {

    init {
        viewModelScope.launch {
            CarEventBus.events.collect { viewEvent ->
                when (viewEvent) {
                    is CarEvents.updateObdData -> {
                        _obdData.value = viewEvent.value
                    }

                    is CarEvents.updateLeftFront -> {
                        _leftFront.value = viewEvent.value
                    }

                    is CarEvents.updateRightFront -> {
                        _rightFront.value = viewEvent.value
                    }

                    is CarEvents.updateLeftRear -> {
                        _leftRear.value = viewEvent.value
                    }

                    is CarEvents.updateRightRear -> {
                        _rightRear.value = viewEvent.value
                    }

                    is CarEvents.updateTrunk -> {
                        _trunk.value = viewEvent.value
                    }

                    is CarEvents.updateRotate -> {
                        _mRotate.value = viewEvent.value
                    }

                    is CarEvents.updateRemainGas -> {
                        _mRemainGas.value = viewEvent.value
                    }

                    is CarEvents.updateMileage -> {
                        _mMileage.value = viewEvent.value
                    }

                    is CarEvents.updateHandBrake -> {
                        _mHandBrake.value = viewEvent.value
                    }

                    is CarEvents.updateSeatbelt -> {
                        _mSeatbelt.value = viewEvent.value
                    }

                    is CarEvents.updateGear -> {
                        _mGear.value = viewEvent.value
                    }

                    is CarEvents.updateGearNum -> {
                        _mGearNum.value = viewEvent.value
                    }

                    is CarEvents.updateErrCount -> {
                        _mErrCount.value = viewEvent.value
                    }

                    is CarEvents.updateVoltage -> {
                        _mVoltage.value = viewEvent.value
                    }

                    is CarEvents.updateSolarTermDoor -> {
                        _mSolarTermDoor.value = viewEvent.value
                    }

                    is CarEvents.updateWaterTemperature -> {
                        _mWaterTemperature.value = viewEvent.value
                    }

                    is CarEvents.updateThreeCatalystTemperatureBankOne1 -> {
                        _mThreeCatalystTemperatureBankOne1.value = viewEvent.value
                    }

                    is CarEvents.updateThreeCatalystTemperatureBankOne2 -> {
                        _mThreeCatalystTemperatureBankOne2.value = viewEvent.value
                    }

                    is CarEvents.updateThreeCatalystTemperatureBankTwo1 -> {
                        _mThreeCatalystTemperatureBankTwo1.value = viewEvent.value
                    }

                    is CarEvents.updateThreeCatalystTemperatureBankTwo2 -> {
                        _mThreeCatalystTemperatureBankTwo2.value = viewEvent.value
                    }

                    is CarEvents.updateMeterVoltage -> {
                        _mMeterVoltage.value = viewEvent.value
                    }

                    is CarEvents.updateMeterRotate -> {
                        _mMeterRotate.value = viewEvent.value
                    }

                    is CarEvents.updateSpeed -> {
                        _mSpeed.value = viewEvent.value
                    }

                    is CarEvents.updateTemp -> {
                        _mTemp.value = viewEvent.value
                    }

                    is CarEvents.updateGearFunOnoffVisible -> {
                        _mGearFunOnoffVisible.value = viewEvent.value
                    }

                    is CarEvents.updateGearDLock -> {
                        _mGearDLock.value = viewEvent.value
                    }

                    is CarEvents.updateGearPUnlock -> {
                        _mGearPUnlock.value = viewEvent.value
                    }

                    is CarEvents.updateCarVIN -> {
                        _carVIN.value = viewEvent.value
                    }
                }
            }
        }
    }

    private val _obdData = MutableStateFlow<MutableList<PIDListData>>(ArrayList())
    val obdData: StateFlow<MutableList<PIDListData>> get() = _obdData

    private val _leftFront =
        MutableStateFlow<CarEventModel.DoorState>(CarEventModel.DoorState.Closed)
    val leftFront: StateFlow<CarEventModel.DoorState> get() = _leftFront.asStateFlow()

    private val _rightFront =
        MutableStateFlow<CarEventModel.DoorState>(CarEventModel.DoorState.Closed)
    val rightFront: StateFlow<CarEventModel.DoorState> get() = _rightFront.asStateFlow()

    private val _leftRear =
        MutableStateFlow<CarEventModel.DoorState>(CarEventModel.DoorState.Closed)
    val leftRear: StateFlow<CarEventModel.DoorState> get() = _leftRear.asStateFlow()

    private val _rightRear =
        MutableStateFlow<CarEventModel.DoorState>(CarEventModel.DoorState.Closed)
    val rightRear: StateFlow<CarEventModel.DoorState> get() = _rightRear.asStateFlow()

    private val _trunk = MutableStateFlow<CarEventModel.DoorState>(CarEventModel.DoorState.Closed)
    val trunk: StateFlow<CarEventModel.DoorState> get() = _trunk.asStateFlow()

    private val _mRotate = MutableStateFlow<Int>(0)
    val mRotate: StateFlow<Int> get() = _mRotate.asStateFlow()

    private val _mRemainGas = MutableStateFlow<String>("")
    val mRemainGas: StateFlow<String> get() = _mRemainGas.asStateFlow()

    private val _mMileage = MutableStateFlow<String>("")
    val mMileage: StateFlow<String> get() = _mMileage.asStateFlow()

    private val _mHandBrake = MutableStateFlow<Boolean>(false)
    val mHandBrake: StateFlow<Boolean> get() = _mHandBrake.asStateFlow()

    private val _mSeatbelt = MutableStateFlow<Boolean>(false)
    val mSeatbelt: StateFlow<Boolean> get() = _mSeatbelt.asStateFlow()

    private val _mGear = MutableStateFlow<String>("")
    val mGear: StateFlow<String> get() = _mGear.asStateFlow()

    private val _mGearNum = MutableStateFlow<Int>(0)
    val mGearNum: StateFlow<Int> get() = _mGearNum.asStateFlow()

    private val _mErrCount = MutableStateFlow<Int>(0)
    val mErrCount: StateFlow<Int> get() = _mErrCount.asStateFlow()

    private val _mVoltage = MutableStateFlow<String>("")
    val mVoltage: StateFlow<String> get() = _mVoltage.asStateFlow()

    private val _mSolarTermDoor = MutableStateFlow<String>("")
    val mSolarTermDoor: StateFlow<String> get() = _mSolarTermDoor.asStateFlow()

    private val _mWaterTemperature = MutableStateFlow<String>("")
    val mWaterTemperature: StateFlow<String> get() = _mWaterTemperature.asStateFlow()

    private val _mThreeCatalystTemperatureBankOne1 = MutableStateFlow<String>("")
    val mThreeCatalystTemperatureBankOne1: StateFlow<String> get() = _mThreeCatalystTemperatureBankOne1.asStateFlow()

    private val _mThreeCatalystTemperatureBankOne2 = MutableStateFlow<String>("")
    val mThreeCatalystTemperatureBankOne2: StateFlow<String> get() = _mThreeCatalystTemperatureBankOne2.asStateFlow()

    private val _mThreeCatalystTemperatureBankTwo1 = MutableStateFlow<String>("")
    val mThreeCatalystTemperatureBankTwo1: StateFlow<String> get() = _mThreeCatalystTemperatureBankTwo1.asStateFlow()

    private val _mThreeCatalystTemperatureBankTwo2 = MutableStateFlow<String>("")
    val mThreeCatalystTemperatureBankTwo2: StateFlow<String> get() = _mThreeCatalystTemperatureBankTwo2.asStateFlow()

    private val _mMeterVoltage = MutableStateFlow<String>("")
    val mMeterVoltage: StateFlow<String> get() = _mMeterVoltage.asStateFlow()

    private val _mMeterRotate = MutableStateFlow<Int>(0)
    val mMeterRotate: StateFlow<Int> get() = _mMeterRotate.asStateFlow()

    private val _mSpeed = MutableStateFlow<Int>(0)
    val mSpeed: StateFlow<Int> get() = _mSpeed.asStateFlow()

    private val _mTemp = MutableStateFlow<Int>(0)
    val mTemp: StateFlow<Int> get() = _mTemp.asStateFlow()

    private val _mGearFunOnoffVisible = MutableStateFlow<Int>(0)
    val mGearFunOnoffVisible: StateFlow<Int> get() = _mGearFunOnoffVisible.asStateFlow()

    private val _mGearDLock = MutableStateFlow<Int>(0)
    val mGearDLock: StateFlow<Int> get() = _mGearDLock.asStateFlow()

    private val _mGearPUnlock = MutableStateFlow<Int>(0)
    val mGearPUnlock: StateFlow<Int> get() = _mGearPUnlock.asStateFlow()

    private val _carVIN = MutableStateFlow<String>("")
    val carVIN: StateFlow<String> get() = _carVIN.asStateFlow()
}