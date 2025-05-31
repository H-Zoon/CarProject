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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    // 내부에서 위치를 emit 할 SharedFlow (최근 1개를 replay)
    private val _locationUpdates = MutableSharedFlow<Location>(replay = 1)
    // 외부에 공개하는 읽기 전용 SharedFlow
    val locationUpdates: SharedFlow<Location> = _locationUpdates.asSharedFlow()

    // LocationCallback 을 클래스 레벨로 정의해서 start/stop 시 재사용
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _locationUpdates.tryEmit(loc)
            }
        }
    }

    @SuppressLint("MissingPermission")
     fun startLocationUpdates(
        intervalMs: Long = 1_000L,
        fastestMs: Long = 500L,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ) {
        val request = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateIntervalMillis(fastestMs)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * 필요할 때 호출해서 위치 업데이트를 중단할 수 있습니다.
     */
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}