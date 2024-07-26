package com.devidea.chevy.viewmodel

import androidx.lifecycle.ViewModel
import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.carsystem.OBDData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CarViewModel @Inject constructor() : ViewModel() {
    private val _obdData = MutableStateFlow<OBDData>(OBDData())
    val obdData: StateFlow<OBDData> get() = _obdData

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

    // Update functions for each variable
    fun updateObdData(data: OBDData) { _obdData.value = data }
    fun updateLeftFront(value: CarEventModule.DoorState) { _leftFront.value = value }
    fun updateRightFront(value: CarEventModule.DoorState) { _rightFront.value = value }
    fun updateLeftRear(value: CarEventModule.DoorState) { _leftRear.value = value }
    fun updateRightRear(value: CarEventModule.DoorState) { _rightRear.value = value }
    fun updateTrunk(value: CarEventModule.DoorState) { _trunk.value = value }
    fun updateRotate(value: Int) { _mRotate.value = value }
    fun updateRemainGas(value: String) { _mRemainGas.value = value }
    fun updateMileage(value: String) { _mMileage.value = value }
    fun updateHandBrake(value: Boolean) { _mHandBrake.value = value }
    fun updateSeatbelt(value: Boolean) { _mSeatbelt.value = value }
    fun updateGear(value: String) { _mGear.value = value }
    fun updateGearNum(value: Int) { _mGearNum.value = value }
    fun updateErrCount(value: Int) { _mErrCount.value = value }
    fun updateVoltage(value: String) { _mVoltage.value = value }
    fun updateSolarTermDoor(value: String) { _mSolarTermDoor.value = value }
    fun updateWaterTemperature(value: String) { _mWaterTemperature.value = value }
    fun updateThreeCatalystTemperatureBankOne1(value: String) { _mThreeCatalystTemperatureBankOne1.value = value }
    fun updateThreeCatalystTemperatureBankOne2(value: String) { _mThreeCatalystTemperatureBankOne2.value = value }
    fun updateThreeCatalystTemperatureBankTwo1(value: String) { _mThreeCatalystTemperatureBankTwo1.value = value }
    fun updateThreeCatalystTemperatureBankTwo2(value: String) { _mThreeCatalystTemperatureBankTwo2.value = value }
    fun updateMeterVoltage(value: String) { _mMeterVoltage.value = value }
    fun updateMeterRotate(value: Int) { _mMeterRotate.value = value }
    fun updateSpeed(value: Int) { _mSpeed.value = value }
    fun updateTemp(value: Int) { _mTemp.value = value }
    fun updateGearFunOnoffVisible(value: Int) { _mGearFunOnoffVisible.value = value }
    fun updateGearDLock(value: Int) { _mGearDLock.value = value }
    fun updateGearPUnlock(value: Int) { _mGearPUnlock.value = value }
    fun updateCarVIN(value: String) { _carVIN.value = value }
}