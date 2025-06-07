package com.devidea.aicar.ui.main.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.aicar.drive.PollingManager
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.service.SppClient
import com.devidea.aicar.storage.datastore.DataStoreRepository
import com.devidea.aicar.storage.room.dtc.DtcInfoEntity
import com.devidea.aicar.storage.room.dtc.DtcInfoRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DtcViewModel @Inject constructor(
    private val sppClient: SppClient,
    private val pollingManager: PollingManager,
    private val dtcRepository: DtcInfoRepository,
    private val dataStoreRepository: DataStoreRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    init {
        viewModelScope.launch {
            initializeDtcDbIfNeeded()
        }
    }


    private val _dtcList = MutableStateFlow<List<String>>(emptyList())
    val dtcList: StateFlow<List<String>> = _dtcList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val connectionState: StateFlow<ConnectionEvent> = sppClient.connectionEvents

    val isPolling: Boolean
        get() = pollingManager.isPolling

    private val _selectedInfo = MutableStateFlow<DtcInfoEntity?>(null)
    val selectedInfo: StateFlow<DtcInfoEntity?> = _selectedInfo.asStateFlow()

    fun pausePolling() {
        pollingManager.pausePolling()
    }

    fun resumePolling() {
        pollingManager.resumePolling()
    }

    fun fetchDtcCodes() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            delay(1000L)
            try {
                val raw = sppClient.query(header = "7DF", cmd = "03", timeoutMs = 5000L)
                val parsed = parseDtcResponse(raw)
                _dtcList.value = parsed
            } catch (e: Exception) {
                _errorMessage.value = "고장코드 조회 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDtcCodes() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            delay(1000L)
            try {
                sppClient.query(header = "7DF", cmd = "04", timeoutMs = 5000L)
                delay(500)
                fetchDtcCodes()
            } catch (e: Exception) {
                _errorMessage.value = "고장코드 삭제 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchInfo(code: String) {
        viewModelScope.launch {
            _selectedInfo.value = dtcRepository.getDtcInfo(code)
        }
    }

    private fun parseDtcResponse(raw: String): List<String> {
        if (!raw.startsWith("43")) return emptyList()
        val payload = raw.removePrefix("43").chunked(4)
        return payload.mapNotNull { hex ->
            if (hex.length != 4) return@mapNotNull null
            val b1 = hex[0].digitToInt(16)
            val b2 = hex.substring(1)
            val type = when (b1 shr 6) {
                0b00 -> "P"
                0b01 -> "C"
                0b10 -> "B"
                0b11 -> "U"
                else -> "?"
            }
            "$type${(b1 and 0b00111111).toString(16).padStart(2, '0').uppercase()}$b2"
        }
    }

    private suspend fun initializeDtcDbIfNeeded() {
        val alreadyInitialized = dataStoreRepository.isDtcDbInitialized.first()
        if (!alreadyInitialized) {
            try {
                preloadDtcInfoFromAssets(context, dtcRepository)
                dataStoreRepository.setDtcDbInitialized(true)
                Log.d("DtcViewModel", "DTC DB 초기화 완료")
            } catch (e: Exception) {
                Log.e("DtcViewModel", "DTC DB 초기화 실패: ${e.message}", e)
            }
        } else {
            Log.d("DtcViewModel", "DTC DB 이미 초기화됨")
        }
    }

    suspend fun preloadDtcInfoFromAssets(context: Context, dao: DtcInfoRepository) {
        val json = context.assets.open("dtc_info.json").bufferedReader().use { it.readText() }
        val list: List<DtcInfoEntity> = Gson().fromJson(
            json, object : TypeToken<List<DtcInfoEntity>>() {}.type
        )
        dao.setDtcInfo(list)
    }
}