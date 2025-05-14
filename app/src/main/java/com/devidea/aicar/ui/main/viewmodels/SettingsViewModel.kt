package com.devidea.aicar.ui.main.viewmodels

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.module.AppModule
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.storage.datastore.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: DataStoreRepository,
    private val bluetoothAdapter: BluetoothAdapter,
    @AppModule.ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    // 마지막 저장된 디바이스 가져오기
    val lastSavedDevice: StateFlow<ScannedDevice?> =
        repo.getDevice.map { stored ->
            stored?.let {
                // 실제 BluetoothDevice 객체 복원
                val bt = bluetoothAdapter.getRemoteDevice(it.address)
                ScannedDevice(it.name, it.address, bt)
            }
        }
            .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, null)

    // 충전 시 자동 연결 설정 읽기/쓰기
    val autoConnectOnCharge: StateFlow<Boolean> = repo.isAutoConnectOnCharge
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, false)

    fun setAutoConnectOnCharge(enabled: Boolean) {
        viewModelScope.launch {
            repo.setAutoConnectOnCharge(enabled)
        }
    }
    fun resetSavedDevice() {
        viewModelScope.launch {  }
    }

    fun resetDrivingRecord() {
        viewModelScope.launch {  }
    }

    fun resetWidgetSettings() {
        viewModelScope.launch { }
    }

    fun resetAll() {
        viewModelScope.launch { }
    }
}