package com.devidea.chevy.ui.main

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.App.Companion.instance
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.repository.device.DataStoreRepository
import com.devidea.chevy.service.BleService
import com.devidea.chevy.service.BleServiceManager
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataStoreRepository,
    private val serviceManager: BleServiceManager
) : ViewModel() {

    // NavRoutes를 sealed class로 변경
    sealed class NavRoutes(val route: String) {
        object A : NavRoutes("a")
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

    // 인증 성공 여부를 관리하는 Flow
    private val _authenticationSuccess = MutableStateFlow(false)
    val authenticationSuccess: StateFlow<Boolean> = _authenticationSuccess.asStateFlow()

    // 인증 중 상태를 관리하는 Flow
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // 에러 메시지 상태를 관리하는 Flow
    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

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

    // 인증 시작
    fun authenticateUser() {
        CoroutineScope(Dispatchers.IO).launch {
            _isAuthenticating.value = true
            KNSDK.apply {
                initializeWithAppKey(
                    aAppKey = "e31e85ed66b03658041340618628e93f",
                    aClientVersion = "1.0.0",
                    aAppUserId = Settings.Secure.getString(instance.contentResolver, Settings.Secure.ANDROID_ID),
                    aLangType = KNLanguageType.KNLanguageType_KOREAN,
                    aCompletion = { result ->
                        result?.let {
                            _isAuthenticating.value = false
                            when (it.code) {
                                KNError_Code_C103 -> {
                                    // 인증 실패 처리
                                    _authErrorMessage.value = "인증 실패: 코드 C103"
                                }

                                KNError_Code_C302 -> {
                                    // 권한 오류 처리
                                    _authErrorMessage.value = "권한 오류: 코드 C302"
                                }

                                else -> {
                                    // 기타 오류 처리
                                    _authErrorMessage.value = "초기화 실패: 알 수 없는 오류"
                                }
                            }
                        } ?: run {
                            _authenticationSuccess.value = true
                        }
                    }
                )
            }
        }
    }

    // 에러 메시지 초기화
    fun clearError() {
        _authErrorMessage.value = null
    }

}
