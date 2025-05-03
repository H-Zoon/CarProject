package com.devidea.chevy.ui.map

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.devidea.chevy.network.AddressRepository
import com.devidea.chevy.LocationProvider
import com.devidea.chevy.Logger
import com.devidea.chevy.Trip
import com.devidea.chevy.storage.DataStoreRepository
import com.devidea.chevy.network.reomote.Document
import com.devidea.chevy.network.reomote.KakaoAddressResponse
import com.devidea.chevy.storage.AppDatabase
import com.devidea.chevy.storage.DocumentDao
import com.devidea.chevy.storage.DocumentEntity
import com.devidea.chevy.storage.DocumentRepository
import com.devidea.chevy.storage.DocumentTag
import com.devidea.chevy.ui.navi.NavigateData
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val locationProvider: LocationProvider,
    private val repository: DocumentRepository,
    context: Context
) :  AndroidViewModel(context as Application), DefaultLifecycleObserver {

    val allDocuments: Flow<List<DocumentEntity>> = repository.allDocuments
    val favoriteDocuments: Flow<List<DocumentEntity>> = repository.favoriteDocuments

    fun insert(document: DocumentEntity) = viewModelScope.launch { repository.insert(document) }
    fun deleteById(id: String) = viewModelScope.launch { repository.deleteById(id) }
    fun setTag(id: String, tag: DocumentTag?) = viewModelScope.launch { repository.setTag(id, tag) }
    fun toggleFavorite(id: String, isFav: Boolean) = viewModelScope.launch { repository.toggleFavorite(id, isFav) }

    fun isFavorite(id: String): Flow<Boolean> = repository.isFavorite(id)
    fun getTag(id: String): Flow<DocumentTag?> = repository.getTag(id)

    /** 특정 태그 문서 조회 */
    fun getDocumentByTag(tag: DocumentTag): Flow<DocumentEntity?> =
        repository.getDocumentByTag(tag)

    // 네트워크 응답 Document 객체로 즐겨찾기/태그 업데이트
    fun updateFavoriteFromNetwork(document: Document, isFav: Boolean) = viewModelScope.launch {
        repository.updateFavoriteFromNetwork(document, isFav)
    }
    fun updateTagFromNetwork(document: Document, tag: DocumentTag?) = viewModelScope.launch {
        repository.updateTagFromNetwork(document, tag)
    }

    val mapView: MapView by lazy {
        MapView(getApplication<Application>().applicationContext)
    }

    // 2) KakaoMap 참조 저장
    private val _kakaoMap = MutableStateFlow<KakaoMap?>(null)
    val kakaoMap: StateFlow<KakaoMap?> = _kakaoMap

    // 3) LifecycleObserver 구현으로 resume/pause 자동 호출
    override fun onResume(owner: LifecycleOwner) = mapView.resume()
    override fun onPause(owner: LifecycleOwner) = mapView.pause()
    //override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()

    // 4) start() 한 번만 실행하도록 플래그
    private var started = false
    fun startMap(onReady: (KakaoMap) -> Unit) {
        if (started) return
        started = true
        mapView.start(
            object : MapLifeCycleCallback() { /* ... */
                override fun onMapDestroy() {
                    TODO("Not yet implemented")
                }

                override fun onMapError(p0: java.lang.Exception?) {
                    TODO("Not yet implemented")
                }
            },
            object : KakaoMapReadyCallback() {
                override fun getPosition() = LatLng.from(37.5665, 126.9780)
                override fun onMapReady(map: KakaoMap) {
                    _kakaoMap.value = map
                    onReady(map)
                }
            }
        )
    }

    //경로 그리기를 위한 우선순위
    private val priorities = listOf(
        KNRoutePriority.KNRoutePriority_Recommand,
        KNRoutePriority.KNRoutePriority_Time,
        KNRoutePriority.KNRoutePriority_Distance,
        KNRoutePriority.KNRoutePriority_HighWay,
        KNRoutePriority.KNRoutePriority_WideWay
    )

    /** 모든 우선순위 경로를 병렬로 가져와 합쳐서 리턴 */
    suspend fun fetchAllRoutes(data: NavigateData): List<KNRoute> = withContext(Dispatchers.IO) {
        val trip = Trip.makeTripAsync(data)
        coroutineScope {
            priorities
                .map { priority ->
                    async { Trip.makeRouteAsync(trip, priority) }
                }
                .awaitAll()
                .flatten()
        }
    }

    // UI 상태를 관리하는 sealed class
    sealed class UiState {
        data object Idle : UiState() // 초기 상태
        data object IsSearching : UiState() // 초기 상태
        data class SearchResult(val items: List<Document>) : UiState() // 검색 결과 표시 중
        data class ShowDetail(val item: Document) : UiState() // 상세 정보 표시 중
        data class DrawRoute(val item: Document) : UiState() // 상세 정보 표시 중
    }

    // UI 이벤트를 관리하는 sealed class
    sealed class UiEvent {
        data class RequestSearch(val query: String) : UiEvent()
        data class SelectItem(val document: Document) : UiEvent()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage


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

    fun setLoadingState(value: Boolean) {
        _isLoading.value = value
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


    fun setState(state : UiState){
        _uiState.value = state
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.RequestSearch -> {
                searchAddress(event.query)
            }

            is UiEvent.SelectItem -> {
                setState(UiState.ShowDetail(event.document))
            }
        }
    }

    var lastSearchResult: List<Document>? = null

    // 주소 검색 함수
    private fun searchAddress(query: String) {
        _isLoading.value = true
        _errorMessage.value = null
        setLoadingState(true)
        viewModelScope.launch {
            try {
                val apiResult: KakaoAddressResponse? = addressRepository.searchAddress(query, 1, 10)
                apiResult?.let {
                    lastSearchResult = it.documents // 검색 결과를 캐싱
                    it.documents?.let {res ->  setState(UiState.SearchResult(res)) }
                } ?: run {
                    _errorMessage.value = "결과가 없습니다."
                    setState(UiState.Idle)
                }
                setLoadingState(false)
            } catch (e: Exception) {
                // 에러 처리
                Logger.e { "Error: ${e.message}" }
                _errorMessage.value = e.message
                setState(UiState.Idle)
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