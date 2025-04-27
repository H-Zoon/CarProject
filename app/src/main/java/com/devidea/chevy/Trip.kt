package com.devidea.chevy

import com.devidea.chevy.ui.navi.NavigateData
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Trip {

    val knTrip: KNTrip? = null
    val curRoutePriority: KNRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
    val curAvoidOptions: Int = 0

    suspend fun makeTripAsync(data: NavigateData): KNTrip =
        suspendCancellableCoroutine { cont ->
            // 좌표 변환
            val startKATEC = KNSDK.convertWGS84ToKATEC(
                aWgs84Lon = data.startX, aWgs84Lat = data.startY
            )
            val goalKATEC = KNSDK.convertWGS84ToKATEC(
                aWgs84Lon = data.goalX, aWgs84Lat = data.goalY
            )
            // POI 생성
            val startPOI = KNPOI("", startKATEC.x.toInt(), startKATEC.y.toInt(), null)
            val goalPOI  = KNPOI("", goalKATEC.x.toInt(),  goalKATEC.y.toInt(), data.addressName)

            // SDK 호출
            KNSDK.makeTripWithStart(startPOI, goalPOI, null) { err, trip ->
                if (err != null) cont.resumeWithException(RuntimeException(err.toString()))
                else if (trip != null) cont.resume(trip)
                else cont.resumeWithException(IllegalStateException("trip==null"))
            }
            cont.invokeOnCancellation { /* 필요시 취소 로직 */ }
        }

    suspend fun makeRouteAsync(
        trip: KNTrip,
        priority: KNRoutePriority
    ): List<KNRoute> =
        suspendCancellableCoroutine { cont ->
            val avoidOpts = KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value
            trip.routeWithPriority(priority, avoidOpts) { err, routes ->
                if (err != null) cont.resumeWithException(RuntimeException(err.toString()))
                else if (routes != null) cont.resume(routes)
                else cont.resumeWithException(IllegalStateException("routes==null"))
            }
            cont.invokeOnCancellation { /* 취소 시 필요하다면 SDK 호출 해제 */ }
        }
}