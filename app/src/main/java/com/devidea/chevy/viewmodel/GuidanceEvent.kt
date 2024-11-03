package com.devidea.chevy.viewmodel

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
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute

/*
sealed class GuidanceEvent {
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
}*/
