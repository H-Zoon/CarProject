package com.devidea.chevy.ui.screen.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.devidea.chevy.viewmodel.MainViewModel
import com.devidea.chevy.viewmodel.MapViewModel
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
                .fillMaxWidth(),
        ) {
            SearchBar(
                buttonVisible = buttonVisible,
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onFocusChanged = { focused ->
                    isSearchHistoryVisible = focused
                },
                onSearch = {
                    viewModel.addSearchQuery(searchText.text)
                    viewModel.onEvent(MapViewModel.UiEvent.Search(searchText.text))
                },
                // 상태 전달
                isSearchResult = uiState is MapViewModel.UiState.SearchResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
            )

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