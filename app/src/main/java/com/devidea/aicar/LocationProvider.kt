package com.devidea.aicar

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * 지속적으로 위치를 방출하며, 호출한 측에서 collect를 취소할 때까지 유지됩니다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    fun locationUpdates(
        intervalMs: Long = 1_000L,
        fastestMs: Long = 500L,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateIntervalMillis(fastestMs)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(loc).isSuccess
                }
            }
        }

        // 위치 업데이트 시작
        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        // 호출 측이 Flow를 취소할 때 실행되어 콜백 해제
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
        .conflate()                // 최신 위치만 유지
        .flowOn(Dispatchers.Main)  // FusedLocationProvider는 MainLooper에서 동작
}
