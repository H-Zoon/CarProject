package com.devidea.chevy.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.Logger
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.datas.navi.NavigateDocument
import com.devidea.chevy.repository.device.DataStoreRepository
import com.devidea.chevy.repository.remote.Document
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataStoreRepository
) : ViewModel() {

    sealed class NavRoutes {
        val HOME = "home"
        val DETAILS = "details"
        val MAP = "home"
        val NAV = "home"
        val LOGS = "logs"
    }

    private val _bluetoothStatus = MutableStateFlow(BTState.DISCONNECTED)
    val bluetoothStatus: StateFlow<BTState> get() = _bluetoothStatus

    init {
        viewModelScope.launch {
            launch {
                repository.getConnectDate().collect { difference ->
                    _lastConnectDate.value =
                        when (difference) {
                            (-1).toLong() -> "-"
                            (0).toLong() -> "방금전"
                            else -> "$difference 일 전"
                        }
                }
            }

            launch {
                repository.getMileageDate().collect { difference ->
                    _recentMileage.value = when (difference) {
                        -1 -> "-"
                        else -> "$difference Km"
                    }
                }
            }

            launch {
                repository.getFuelDate().collect { difference ->
                    _fullEfficiency.value = when (difference) {
                        (-1).toFloat() -> "-"
                        else -> "$difference Km/L"
                    }
                }
            }
            launch {
                BluetoothModel.bluetoothStatus.collect {
                    _bluetoothStatus.value = it
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

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // 네비게이션 요청 함수
    fun navigateTo(route: String) {
        _navigationEvent.emit(route)
    }

    fun foundRoot(addressName: String, goalX: Double, goalY: Double, startX: Double, startY: Double, failure: (() -> Unit)) {
        val startKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = startX, aWgs84Lat = startY)
        val goalKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = goalX, aWgs84Lat = goalY)

        val startKNPOI = KNPOI("", startKATEC.x.toInt(), startKATEC.y.toInt(), aAddress = null)
        val goalKNPOI = KNPOI("", goalKATEC.x.toInt(), goalKATEC.y.toInt(), addressName)

        val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
        val curAvoidOptions =
            KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value

        KNSDK.makeTripWithStart(aStart = startKNPOI, aGoal = goalKNPOI, aVias = null) { knError, knTrip ->
            if (knError != null) {
                Logger.e { "경로 생성 에러(KNError: $knError" }
            }
            knTrip?.routeWithPriority(curRoutePriority, curAvoidOptions) { error, _ ->
                if (error != null) {
                    failure()
                } else {
                    KNSDK.sharedGuidance()?.apply {
                        navigateTo("${NavRoutes.DETAILS}/2")
                    }
                }
            }
        }
    }
}
