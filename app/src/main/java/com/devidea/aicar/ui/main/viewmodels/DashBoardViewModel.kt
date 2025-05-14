package com.devidea.aicar.ui.main.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.drive.usecase.DashBoardUseCase
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.ui.main.components.GaugeItem
import com.devidea.aicar.ui.main.components.gaugeItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashBoardViewModel @Inject constructor(
    private val repository: DataStoreRepository,
    private val pidManager: DashBoardUseCase
) : ViewModel() {

    //region 엔진 파라미터 스트림

    val rpm: SharedFlow<Int> = pidManager.rpm
    val speed: SharedFlow<Int> = pidManager.speed
    val ect: SharedFlow<Int> = pidManager.ect
    val throttle: SharedFlow<Int> = pidManager.throttle
    val load: SharedFlow<Int> = pidManager.load
    val iat: SharedFlow<Int> = pidManager.iat

    val maf: SharedFlow<Float> = pidManager.maf
    val batt: SharedFlow<Float> = pidManager.batt
    val fuelRate: SharedFlow<Float> = pidManager.fuelRate

    val currentGear: SharedFlow<Int> = pidManager.currentGear
    val oilPressure: SharedFlow<Float> = pidManager.oilPressure
    val oilTemp: SharedFlow<Int> = pidManager.oilTemp
    val transFluidTemp: SharedFlow<Int> = pidManager.transFluidTemp


    /** 연속 PID 데이터 폴링을 시작합니다. */
    fun startPalling() = pidManager.startPall()

    /** PID 데이터 폴링을 중지합니다. */
    fun stopPalling() = pidManager.stopAll()

    //endregion

    //region 게이지 관리

    /**
     * 현재 선택된 게이지 항목 리스트 (표시 순서 유지)
     */
    val gauges: StateFlow<List<GaugeItem>> = repository.selectedGaugeIds
        .map { ids ->
            ids.mapNotNull { id -> gaugeItems.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5_000), emptyList())

    /** 지정된 ID의 게이지를 토글합니다. */
    fun onGaugeToggle(id: String) = viewModelScope.launch {
        repository.toggleGauge(id)
    }

    /** 두 게이지 위치를 교체합니다. */
    suspend fun swap(from: Int, to: Int) {
        repository.swapGauge(from = from, to = to)
    }

    /** 모든 게이지를 기본 상태로 초기화합니다. */
    fun onReset() = viewModelScope.launch {
        repository.resetAllGauges()
    }

    //endregion
}