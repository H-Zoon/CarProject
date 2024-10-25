package com.devidea.chevy.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.repository.remote.AddressRepository
import com.devidea.chevy.LocationProvider
import com.devidea.chevy.Logger
import com.devidea.chevy.repository.device.DataStoreRepository
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.repository.remote.KakaoAddressResponse
import com.kakao.vectormap.LatLng
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val locationProvider: LocationProvider
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

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 인증 성공 여부를 관리하는 Flow
    private val _authenticationSuccess = MutableStateFlow(false)
    val authenticationSuccess: StateFlow<Boolean> = _authenticationSuccess.asStateFlow()

    // 인증 중 상태를 관리하는 Flow
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // 에러 메시지 상태를 관리하는 Flow
    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    // 카메라 추적 상태를 관리하는 Flow
    private val _cameraIsTracking = MutableStateFlow(true)
    val cameraIsTracking: StateFlow<Boolean> = _cameraIsTracking.asStateFlow()

    // 사용자 위치를 관리하는 Flow
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    // 검색기록 Flow
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private var trackingJob: Job? = null

    init {
        // 초기 위치 업데이트 시작
        startLocationUpdates()

        viewModelScope.launch {
            launch {
                dataStoreRepository.getSearchHistory().collect { history ->
                    _searchHistory.value = history
                }
            }
        }
    }

    // 검색어 추가
    fun addSearchQuery(query: String) {
        viewModelScope.launch {
            dataStoreRepository.addSearchQuery(query)
        }
    }

    // 검색 히스토리 삭제
    fun clearSearchHistory() {
        viewModelScope.launch {
            dataStoreRepository.clearSearchHistory()
        }
    }

    // 개별 검색 히스토리 삭제
    fun removeSearchQuery(value: String) {
        viewModelScope.launch {
            dataStoreRepository.removeSearchQuery(value)
        }
    }
    // 카메라 추적 상태 설정
    fun setCameraTracking(value: Boolean) {
        _cameraIsTracking.value = value
        if (value) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    // 위치 업데이트 시작
    private fun startLocationUpdates() {
        locationProvider.requestLocationUpdates { location ->
            _userLocation.value = LatLng.from(location.latitude, location.longitude)
        }
    }

    // 카메라 추적 시작
    private fun startTracking() {
        trackingJob = viewModelScope.launch {
            _cameraIsTracking.collect { isTracking ->
                if (isTracking) {
                    // 카메라가 사용자의 위치를 추적하도록 로직 구현
                    // 예: 맵의 카메라를 사용자 위치로 이동
                } else {
                    // 카메라 추적 중지 로직 구현
                }
            }
        }
    }

    // 카메라 추적 중지
    private fun stopTracking() {
        trackingJob?.cancel()
    }

    // 인증 시작
    fun authenticateUser() {
        CoroutineScope(Dispatchers.IO).launch {
            _isAuthenticating.value = true
            KNSDK.apply {
                initializeWithAppKey(
                    aAppKey = "e31e85ed66b03658041340618628e93f",
                    aClientVersion = "1.0.0",
                    aAppUserId = null,
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

    // 주소 검색 함수
    private fun searchAddress(query: String?) {
        _isLoading.value = true
        _errorMessage.value = null
        _uiState.value = UiState.Searching
        viewModelScope.launch {
            try {
                val apiResult: KakaoAddressResponse? = addressRepository.searchAddress(query, 1, 10)
                apiResult?.let {
                    lastSearchResult = it.documents // 검색 결과를 캐싱
                    _uiState.value = it.documents?.let { it1 -> UiState.SearchResult(it1) }!!
                } ?: run {
                    _errorMessage.value = "결과가 없습니다."
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                // 에러 처리
                Logger.e{"Error: ${e.message}"}
                _errorMessage.value = e.message
                _uiState.value = UiState.Idle
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopLocationUpdates()
        stopTracking()
    }
}