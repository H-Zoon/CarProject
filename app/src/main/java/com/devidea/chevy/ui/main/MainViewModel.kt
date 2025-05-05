package com.devidea.chevy.ui.main

import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.App.Companion.instance
import com.devidea.chevy.bluetooth.GaugeManager
import com.devidea.chevy.bluetooth.GaugeSnapshot
import com.devidea.chevy.bluetooth.GmExtManagerPlus
import com.devidea.chevy.bluetooth.GmSnapshotPlus
import com.devidea.chevy.storage.DataStoreRepository
import com.devidea.chevy.service.ConnectionEvent
import com.devidea.chevy.service.ScannedDevice
import com.devidea.chevy.service.SppClient
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataStoreRepository,
    private val sppClient: SppClient,
    private val gaugeManager: GaugeManager,
    private val gmExtManagerPlus: GmExtManagerPlus
) : ViewModel() {

    private val _devices = MutableLiveData<List<ScannedDevice>>()
    val devices: LiveData<List<ScannedDevice>> = _devices

    private val _events = MutableSharedFlow<ConnectionEvent>()
    val events: SharedFlow<ConnectionEvent> = _events.asSharedFlow()

    private val _dtcCodes = MutableStateFlow<List<String>>(emptyList())
    val dtcCodes: StateFlow<List<String>> = _dtcCodes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
                sppClient.deviceList.collect { list ->
                    _devices.postValue(list)
                }
            }

            launch {
                sppClient.connectionEvents.collect { evt ->
                    _events.emit(evt)
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


    val snapshot: StateFlow<GaugeSnapshot> = gaugeManager
        .snapshot
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GaugeSnapshot()
        )

    val snapshot2: StateFlow<GmSnapshotPlus> = gmExtManagerPlus
        .snapshot
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GmSnapshotPlus()
        )

   fun startInfo(){
       gaugeManager.start()
    }

    fun stopInfo(){
        gaugeManager.stop()
    }

    fun startGM(){
        gmExtManagerPlus.start()
    }

    fun stopGM(){
        gmExtManagerPlus.stop()
    }

    suspend fun saveFirstLunch() {
        repository.saveFirstLunch(false)
    }

    fun startScan() {
        sppClient.requestScan()
    }

    fun connectTo(scannedDevice: ScannedDevice) {
        sppClient.requestConnect(scannedDevice)
    }

    fun disconnect() {
        sppClient.requestDisconnect()
    }

    // 인증 시작
    fun authenticateUser() {
        CoroutineScope(Dispatchers.IO).launch {
            _isAuthenticating.value = true
            KNSDK.apply {
                initializeWithAppKey(
                    aAppKey = "e31e85ed66b03658041340618628e93f",
                    aClientVersion = "1.0.0",
                    aAppUserId = Settings.Secure.getString(
                        instance.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ),
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
