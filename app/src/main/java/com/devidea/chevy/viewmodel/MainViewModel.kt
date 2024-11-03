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
import com.devidea.chevy.eventbus.UIEventBus
import com.devidea.chevy.eventbus.UIEvents
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

    enum class NavRoutes {
        HOME,
        DETAILS,
        MAP,
        NAV,
        LOGS,
    }

    private val _bluetoothStatus = MutableStateFlow(BTState.DISCONNECTED)
    val bluetoothStatus: StateFlow<BTState> get() = _bluetoothStatus

    private val _navigationEvent = MutableStateFlow(NavRoutes.HOME)
    val navigationEvent:StateFlow<NavRoutes> get() = _navigationEvent

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
                UIEventBus.events.collect {
                    when(it){
                        is UIEvents.reuestNavHost -> {
                            _navigationEvent.value = it.value
                        }
                        is UIEvents.reuestBluetooth -> {
                            _bluetoothStatus.value = it.value
                        }
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

}
