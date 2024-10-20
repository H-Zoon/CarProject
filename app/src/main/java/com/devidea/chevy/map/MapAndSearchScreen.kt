package com.devidea.chevy.map

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.devidea.chevy.ui.LocationDetailBottomSheet
import com.devidea.chevy.viewmodel.MapViewModel

@Composable
fun MapAndSearchScreen(
    viewModel: MapViewModel,
    activity: Activity
) {
    val uiState by viewModel.uiState.observeAsState(MapViewModel.UiState.Idle)
    val focusManager = LocalFocusManager.current
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchHistoryVisible by remember { mutableStateOf(false) }
    val cameraState by viewModel.cameraIsTracking.collectAsState()

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
                    activity.finish()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SearchBar(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onFocusChanged = { focused ->
                    isSearchHistoryVisible = focused
                },
                onSearch = {
                    viewModel.onEvent(MapViewModel.UiEvent.Search(searchText.text))
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                        // bottom 패딩을 제외하려면 따로 처리
                    )
            ) {
                MapScreen(viewModel)

                when (uiState) {
                    is MapViewModel.UiState.Idle -> {
                        FloatingActionButton(
                            onClick = { viewModel.setCameraTracking(!cameraState) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            shape = CircleShape,
                            containerColor = if (cameraState) Color.Blue else Color.Gray
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "내 위치",
                                tint = Color.White
                            )
                        }

                        AnimatedVisibility(
                            visible = isSearchHistoryVisible,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            SearchHistoryList(
                                searchHistory = listOf("기록 1", "기록 2"),
                                onHistoryItemClick = { selectedItem ->
                                    searchText = TextFieldValue(selectedItem)
                                    viewModel.onEvent(MapViewModel.UiEvent.Search(selectedItem))
                                    isSearchHistoryVisible = false
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                    is MapViewModel.UiState.Searching -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                        LocationDetailBottomSheet(document = selectedDocument)
                    }
                }
            }
        }
    )
}