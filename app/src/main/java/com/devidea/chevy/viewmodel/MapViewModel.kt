package com.devidea.chevy.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.AddressRepository
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.carsystem.pid.PIDListData
import com.devidea.chevy.eventbus.ViewEvent
import com.devidea.chevy.eventbus.ViewEventBus
import com.devidea.chevy.repository.DataStoreRepository
import com.devidea.chevy.response.Document
import com.devidea.chevy.response.KakaoAddressResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: AddressRepository
) : ViewModel() {

    // UI 상태를 관리하는 sealed class
    sealed class UiState {
        object Idle : UiState() // 초기 상태
        object Searching : UiState() // 검색 중
        data class SearchResult(val items: List<Document>) : UiState() // 검색 결과 표시 중
        data class ShowDetail(val item: Document) : UiState() // 상세 정보 표시 중
    }

    // UI 이벤트를 관리하는 sealed class
    sealed class UiEvent {
        data class Search(val query: String) : UiEvent()
        data class SelectResult(val document: Document) : UiEvent()
        object ClearResult : UiEvent()
        object ClearDetail : UiEvent()  // 상세 다이얼로그 닫기 이벤트
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 검색 결과 초기화
    fun clearResult() {
        _uiState.value = UiState.Idle
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.Search -> {
                searchAddress(event.query)
            }
            is UiEvent.SelectResult -> {
                _uiState.value = UiState.ShowDetail(event.document)
            }
            UiEvent.ClearResult -> {
                clearResult()
            }
            UiEvent.ClearDetail -> {
                // 상세 보기 닫으면 다시 검색 결과로 이동
                if (_uiState.value is UiState.ShowDetail) {
                    _uiState.value = UiState.SearchResult(lastSearchResult ?: emptyList())
                }
            }
        }
    }

    private var lastSearchResult: List<Document>? = null
    var requestLoadTrip: Document? = null

    // 주소 검색 함수
    private fun searchAddress(query: String?) {
        _isLoading.value = true
        _errorMessage.value = null
        _uiState.value = UiState.Searching
        viewModelScope.launch {
            try {
                val apiResult: KakaoAddressResponse? = repository.searchAddress(query, 1, 10)
                apiResult?.let {
                    lastSearchResult = it.documents // 검색 결과를 캐싱
                    _uiState.value = it.documents?.let { it1 -> UiState.SearchResult(it1) }
                } ?: run {
                    _errorMessage.value = "결과가 없습니다."
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                // 에러 처리
                Log.e("AddressViewModel", "Error: ${e.message}")
                _errorMessage.value = e.message
                _uiState.value = UiState.Idle
            } finally {
                _isLoading.value = false
            }
        }
    }
}