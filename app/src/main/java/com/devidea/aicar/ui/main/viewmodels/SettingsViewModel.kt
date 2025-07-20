package com.devidea.aicar.ui.main.viewmodels

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.module.AppModule
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.drive.DrivingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val dataStoreRepository: DataStoreRepository,
        private val drivingRepository: DrivingRepository,
        private val bluetoothAdapter: BluetoothAdapter,
        @AppModule.ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        // 마지막 저장된 디바이스 가져오기
        val lastSavedDevice: StateFlow<ScannedDevice?> =
            dataStoreRepository.getDevice
                .map { stored ->
                    stored?.let {
                        // 실제 BluetoothDevice 객체 복원
                        val bt = bluetoothAdapter.getRemoteDevice(it.address)
                        ScannedDevice(it.name, it.address, bt)
                    }
                }.stateIn(viewModelScope, SharingStarted.Companion.Eagerly, null)

        // 충전 시 자동 연결 설정 읽기/쓰기
        val autoConnectOnCharge: StateFlow<Boolean> =
            dataStoreRepository.isAutoConnectOnCharge
                .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, false)

        fun setAutoConnectOnCharge(enabled: Boolean) {
            viewModelScope.launch {
                dataStoreRepository.setAutoConnectOnCharge(enabled)
            }
        }

        fun resetSavedDevice() {
            viewModelScope.launch {
                dataStoreRepository.clearSavedDevice()
            }
        }

        fun resetDrivingRecord() {
            viewModelScope.launch {
                drivingRepository.deleteAllSessions()
            }
        }

        fun resetWidgetSettings() {
            viewModelScope.launch {
                dataStoreRepository.resetAllGauges()
            }
        }

        fun resetAll() {
            viewModelScope.launch {
                dataStoreRepository.clearSavedDevice()
                drivingRepository.deleteAllSessions()
                dataStoreRepository.resetAllGauges()
            }
        }

        // ---------- 새로 추가된 유류비(Fuel Cost) 관련 ----------

        // 저장된 유류비를 Flow로 구독해서 Compose에 전달
        val fuelCost: StateFlow<Int> =
            dataStoreRepository.fuelCostFlow
                .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

        // 유류비를 저장 또는 수정
        fun setFuelCost(cost: Int) {
            viewModelScope.launch {
                dataStoreRepository.setFuelCost(cost)
            }
        }
    }
