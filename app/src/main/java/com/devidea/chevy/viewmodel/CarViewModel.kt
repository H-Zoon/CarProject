package com.devidea.chevy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.carsystem.pid.PIDListData
import com.devidea.chevy.eventbus.ViewEvent
import com.devidea.chevy.eventbus.ViewEventBus
import com.devidea.chevy.repository.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarViewModel @Inject constructor(
    private val repository: DataStoreRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            ViewEventBus.events.collect { viewEvent ->
                when (viewEvent) {
                    is ViewEvent.updateObdData -> {
                        _obdData.value = viewEvent.value
                    }

                    is ViewEvent.updateLeftFront -> {
                        _leftFront.value = viewEvent.value
                    }

                    is ViewEvent.updateRightFront -> {
                        _rightFront.value = viewEvent.value
                    }

                    is ViewEvent.updateLeftRear -> {
                        _leftRear.value = viewEvent.value
                    }

                    is ViewEvent.updateRightRear -> {
                        _rightRear.value = viewEvent.value
                    }

                    is ViewEvent.updateTrunk -> {
                        _trunk.value = viewEvent.value
                    }

                    is ViewEvent.updateRotate -> {
                        _mRotate.value = viewEvent.value
                    }

                    is ViewEvent.updateRemainGas -> {
                        _mRemainGas.value = viewEvent.value
                    }

                    is ViewEvent.updateMileage -> {
                        _mMileage.value = viewEvent.value
                    }

                    is ViewEvent.updateHandBrake -> {
                        _mHandBrake.value = viewEvent.value
                    }

                    is ViewEvent.updateSeatbelt -> {
                        _mSeatbelt.value = viewEvent.value
                    }

                    is ViewEvent.updateGear -> {
                        _mGear.value = viewEvent.value
                    }

                    is ViewEvent.updateGearNum -> {
                        _mGearNum.value = viewEvent.value
                    }

                    is ViewEvent.updateErrCount -> {
                        _mErrCount.value = viewEvent.value
                    }

                    is ViewEvent.updateVoltage -> {
                        _mVoltage.value = viewEvent.value
                    }

                    is ViewEvent.updateSolarTermDoor -> {
                        _mSolarTermDoor.value = viewEvent.value
                    }

                    is ViewEvent.updateWaterTemperature -> {
                        _mWaterTemperature.value = viewEvent.value
                    }

                    is ViewEvent.updateThreeCatalystTemperatureBankOne1 -> {
                        _mThreeCatalystTemperatureBankOne1.value = viewEvent.value
                    }

                    is ViewEvent.updateThreeCatalystTemperatureBankOne2 -> {
                        _mThreeCatalystTemperatureBankOne2.value = viewEvent.value
                    }

                    is ViewEvent.updateThreeCatalystTemperatureBankTwo1 -> {
                        _mThreeCatalystTemperatureBankTwo1.value = viewEvent.value
                    }

                    is ViewEvent.updateThreeCatalystTemperatureBankTwo2 -> {
                        _mThreeCatalystTemperatureBankTwo2.value = viewEvent.value
                    }

                    is ViewEvent.updateMeterVoltage -> {
                        _mMeterVoltage.value = viewEvent.value
                    }

                    is ViewEvent.updateMeterRotate -> {
                        _mMeterRotate.value = viewEvent.value
                    }

                    is ViewEvent.updateSpeed -> {
                        _mSpeed.value = viewEvent.value
                    }

                    is ViewEvent.updateTemp -> {
                        _mTemp.value = viewEvent.value
                    }

                    is ViewEvent.updateGearFunOnoffVisible -> {
                        _mGearFunOnoffVisible.value = viewEvent.value
                    }

                    is ViewEvent.updateGearDLock -> {
                        _mGearDLock.value = viewEvent.value
                    }

                    is ViewEvent.updateGearPUnlock -> {
                        _mGearPUnlock.value = viewEvent.value
                    }

                    is ViewEvent.updateCarVIN -> {
                        _carVIN.value = viewEvent.value
                    }

                    is ViewEvent.BluetoothState -> {
                        _bluetoothState.value = viewEvent.value
                    }
                }
            }
        }
        viewModelScope.launch {
            launch {
                repository.getConnectDate().collect { difference ->
                    _lastConnectDate.value =
                        when(difference) {
                            (-1).toLong() -> "-"
                            (0).toLong() -> "방금전"
                            else -> "$difference 일 전"
                        }
                }
            }

            launch {
                repository.getMileageDate().collect { difference ->
                    _recentMileage.value = when(difference) {
                        -1 -> "-"
                        else -> "$difference Km"
                    }
                }
            }

            launch {
                repository.getFuelDate().collect { difference ->
                    _fullEfficiency.value = when(difference) {
                        (-1).toFloat() -> "-"
                        else -> "$difference Km/L"
                    }
                }
            }
        }
    }

    private val _lastConnectDate = MutableStateFlow<String>("")
    val lastConnectDate: StateFlow<String> get() = _lastConnectDate

    private val _recentMileage = MutableStateFlow<String>("")
    val recentMileage: StateFlow<String> get() = _recentMileage

    private val _fullEfficiency = MutableStateFlow<String>("")
    val fullEfficiency: StateFlow<String> get() = _fullEfficiency

    private val _bluetoothState = MutableStateFlow<BTState>(BTState.DISCONNECTED)
    val bluetoothState: StateFlow<BTState> get() = _bluetoothState

    private val _obdData = MutableStateFlow<MutableList<PIDListData>>(ArrayList())
    val obdData: StateFlow<MutableList<PIDListData>> get() = _obdData

    private val _leftFront = MutableStateFlow<CarEventModule.DoorState>(CarEventModule.DoorState.Closed)
    val leftFront: StateFlow<CarEventModule.DoorState> get() = _leftFront.asStateFlow()

    private val _rightFront = MutableStateFlow<CarEventModule.DoorState>(CarEventModule.DoorState.Closed)
    val rightFront: StateFlow<CarEventModule.DoorState> get() = _rightFront.asStateFlow()

    private val _leftRear = MutableStateFlow<CarEventModule.DoorState>(CarEventModule.DoorState.Closed)
    val leftRear: StateFlow<CarEventModule.DoorState> get() = _leftRear.asStateFlow()

    private val _rightRear = MutableStateFlow<CarEventModule.DoorState>(CarEventModule.DoorState.Closed)
    val rightRear: StateFlow<CarEventModule.DoorState> get() = _rightRear.asStateFlow()

    private val _trunk = MutableStateFlow<CarEventModule.DoorState>(CarEventModule.DoorState.Closed)
    val trunk: StateFlow<CarEventModule.DoorState> get() = _trunk.asStateFlow()

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