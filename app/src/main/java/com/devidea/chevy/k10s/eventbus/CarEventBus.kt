package com.devidea.chevy.k10s.eventbus

import com.devidea.chevy.k10s.obd.model.CarEventModel
import com.devidea.chevy.k10s.obd.protocol.pid.PIDListData
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus : IEventBusBehavior<Event> {
    private val _events = MutableSharedFlow<Event>()
    override val events = _events.asSharedFlow()
    override suspend fun post(event: Event) {
        _events.emit(event)
    }
}

sealed class Event {
    data class carStateEvent(val message: ByteArray) : Event()
    data class TPMSEvent(val message: ByteArray) : Event()
    data class controlEvent(val message: ByteArray) : Event()
}

object CarEventBus : IEventBusBehavior<CarEvents> {
    private val _events = MutableSharedFlow<CarEvents>()
    override val events = _events.asSharedFlow()
    override suspend fun post(event: CarEvents) {
        _events.emit(event)
    }
}

sealed class CarEvents {
    data class updateObdData(val value: MutableList<PIDListData>) : CarEvents()
    data class updateLeftFront(val value: CarEventModel.DoorState) : CarEvents()
    data class updateRightFront(val value: CarEventModel.DoorState) : CarEvents()
    data class updateLeftRear(val value: CarEventModel.DoorState) : CarEvents()
    data class updateRightRear(val value: CarEventModel.DoorState) : CarEvents()
    data class updateTrunk(val value: CarEventModel.DoorState) : CarEvents()
    data class updateRotate(val value: Int) : CarEvents()
    data class updateRemainGas(val value: String) : CarEvents()
    data class updateMileage(val value: String) : CarEvents()
    data class updateHandBrake(val value: Boolean) : CarEvents()
    data class updateSeatbelt(val value: Boolean) : CarEvents()
    data class updateGear(val value: String) : CarEvents()
    data class updateGearNum(val value: Int) : CarEvents()
    data class updateErrCount(val value: Int) : CarEvents()
    data class updateVoltage(val value: String) : CarEvents()
    data class updateSolarTermDoor(val value: String) : CarEvents()
    data class updateWaterTemperature(val value: String) : CarEvents()
    data class updateThreeCatalystTemperatureBankOne1(val value: String) : CarEvents()
    data class updateThreeCatalystTemperatureBankOne2(val value: String) : CarEvents()
    data class updateThreeCatalystTemperatureBankTwo1(val value: String) : CarEvents()
    data class updateThreeCatalystTemperatureBankTwo2(val value: String) : CarEvents()
    data class updateMeterVoltage(val value: String) : CarEvents()
    data class updateMeterRotate(val value: Int) : CarEvents()
    data class updateSpeed(val value: Int) : CarEvents()
    data class updateTemp(val value: Int) : CarEvents()
    data class updateGearFunOnoffVisible(val value: Int) : CarEvents()
    data class updateGearDLock(val value: Int) : CarEvents()
    data class updateGearPUnlock(val value: Int) : CarEvents()
    data class updateCarVIN(val value: String) : CarEvents()
}

object KNAVStartEventBus : IEventBusBehavior<GuidanceStartEvent> {
    private val _events = MutableSharedFlow<GuidanceStartEvent>()
    override val events = _events.asSharedFlow()
    override suspend fun post(event: GuidanceStartEvent) {
        _events.emit(event)
    }
}

sealed class GuidanceStartEvent {
    data class RequestNavGuidance(val knTrip: KNTrip?, val knRoutePriority: KNRoutePriority, val curAvoidOptions: Int) : GuidanceStartEvent()
}