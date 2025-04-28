package com.devidea.chevy.ui.map.compose

import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.devidea.chevy.ui.map.MapViewModel
import com.devidea.chevy.ui.navi.KNaviActivity
import io.morfly.compose.bottomsheet.material3.BottomSheetScaffold
import io.morfly.compose.bottomsheet.material3.rememberBottomSheetScaffoldState
import io.morfly.compose.bottomsheet.material3.rememberBottomSheetState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomFinalizedDemoScreen(viewModel: MapViewModel) {
    val context = LocalContext.current
    val cameraState by viewModel.cameraIsTracking.collectAsState()
    val viewState by viewModel.uiState.collectAsState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    var searchBarHeightPx by remember { mutableStateOf(0) }
    val searchBarHeightDp = with(LocalDensity.current) { searchBarHeightPx.toDp() }
    // 바텀시트를 고정시켜야 하는 상태인지 여부
    val sheetFixed by remember(viewState) {
        mutableStateOf(
            viewState is MapViewModel.UiState.IsSearching ||
                    viewState is MapViewModel.UiState.SearchResult
        )
    }


    // ▼ 0) 투명도 변화가 시작·끝나는 위치(dp)
    val fadeStartPx = 1200f      // 시트 상단이 120dp 지점부터
    val fadeEndPx = 600f       // 60dp 지점에 도달하면 α = 0

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        defineValues = {
            if (!sheetFixed) {
                SheetValue.Collapsed at height(100.dp)
                SheetValue.PartiallyExpanded at offset(percent = 60)
            }
            SheetValue.Expanded at contentHeight
        },
        // ⚠ 여기서는 “고정 중엔 Expanded 아닌 값으로 못 가게”만 막는다
        confirmValueChange = { new ->
            !(sheetFixed && new != SheetValue.Expanded)
        }
    )

    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)

    // 2) sheetFixed 가 바뀔 때 한 번만 앵커 갱신
    LaunchedEffect(sheetFixed) {
        if (sheetFixed) {
            sheetState.animateTo(SheetValue.Expanded) // ① 먼저 이동
        }
        // ② 이동이 끝난 뒤 새 집합으로 교체
        sheetState.refreshValues()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = !sheetFixed,
        sheetContent = {
            Box(modifier = Modifier.fillMaxSize()
            ) {
                when (viewState) {
                    is MapViewModel.UiState.Idle -> {
                        MainSheet()
                    }

                    is MapViewModel.UiState.IsSearching -> {
                        HistorySheet(viewModel = viewModel)
                    }

                    is MapViewModel.UiState.SearchResult -> {
                        val searchResult = (viewState as MapViewModel.UiState.SearchResult).items
                        ResultSheet(searchResult) {
                            viewModel.onEvent(
                                MapViewModel.UiEvent.SelectItem(it)
                            )
                        }
                    }

                    is MapViewModel.UiState.ShowDetail -> {
                        val selectedDocument = (viewState as MapViewModel.UiState.ShowDetail).item
                        DetailSheet(viewModel = viewModel, document = selectedDocument)
                    }

                    is MapViewModel.UiState.DrawRoute -> {}
                }
            }
        },
        content = {
            val bottomPadding by remember {
                derivedStateOf { sheetState.requireSheetVisibleHeight() }
            }
            val isBottomSheetMoving by remember {
                derivedStateOf { sheetState.currentValue != sheetState.targetValue }
            }
            val sheetTopPx by remember {
                derivedStateOf { sheetState.requireOffset() }
            }
            LaunchedEffect(sheetTopPx) { Log.d("?", sheetTopPx.toString()) }
            val rawProgress = (fadeStartPx - sheetTopPx) / (fadeStartPx - fadeEndPx)
            val clamped = rawProgress.coerceIn(0f, 1f)
            val targetAlpha = 1f - clamped          // 시트가 올라올수록 0으로

            val animatedAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                label = "FabAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()

            ) {
                MapScreen(viewModel, bottomPadding)

                FloatingActionButton(
                    onClick = { viewModel.setCameraTracking(!cameraState) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (sheetTopPx - 75.dp.roundToPx()).toInt()   // FAB 높이 56dp 가정
                            )
                        }
                        .graphicsLayer { alpha = animatedAlpha },
                    shape = CircleShape,
                    containerColor = if (cameraState) Color(0xFFff722b) else Color.Gray
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "내 위치",
                        tint = Color.White
                    )
                }
                SearchBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .onGloballyPositioned { coords ->
                            searchBarHeightPx = coords.size.height
                        },
                    onSearch = {
                        viewModel.addSearchQuery(it)
                        viewModel.onEvent(MapViewModel.UiEvent.RequestSearch(it))
                    },
                    onSafety = {
                        val intent =
                            Intent(context, KNaviActivity::class.java)
                        context.startActivity(intent)
                    },
                    scaffoldState = sheetState,
                    viewModel = viewModel
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}