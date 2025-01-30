package com.devidea.chevy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.App
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.eventbus.KNAVStartEventBus
import com.devidea.chevy.repository.device.DataStoreRepository
import com.devidea.chevy.service.BleService
import com.devidea.chevy.service.BleServiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataStoreRepository,
    private val serviceManager: BleServiceManager
) : ViewModel() {

    // NavRoutes를 sealed class로 변경
    sealed class NavRoutes(val route: String) {
        object Home : NavRoutes("home")
        object Details : NavRoutes("details")
        object Map : NavRoutes("map")
        //object Nav : NavRoutes("nav")
        object PERMISSION : NavRoutes("permission")
    }

    private val _bluetoothStatus = MutableStateFlow(BTState.DISCONNECTED)
    val bluetoothStatus: StateFlow<BTState> get() = _bluetoothStatus

    private val _navigationEvent = MutableSharedFlow<NavRoutes>(replay = 0)
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _requestNavGuidance = MutableStateFlow<GuidanceStartEvent.RequestNavGuidance?>(null)
    val requestNavGuidance: StateFlow<GuidanceStartEvent.RequestNavGuidance?> get() = _requestNavGuidance

    private val _isServiceInitialized = MutableStateFlow(false)
    val isServiceInitialized: StateFlow<Boolean> get() = _isServiceInitialized

    private val _isFirstLunch = MutableStateFlow(false)
    val isFirstLunch: StateFlow<Boolean> get() = _isFirstLunch

    init {
        viewModelScope.launch {
            launch {
                serviceManager.serviceState.collect { service ->
                    if (service != null) {
                        observeBtState(service)
                        _isServiceInitialized.value = true
                    } else {
                        _isServiceInitialized.value = true
                    }
                }
            }

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

            /*launch {
                KNAVStartEventBus.events.collect {
                    when (it) {
                        is GuidanceStartEvent.RequestNavGuidance -> {
                            _requestNavGuidance.value = it
                            _navigationEvent.emit(NavRoutes.Nav)
                        }
                    }
                }
            }*/
            launch {
                repository.getFirstLunch().collect { difference ->
                    _isFirstLunch.value = difference
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

    suspend fun requestNavHost(value: NavRoutes) {
        _navigationEvent.emit(value)
    }

    private fun observeBtState(service: BleService) {
        viewModelScope.launch {
            service.btState.collect { state ->
                _bluetoothStatus.value = state
            }
        }
    }

    suspend fun saveFirstLunch() {
        repository.saveFirstLunch(false)
    }

    fun connect() {
        if(isServiceInitialized.value) serviceManager.getService()?.requestScan()
    }

    fun disconnect() {
        if(isServiceInitialized.value) serviceManager.getService()?.requestDisconnect()
    }
}
