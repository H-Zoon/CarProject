package com.devidea.chevy

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.devidea.chevy.response.Document
import com.devidea.chevy.viewmodel.MapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.animation.Interpolation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.TrackingManager
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyles
import com.kakao.vectormap.shape.PolygonStylesSet
import com.kakao.vectormap.shape.ShapeAnimator
import com.kakao.vectormap.shape.animation.CircleWave
import com.kakao.vectormap.shape.animation.CircleWaves
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MapActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var trackingManager: TrackingManager? = null
    private var kakaoMaps: KakaoMap? = null
    private var centerLabel: Label? = null
    private var animationPolygon: Polygon? = null
    private var detailLabel: Label? = null
    private var userLocation = LatLng.from(37.5665, 126.9780)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            BActivityScreen(viewModel)
        }
    }

    private fun collectEvent() {
        // StateFlow 관찰
        lifecycleScope.launch {
            viewModel.cameraIsTracking.collect { isTracking ->
                if (isTracking) {
                    trackingManager?.startTracking(centerLabel)
                    trackingManager?.setTrackingRotation(false)
                } else {
                    trackingManager?.stopTracking()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // 1초마다 업데이트 (밀리초 단위)
        ).setMinUpdateIntervalMillis(500L) // 가장 빠른 업데이트 간격
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    userLocation = LatLng.from(37.645143328591324, 126.6878879070282)
                    centerLabel?.moveTo(userLocation)
                }
            }
        }

        // 위치 업데이트 시작
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    @Composable
    fun BActivityScreen(viewModel: MapViewModel) {
        // ViewModel의 Flow 상태를 collectAsState로 관찰
        val authenticationSuccess by viewModel.authenticationSuccess.collectAsState()
        val isAuthenticating by viewModel.isAuthenticating.collectAsState()
        val errorMessage by viewModel.authErrorMessage.collectAsState()

        if (authenticationSuccess) {
            // 인증 성공 후 SearchHistoryApp으로 이동
            SearchHistoryApp(viewModel)
        } else {
            // 인증 프로세스 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("B Activity 설명: 여기는 네비게이션이 설정된 B 액티비티입니다.")

                Button(
                    onClick = {
                        if (!isAuthenticating) {
                            // 인증 시작
                            viewModel.authenticateUser()
                        }
                    },
                    enabled = !isAuthenticating  // 인증 중이면 버튼 비활성화
                ) {
                    if (isAuthenticating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                            Text("인증 중...")
                        }
                    } else {
                        Text("인증 후 B Activity로 이동")
                    }
                }

                // 에러 메시지가 있을 경우 AlertDialog 표시
                errorMessage?.let { error ->
                    AlertDialog(
                        onDismissRequest = { viewModel.clearError() },
                        title = { Text("에러 발생") },
                        text = { Text(error) },
                        confirmButton = {
                            Button(onClick = { viewModel.clearError() }) {
                                Text("확인")
                            }
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    fun MapScreen() {
        val mapView = rememberMapViewWithLifecycle()
        AndroidView({ mapView }) { view ->
        }
    }

    @Composable
    fun rememberMapViewWithLifecycle(): View {
        val context = LocalContext.current
        val mapView = remember { MapView(context) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    mapView.start(object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {
                            // 지도 API 가 정상적으로 종료될 때 호출됨
                        }

                        override fun onMapError(error: Exception) {
                            // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출됨
                        }
                    }, object : KakaoMapReadyCallback() {
                        override fun getPosition(): LatLng {
                            return userLocation
                        }

                        override fun onMapReady(kakaoMap: KakaoMap) {
                            kakaoMaps = kakaoMap
                            trackingManager = kakaoMap.trackingManager

                            // 중심 라벨 생성
                            centerLabel = kakaoMap.labelManager?.layer?.addLabel(
                                LabelOptions.from("dotLabel", userLocation)
                                    .setStyles(
                                        LabelStyle.from(R.drawable.c).setAnchorPoint(0.5f, 0.5f)
                                    )
                                    .setRank(1)
                            )

                            animationPolygon = kakaoMap.shapeManager?.layer?.addPolygon(
                                PolygonOptions.from("circlePolygon")
                                    .setDotPoints(DotPoints.fromCircle(userLocation, 1.0f))
                                    .setStylesSet(
                                        PolygonStylesSet.from(
                                            PolygonStyles.from(
                                                Color(
                                                    0xFFff722b
                                                ).toArgb()
                                            )
                                        )
                                    )
                            )

                            val circleWaves: CircleWaves = CircleWaves.from(
                                "circleWaveAnim",
                                CircleWave.from(1F, 0F, 0F, 100F)
                            )
                                .setHideShapeAtStop(false)
                                .setInterpolation(Interpolation.CubicInOut)
                                .setDuration(1500)
                                .setRepeatCount(500)
                            val shapeAnimator: ShapeAnimator? =
                                kakaoMap.shapeManager?.addAnimator(circleWaves)

                            shapeAnimator?.addPolygons(animationPolygon)
                            shapeAnimator?.start()

                            centerLabel?.addShareTransform(animationPolygon)

                            collectEvent()
                            startLocationUpdates()

                            kakaoMap.setOnCameraMoveStartListener { kakaoMap, gestureType ->
                                viewModel.setCameraTracking(false)
                            }

                        }
                    })
                }

                override fun onStart(owner: LifecycleOwner) {

                }

                override fun onResume(owner: LifecycleOwner) {
                    mapView.resume()
                }

                override fun onPause(owner: LifecycleOwner) {
                    mapView.pause()
                }

                override fun onStop(owner: LifecycleOwner) {

                }

                override fun onDestroy(owner: LifecycleOwner) {

                }
            }

            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
        return mapView
    }

    @Composable
    fun SearchHistoryApp(viewModel: MapViewModel) {
        val context = LocalContext.current

        val uiState by viewModel.uiState.observeAsState(MapViewModel.UiState.Idle)
        val focusManager = LocalFocusManager.current
        var searchText by remember { mutableStateOf(TextFieldValue("")) }
        var isSearchHistoryVisible by remember { mutableStateOf(false) }
        var isSearchButtonVisible by remember { mutableStateOf(true) }
        val cameraState by viewModel.cameraIsTracking.collectAsState()

        // BackHandler 처리
        BackHandler {
            when (uiState) {
                is MapViewModel.UiState.ShowDetail -> {
                    viewModel.onEvent(MapViewModel.UiEvent.ClearDetail)

                    detailLabel?.let {
                        kakaoMaps?.labelManager?.layer?.remove(detailLabel)
                    }
                }

                is MapViewModel.UiState.SearchResult -> {
                    viewModel.onEvent(MapViewModel.UiEvent.ClearResult)
                }

                else -> {
                    if (isSearchHistoryVisible || isSearchButtonVisible) {
                        focusManager.clearFocus()
                        searchText = TextFieldValue("")
                    } else {
                        (context as? Activity)?.finish()
                    }
                    // Idle 상태이거나 다른 상태에서는 기본 뒤로가기 동작 (앱 종료 또는 다른 처리)
                }
            }
        }

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 검색 입력 필드
                    val textFieldWidth by animateDpAsState(
                        targetValue = if (isSearchHistoryVisible) 250.dp else 300.dp,
                        animationSpec = tween(durationMillis = 300)
                    )
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("검색어 입력") },
                        modifier = Modifier
                            .width(textFieldWidth)
                            .background(Color.Transparent)
                            .onFocusChanged { focusState ->
                                isSearchHistoryVisible = focusState.isFocused
                                isSearchButtonVisible = focusState.isFocused
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 검색 버튼에 애니메이션 적용
                    AnimatedVisibility(
                        visible = isSearchButtonVisible,
                        enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(),
                        exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.onEvent(MapViewModel.UiEvent.Search(searchText.text)) // 검색 실행
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "검색")
                        }
                    }
                }
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MapScreen()
                    // 상태에 따른 UI 처리
                    when (uiState) {
                        is MapViewModel.UiState.Idle -> {
                            // 우하단에 원형 버튼 추가
                            FloatingActionButton(
                                onClick = {
                                    viewModel.setCameraTracking(!cameraState)
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                                shape = CircleShape,
                                containerColor = if (cameraState) Color.Blue else Color.Gray
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn, // 원하는 아이콘으로 변경
                                    contentDescription = "내 위치",
                                    tint = Color.White
                                )
                            }
                            // 텍스트 필드가 포커싱되면 검색 기록 표시
                            AnimatedVisibility(
                                visible = isSearchHistoryVisible,
                                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(),
                                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
                            ) {
                                SearchHistoryList(
                                    searchHistory = listOf("기록 1", "기록 2"),
                                    onHistoryItemClick = { selectedItem ->
                                        searchText = TextFieldValue(selectedItem)
                                        viewModel.onEvent(MapViewModel.UiEvent.Search(selectedItem)) // 선택한 기록으로 검색
                                        isSearchHistoryVisible = false
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }

                        is MapViewModel.UiState.Searching -> {
                            // 검색 중 로딩 상태 표시
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        is MapViewModel.UiState.SearchResult -> {
                            // 검색 결과를 표시
                            val searchResult = (uiState as MapViewModel.UiState.SearchResult).items
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(),
                                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
                            ) {
                                AddressList(
                                    items = searchResult,
                                    onAddressItemClick = { selectedDocument ->
                                        viewModel.onEvent(
                                            MapViewModel.UiEvent.SelectResult(
                                                selectedDocument
                                            )
                                        ) // 검색된 주소 선택 시 상세 정보 표시
                                    }
                                )
                            }
                        }

                        is MapViewModel.UiState.ShowDetail -> {
                            // 상세 정보만 표시
                            val selectedDocument = (uiState as MapViewModel.UiState.ShowDetail).item
                            LocationDetailBottomSheetScaffold(
                                document = selectedDocument
                            )
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun SearchHistoryList(
        searchHistory: List<String>,
        onHistoryItemClick: (String) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            items(searchHistory) { historyItem ->
                Text(
                    text = historyItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            onHistoryItemClick(historyItem)
                        }
                )
            }
        }
    }

    @Composable
    fun AddressList(
        items: List<Document>,
        onAddressItemClick: (Document) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            items(items) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddressItemClick(item)
                        }
                        .padding(16.dp)
                ) {
                    Text(text = item.place_name ?: "", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "주소: ${item.address_name}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "도로명 주소: ${item.road_address}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                HorizontalDivider()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LocationDetailBottomSheetScaffold(document: Document) {
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Expanded,
                skipHiddenState = false
            )
        )

        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        // BackHandler에서 모달 상태에 따른 처리
        BackHandler(enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            coroutineScope.launch {
                scaffoldState.bottomSheetState.partialExpand()
            }
        }

        val cameraUpdate =
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(
                    document.y,
                    document.x
                )
            )
        kakaoMaps?.moveCamera(cameraUpdate)

        detailLabel = kakaoMaps?.labelManager?.layer?.addLabel(
            LabelOptions.from(
                "dotLabel2", LatLng.from(
                    document.y,
                    document.x
                )
            )
                .setStyles(
                    LabelStyle.from(R.drawable.icon_pin_orange).setAnchorPoint(0.5f, 1f)
                )
                .setRank(1)
        )

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 64.dp,
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    document.place_name?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val intent = Intent(
                            context,
                            NaviActivity::class.java
                        ).apply { putExtra("document_key", document) }
                        (context as? Activity)?.startActivity(intent)
                    }) {
                        Text(text = "길안내")
                    }
                }
            }
        ) {}
    }
}

