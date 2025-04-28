package com.devidea.chevy.ui.map

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import com.devidea.chevy.ui.activity.ui.theme.CarProjectTheme
import com.devidea.chevy.ui.map.MapViewModel.UiState
import com.devidea.chevy.ui.map.compose.CustomFinalizedDemoScreen
import com.devidea.chevy.ui.map.compose.LoadRequestScreen
import com.devidea.chevy.ui.map.compose.MapScreen
import com.devidea.chevy.ui.map.compose.SearchBar
import com.devidea.chevy.ui.navi.KNaviActivity
import com.devidea.chevy.ui.navi.NavigateData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState(UiState.Idle)

            BackHandler {
                when (uiState) {
                    is UiState.ShowDetail -> viewModel.setState(
                        UiState.SearchResult(
                            viewModel.lastSearchResult ?: emptyList()
                        )
                    )

                    is UiState.SearchResult -> viewModel.setState(UiState.Idle)
                    is UiState.DrawRoute -> viewModel.setState(UiState.Idle)
                    is UiState.IsSearching -> viewModel.setState(UiState.Idle)
                    is UiState.Idle -> finish()
                }
            }

            CarProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    when (uiState) {
                        is UiState.DrawRoute -> {
                            MapScreen(viewModel, 0f)
                            val data = (uiState as UiState.DrawRoute).item?.let {
                                NavigateData(
                                    addressName = it.address_name,
                                    goalX = it.x,
                                    goalY = it.y,
                                    startX = viewModel.userLocation.value?.longitude ?: 0.0,
                                    startY = viewModel.userLocation.value?.latitude ?: 0.0,
                                )
                            }
                            LoadRequestScreen(data = data!!, viewModel)
                        }

                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                //MainMapBottomSheet(viewModel, background = { })
                                //a().CustomBottomSheetDemo()
                                CustomFinalizedDemoScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

