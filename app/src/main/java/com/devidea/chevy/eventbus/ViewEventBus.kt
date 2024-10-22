package com.devidea.chevy.eventbus

import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.datas.obd.module.CarEventModule
import com.devidea.chevy.datas.obd.protocol.pid.PIDListData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ViewEventBus : IEventBusBehavior<ViewEvent> {
    private val _events = MutableSharedFlow<ViewEvent>()
    override val events = _events.asSharedFlow()
    override suspend fun post(event: ViewEvent) {
        _events.emit(event)
    }
}

sealed class ViewEvent {
    data class updateObdData(val value: MutableList<PIDListData>) : ViewEvent()
    data class updateLeftFront(val value: CarEventModule.DoorState) : ViewEvent()
    data class updateRightFront(val value: CarEventModule.DoorState) : ViewEvent()
    data class updateLeftRear(val value: CarEventModule.DoorState) : ViewEvent()
    data class updateRightRear(val value: CarEventModule.DoorState) : ViewEvent()
    data class updateTrunk(val value: CarEventModule.DoorState) : ViewEvent()
    data class updateRotate(val value: Int) : ViewEvent()
    data class updateRemainGas(val value: String) : ViewEvent()
    data class updateMileage(val value: String) : ViewEvent()
    data class updateHandBrake(val value: Boolean) : ViewEvent()
    data class updateSeatbelt(val value: Boolean) : ViewEvent()
    data class updateGear(val value: String) : ViewEvent()
    data class updateGearNum(val value: Int) : ViewEvent()
    data class updateErrCount(val value: Int) : ViewEvent()
    data class updateVoltage(val value: String) : ViewEvent()
    data class updateSolarTermDoor(val value: String) : ViewEvent()
    data class updateWaterTemperature(val value: String) : ViewEvent()
    data class updateThreeCatalystTemperatureBankOne1(val value: String) : ViewEvent()
    data class updateThreeCatalystTemperatureBankOne2(val value: String) : ViewEvent()
    data class updateThreeCatalystTemperatureBankTwo1(val value: String) : ViewEvent()
    data class updateThreeCatalystTemperatureBankTwo2(val value: String) : ViewEvent()
    data class updateMeterVoltage(val value: String) : ViewEvent()
    data class updateMeterRotate(val value: Int) : ViewEvent()
    data class updateSpeed(val value: Int) : ViewEvent()
    data class updateTemp(val value: Int) : ViewEvent()
    data class updateGearFunOnoffVisible(val value: Int) : ViewEvent()
    data class updateGearDLock(val value: Int) : ViewEvent()
    data class updateGearPUnlock(val value: Int) : ViewEvent()
    data class updateCarVIN(val value: String) : ViewEvent()
    data class BluetoothState(val value: BTState) : ViewEvent()
}