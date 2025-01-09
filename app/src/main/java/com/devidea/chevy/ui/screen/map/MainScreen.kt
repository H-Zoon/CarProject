package com.devidea.chevy.ui.screen.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.eventbus.KNAVStartEventBus
import com.devidea.chevy.ui.LocationDetailBottomSheet
import com.devidea.chevy.viewmodel.MainViewModel
import com.devidea.chevy.viewmodel.MapViewModel
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import kotlinx.coroutines.launch

@Composable
fun MainScreen(mainViewModel: MainViewModel, viewModel: MapViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState(MapViewModel.UiState.Idle)
    val focusManager = LocalFocusManager.current
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchHistoryVisible by remember { mutableStateOf(false) }
    val buttonVisible = uiState == MapViewModel.UiState.Idle && !isSearchHistoryVisible
    BackHandler {
        when (uiState) {
            is MapViewModel.UiState.ShowDetail -> {
                viewModel.onEvent(MapViewModel.UiEvent.ClearDetail)
            }

            is MapViewModel.UiState.SearchResult -> {
                viewModel.onEvent(MapViewModel.UiEvent.ClearResult)
            }

            else -> {
                if (isSearchHistoryVisible) {
                    isSearchHistoryVisible = false
                    focusManager.clearFocus()
                } else {
                    coroutineScope.launch {
                        mainViewModel.requestNavHost(MainViewModel.NavRoutes.Home)
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        MapScreen(viewModel)

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(), // Row의 크기 변화 애니메이션
                verticalAlignment = Alignment.CenterVertically // 수직 중앙 정렬
            ) {
                // SearchBar에 weight 적용하여 남은 공간을 채우도록 함
                SearchBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onFocusChanged = { focused ->
                        isSearchHistoryVisible = focused
                    },
                    onSearch = {
                        viewModel.addSearchQuery(searchText.text)
                        viewModel.onEvent(MapViewModel.UiEvent.Search(searchText.text))
                    },
                    isSearchResult = uiState is MapViewModel.UiState.SearchResult, // 상태 전달
                    modifier = Modifier
                        .weight(1f) // SearchBar가 가능한 많은 공간을 차지하도록 설정
                        .height(85.dp) // SearchBar의 높이를 고정 (필요 시 조정)
                )

                // 버튼과 검색창 사이의 간격을 AnimatedVisibility로 감싸 애니메이션 처리
                AnimatedVisibility(
                    visible = buttonVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 100)) + slideInHorizontally(
                        initialOffsetX = { -it / 2 }, // 버튼이 왼쪽에서 슬라이드 인
                        animationSpec = tween(durationMillis = 100)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 100)) + slideOutHorizontally(
                        targetOffsetX = { it / 2 }, // 버튼이 오른쪽으로 슬라이드 아웃
                        animationSpec = tween(durationMillis = 100)
                    )
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                }


                // 버튼의 AnimatedVisibility 설정
                AnimatedVisibility(
                    visible = buttonVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 100)) + slideInHorizontally(
                        initialOffsetX = { it / 2 }, // 버튼이 왼쪽에서 슬라이드 인
                        animationSpec = tween(durationMillis = 100)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 100)) + slideOutHorizontally(
                        targetOffsetX = { it / 2 }, // 버튼이 오른쪽으로 슬라이드 아웃
                        animationSpec = tween(durationMillis = 100)
                    )
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
                                val curAvoidOptions =
                                    KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value

                                KNAVStartEventBus.post(
                                    GuidanceStartEvent.RequestNavGuidance(
                                        null,
                                        curRoutePriority,
                                        curAvoidOptions
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .size(48.dp) // 원하는 크기로 조정
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search, // 원하는 아이콘으로 변경
                            contentDescription = "Search"
                        )
                    }
                }
            }


            when (uiState) {
                is MapViewModel.UiState.Idle -> {
                    AnimatedVisibility(
                        visible = isSearchHistoryVisible,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight } // 화면 아래에서 위로 슬라이드 인
                        ),
                        exit = fadeOut() + slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight } // 화면 아래로 슬라이드 아웃
                        )
                    ) {
                        SearchHistoryList(
                            viewModel = viewModel,
                            onHistoryItemClick = { selectedItem ->
                                viewModel.addSearchQuery(selectedItem)
                                searchText = TextFieldValue(selectedItem)
                                viewModel.onEvent(MapViewModel.UiEvent.Search(selectedItem))
                                isSearchHistoryVisible = false
                                focusManager.clearFocus()
                            }
                        )
                    }
                }

                is MapViewModel.UiState.Searching -> {
                    CircularProgressIndicator(modifier = Modifier)
                }

                is MapViewModel.UiState.SearchResult -> {
                    val searchResult = (uiState as MapViewModel.UiState.SearchResult).items
                    AddressList(
                        items = searchResult,
                        onAddressItemClick = { selectedDocument ->
                            viewModel.onEvent(MapViewModel.UiEvent.SelectResult(selectedDocument))
                        }
                    )
                }

                is MapViewModel.UiState.ShowDetail -> {
                    val selectedDocument = (uiState as MapViewModel.UiState.ShowDetail).item
                    LocationDetailBottomSheet(
                        viewModel = viewModel,
                        document = selectedDocument
                    )
                }
            }
        }
    }
}