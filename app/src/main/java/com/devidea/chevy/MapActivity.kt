package com.devidea.chevy

import android.os.Bundle
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.devidea.chevy.response.Document
import com.devidea.chevy.viewmodel.MapViewModel
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SearchHistoryApp(viewModel)
        }
    }
}

@Composable
fun MapScreen() {
    val mapView = rememberMapViewWithLifecycle()
    AndroidView({ mapView }) { mapView ->

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
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        // 인증 후 API 가 정상적으로 실행될 때 호출됨
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
    val uiState by viewModel.uiState.observeAsState(MapViewModel.UiState.Idle)
    val focusManager = LocalFocusManager.current
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchHistoryVisible by remember { mutableStateOf(false) }
    var isSearchButtonVisible by remember { mutableStateOf(true) }

    // BackHandler 처리
    BackHandler {
        when (uiState) {
            is MapViewModel.UiState.ShowDetail -> {
                viewModel.onEvent(MapViewModel.UiEvent.ClearDetail)

            }
            is MapViewModel.UiState.SearchResult -> {
                viewModel.onEvent(MapViewModel.UiEvent.ClearResult)
            }
            else -> {
                // Idle 상태이거나 다른 상태에서는 기본 뒤로가기 동작 (앱 종료 또는 다른 처리)
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        .onFocusChanged { focusState ->
                            isSearchHistoryVisible = focusState.isFocused
                            isSearchButtonVisible = focusState.isFocused
                            /*if (!focusState.isFocused && searchText.text.isEmpty()) {
                                viewModel.onEvent(MapViewModel.UiEvent.ClearResult)
                            }*/
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
                // 상태에 따른 UI 처리
                when (uiState) {
                    is MapViewModel.UiState.Idle -> {
                        // Idle 상태이고 텍스트 필드가 포커싱되지 않으면 기본 화면
                        if (!isSearchHistoryVisible) {
                            MapScreen()
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
                                    viewModel.onEvent(MapViewModel.UiEvent.SelectResult(selectedDocument)) // 검색된 주소 선택 시 상세 정보 표시
                                }
                            )
                        }
                    }
                    is MapViewModel.UiState.ShowDetail -> {
                        // 상세 정보만 표시
                        val selectedDocument = (uiState as MapViewModel.UiState.ShowDetail).item
                        AddressDetail(document = selectedDocument)
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
                Text(text = "주소: ${item.address_name}", style = MaterialTheme.typography.labelSmall)
                Text(text = "도로명 주소: ${item.road_address}", style = MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressDetail(document: Document) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(true) }

    // BackHandler에서 모달 상태에 따른 처리
    BackHandler(enabled = isSheetOpen) {
        coroutineScope.launch {
            bottomSheetState.hide()
            isSheetOpen = false
        }
    }

    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    bottomSheetState.hide()
                    isSheetOpen = false
                }
            },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                document.place_name?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                        isSheetOpen = false
                    }
                }) {
                    Text(text = "닫기")
                }
            }
        }
    }
}

