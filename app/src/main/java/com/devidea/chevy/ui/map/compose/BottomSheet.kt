package com.devidea.chevy.ui.map.compose

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.ui.map.MapViewModel
import com.devidea.chevy.ui.navi.KNaviActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.math.round


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapBottomSheet(viewModel: MapViewModel, background: (Boolean) -> Unit) {
    val cameraState by viewModel.cameraIsTracking.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false
        )
    )
    val viewState by viewModel.uiState.collectAsState()
    // 바텀시트를 고정시켜야 하는 상태인지 여부
    val sheetFixed = remember(viewState) {
        viewState is MapViewModel.UiState.IsSearching || viewState is MapViewModel.UiState.SearchResult
    }

    val coroutineScope = rememberCoroutineScope()

    var bottomMarginFraction by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // 1) 화면 전체 높이를 dp 단위로 가져오기
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val density = LocalDensity.current

    // 2) SearchBar 높이를 px → dp로 변환
    var searchBarHeightPx by remember { mutableStateOf(0) }
    val searchBarHeightDp = with(density) { searchBarHeightPx.toDp() }

    // 3) peekHeight(접힌 상태)와 expandedHeight(확장 최대 높이) 계산
    val peekHeight = 200.dp
    val sheetMaxHeight = screenHeightDp.dp - 100.dp

    // 바텀시트 상승시 내부 컨텐트 이동을 위한 변수
    val screenHeightPx = screenHeightDp * density.density

    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        background(scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
    }
    LaunchedEffect(scaffoldState.bottomSheetState) {
        snapshotFlow {
            scaffoldState.bottomSheetState.requireOffset()
        }.catch { e ->
            emit(0f)
        }.collect { offset ->
            bottomMarginFraction = round(
                (((screenHeightPx - offset) / screenHeightPx) * 1000).coerceIn(
                    0f,
                    1000f
                )
            ).toInt() * -1
        }
    }
    LaunchedEffect(sheetFixed) {
        if (sheetFixed) scaffoldState.bottomSheetState.expand() else scaffoldState.bottomSheetState.show()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        sheetShape = RoundedCornerShape(0.dp),
        sheetPeekHeight = peekHeight,
        sheetSwipeEnabled = !sheetFixed,
        sheetDragHandle = {
            if (!sheetFixed) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier
                        //.align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp)
                )
            }
        },
        sheetContent = {
            Box(modifier = Modifier.height(sheetMaxHeight)) {
                when (viewState) {
                    is MapViewModel.UiState.Idle -> {
                        MainSheet(coroutineScope, scaffoldState)
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
        content = { innerPadding ->
            Box(
                modifier = Modifier
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, bottomMarginFraction) }
                ) {
                    MapScreen(viewModel)

                    FloatingActionButton(
                        onClick = { viewModel.setCameraTracking(!cameraState) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .offset { IntOffset(0, bottomMarginFraction - 100) },
                        shape = CircleShape,
                        containerColor = if (cameraState) Color(0xFFff722b) else Color.Gray
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "내 위치",
                            tint = Color.White
                        )
                    }

                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Text("바텀시트 펼치기")
                        }
                    }
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
                    drawBackground = sheetFixed,
                    viewModel = viewModel
                )
            }
        }
    )
}

@Composable
fun HistorySheet(viewModel: MapViewModel) {
    SearchHistoryList(
        viewModel = viewModel,
        onHistoryItemClick = {
            //히스토리 저장
            viewModel.addSearchQuery(it)
            //검색요청
            viewModel.onEvent(MapViewModel.UiEvent.RequestSearch(it))
        }
    )
}

@Composable
fun ResultSheet(list: List<Document>, itemClick: (Document) -> Unit) {
    AddressList(
        items = list,
        onAddressItemClick = { selectedDocument ->
            itemClick(selectedDocument)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSheet(coroutineScope: CoroutineScope, scaffoldState: BottomSheetScaffoldState) {
    val dynamicButtonData = listOf("버튼1", "버튼2", "버튼3", "버튼4", "버튼5", "버튼6", "버튼7")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                }
            ) {
                Text(text = "상단 버튼 1")
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                    }
                }
            ) {
                Text(text = "상단 버튼 2")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(dynamicButtonData) { item ->
                Button(
                    onClick = {},
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = item)
                }
            }
        }
    }
}

@Composable
fun DetailSheet(viewModel: MapViewModel, document: Document) {
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

        Button(
            onClick = {
                viewModel.setState(MapViewModel.UiState.DrawRoute(document))
            },
        ) {
            Text(text = "길안내")
        }
    }
}

