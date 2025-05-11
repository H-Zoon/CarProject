package com.devidea.aicar.ui.map.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.ui.map.MapViewModel
import com.devidea.aicar.ui.navi.NavigateData

@Composable
fun MapScreenHost(
    modifier: Modifier = Modifier, viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState(initial = MapViewModel.UiState.Idle)

    BackHandler {
        when (uiState) {
            is MapViewModel.UiState.ShowDetail -> viewModel.setState(
                MapViewModel.UiState.SearchResult(viewModel.lastSearchResult ?: emptyList())
            )

            is MapViewModel.UiState.SearchResult,
            is MapViewModel.UiState.DrawRoute,
            is MapViewModel.UiState.IsSearching -> viewModel.setState(MapViewModel.UiState.Idle)

            is MapViewModel.UiState.Idle -> {}
        }
    }
        when (uiState) {
            is MapViewModel.UiState.DrawRoute -> {
                // 경로 그리기
                MapScreen(viewModel = viewModel)

                val item = (uiState as MapViewModel.UiState.DrawRoute).item
                val data = NavigateData(
                    addressName = item.address_name,
                    goalX = item.x,
                    goalY = item.y,
                    startX = viewModel.userLocation.value?.longitude ?: 0.0,
                    startY = viewModel.userLocation.value?.latitude ?: 0.0,
                )
                LoadRequestScreen(data = data, viewModel = viewModel)
            }

            else -> CustomFinalizedDemoScreen(viewModel = viewModel)

        }
    }