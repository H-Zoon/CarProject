package com.devidea.chevy.eventbus

import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.datas.obd.model.CarEventModel
import com.devidea.chevy.datas.obd.protocol.pid.PIDListData
import com.devidea.chevy.viewmodel.MainViewModel.NavRoutes
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
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

object UIEventBus : IEventBusBehavior<UIEvents> {
    private val _events = MutableSharedFlow<UIEvents>()
    override val events = _events.asSharedFlow()
    override suspend fun post(event: UIEvents) {
        _events.emit(event)
    }
}

sealed class UIEvents {
    data class reuestNavHost(val value: NavRoutes) : UIEvents()
    data class reuestBluetooth(val value: BTState) : UIEvents()
}

object KNNAVEventBus : IEventBusBehavior<GuidanceEvent> {
    private val _events = MutableSharedFlow<GuidanceEvent>(replay = 1, extraBufferCapacity = 10)
    override val events = _events.asSharedFlow()
    override suspend fun post(event: GuidanceEvent) {
        _events.emit(event)
    }
}

sealed class GuidanceEvent {
    data class RequestNavGuidance(val a: KNTrip, val b : KNRoutePriority, val c : Int) : GuidanceEvent()
    // 경로 변경 이벤트
    data class GuidanceCheckingRouteChange(val guidance: KNGuidance) : GuidanceEvent()
    data class GuidanceDidUpdateIndoorRoute(val guidance: KNGuidance, val route: KNRoute?) : GuidanceEvent()
    data class GuidanceDidUpdateRoutes(
        val guidance: KNGuidance,
        val routes: List<KNRoute>,
        val multiRouteInfo: KNMultiRouteInfo?
    ) : GuidanceEvent()

    // 길 안내 시작/종료 이벤트
    data class GuidanceGuideStarted(val guidance: KNGuidance) : GuidanceEvent()
    data class GuidanceGuideEnded(val guidance: KNGuidance) : GuidanceEvent()

    // 경로 이탈 및 변경 이벤트
    data class GuidanceOutOfRoute(val guidance: KNGuidance) : GuidanceEvent()
    data class GuidanceRouteChanged(
        val guidance: KNGuidance,
        val fromRoute: KNRoute,
        val fromLocation: KNLocation,
        val toRoute: KNRoute,
        val toLocation: KNLocation,
        val changeReason: KNGuideRouteChangeReason
    ) : GuidanceEvent()
    data class GuidanceRouteUnchanged(val guidance: KNGuidance) : GuidanceEvent()
    data class GuidanceRouteUnchangedWithError(val guidance: KNGuidance, val error: KNError) : GuidanceEvent()

    // C-ITS 안내 업데이트 이벤트
    data class DidUpdateCitsGuide(val guidance: KNGuidance, val citsGuide: KNGuide_Cits) : GuidanceEvent()

    // 위치 및 경로 안내 업데이트 이벤트
    data class GuidanceDidUpdateLocation(val guidance: KNGuidance, val locationGuide: KNGuide_Location) : GuidanceEvent()
    data class GuidanceDidUpdateRouteGuide(val guidance: KNGuidance, val routeGuide: KNGuide_Route) : GuidanceEvent()
    data class GuidanceDidUpdateSafetyGuide(val guidance: KNGuidance, val safetyGuide: KNGuide_Safety?) : GuidanceEvent()
    data class GuidanceDidUpdateAroundSafeties(val guidance: KNGuidance, val safeties: List<KNSafety>?) : GuidanceEvent()

    // 음성 안내 이벤트
    data class ShouldPlayVoiceGuide(
        val guidance: KNGuidance,
        val voiceGuide: KNGuide_Voice,
        val newData: MutableList<ByteArray>
    ) : GuidanceEvent()
    data class WillPlayVoiceGuide(val guidance: KNGuidance, val voiceGuide: KNGuide_Voice) : GuidanceEvent()
    data class DidFinishPlayVoiceGuide(val guidance: KNGuidance, val voiceGuide: KNGuide_Voice) : GuidanceEvent()
}