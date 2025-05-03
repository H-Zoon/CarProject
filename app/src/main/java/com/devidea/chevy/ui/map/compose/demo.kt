package com.devidea.chevy.ui.map.compose

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.network.reomote.Document
import com.devidea.chevy.ui.map.MapViewModel
import com.devidea.chevy.ui.navi.KNaviActivity
import io.morfly.compose.bottomsheet.material3.BottomSheetScaffold
import io.morfly.compose.bottomsheet.material3.rememberBottomSheetScaffoldState
import io.morfly.compose.bottomsheet.material3.rememberBottomSheetState
import io.morfly.compose.bottomsheet.material3.requireSheetVisibleHeightDp
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.devidea.chevy.storage.DocumentTag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomFinalizedDemoScreen(viewModel: MapViewModel = hiltViewModel()) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val cameraState by viewModel.cameraIsTracking.collectAsState()
    val viewState by viewModel.uiState.collectAsState()

    val sheetFixed by remember(viewState) {
        mutableStateOf(
            viewState is MapViewModel.UiState.IsSearching ||
                    viewState is MapViewModel.UiState.SearchResult
        )
    }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        defineValues = {
            if (!(viewState is MapViewModel.UiState.IsSearching ||
                        viewState is MapViewModel.UiState.SearchResult)
            ) {
                SheetValue.Collapsed at offset(percent = 90)
                SheetValue.PartiallyExpanded at offset(percent = 60)
            }
            SheetValue.Expanded at height(configuration.screenHeightDp.dp - 150.dp)
        },
        confirmValueChange = { new ->
            !((viewState is MapViewModel.UiState.IsSearching || viewState is MapViewModel.UiState.SearchResult) && new != SheetValue.Expanded)
        }
    )

    LaunchedEffect(sheetFixed) {
        if (sheetFixed) {
            sheetState.animateTo(SheetValue.Expanded)
        } else {
            sheetState.animateTo(SheetValue.PartiallyExpanded)
        }
        sheetState.refreshValues()
    }


    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetShape = if (sheetState.currentValue == SheetValue.Expanded) RoundedCornerShape(0.dp) else RoundedCornerShape(
            20.dp
        ),
        sheetContainerColor = MaterialTheme.colorScheme.onPrimary,
        sheetSwipeEnabled = !(
                viewState is MapViewModel.UiState.IsSearching ||
                        viewState is MapViewModel.UiState.SearchResult
                ),
        sheetDragHandle = {
            if (!sheetFixed) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                )
            }
        },
        topBar = {
            SearchBar(
                modifier = Modifier,
                onSearch = {
                    viewModel.addSearchQuery(it)
                    viewModel.onEvent(MapViewModel.UiEvent.RequestSearch(it))
                },
                onSafety = {
                    val intent = Intent(context, KNaviActivity::class.java)
                    context.startActivity(intent)
                },
                viewModel = viewModel,
                isSheetExpanded = sheetState.currentValue == SheetValue.Expanded
            )
        },
        sheetContent = {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (viewState) {
                    is MapViewModel.UiState.Idle -> {
                        MainSheet()
                    }

                    is MapViewModel.UiState.IsSearching -> {
                        SearchHistoryList(viewModel = viewModel)
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
            val density = LocalDensity.current

            // 전체 화면 높이 (Dp, Px 둘 다 준비)
            val screenHeightDp = configuration.screenHeightDp.dp
            val screenHeightPx = with(density) { screenHeightDp.toPx() }


            val bottomPadding by remember {
                derivedStateOf { sheetState.requireSheetVisibleHeightDp() }
            }

            val sheetTopPx by remember {
                derivedStateOf { sheetState.requireOffset() }
            }

            val fabVisible by remember {
                derivedStateOf {
                    sheetTopPx > screenHeightPx * 0.2f
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()

            ) {
                MapScreen(viewModel, bottomPadding)
                if (fabVisible) {
                    FloatingActionButton(
                        onClick = { viewModel.setCameraTracking(!cameraState) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (sheetTopPx - 75.dp.roundToPx()).toInt()
                                )
                            },
                        shape = CircleShape,
                        containerColor = if (cameraState) Color(0xFFff722b) else Color.Gray
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "내 위치",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
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

@Composable
fun MainSheet() {
    val coroutineScope = rememberCoroutineScope()
    // 하단에 표시할 임의 요소 7개
    val bottomItems = (1..7).map { "항목 $it" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 상단 버튼 1: 집 아이콘, 라운드 16
            Button(
                onClick = {
                    coroutineScope.launch {
                        // TODO: 집 버튼 동작 처리
                    }
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "집"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "집")
            }
            // 상단 버튼 2: 회사 아이콘, 라운드 16
            Button(
                onClick = {
                    coroutineScope.launch {
                        // TODO: 회사 버튼 동작 처리
                    }
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "회사"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "회사")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 하단 버튼 7개를 레이지 그리드로 표시
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(bottomItems) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .clickable { /* TODO: 클릭 처리 for $item */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item)
                }
            }
        }
    }
}

@Composable
fun DetailSheet(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    document: Document
) {
    val context = LocalContext.current

    val isFav by viewModel.isFavorite(document.id).collectAsState(initial = false)
    val tag by viewModel.getTag(document.id).collectAsState(initial = null)

    var isDialogOpen by remember { mutableStateOf(false) }
    var selectedType by remember(tag) { mutableStateOf(tag) }
    var isCustomFavorite by remember(isFav) { mutableStateOf(isFav) }

    Column(modifier = modifier.padding(16.dp)) {
        // 제목과 즐겨찾기 아이콘을 한 줄에 배치
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = document.place_name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { isDialogOpen = true }
            ) {
                if (isFav || tag != null) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "즐겨찾기 해제",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "즐겨찾기"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${document.category_group_name} • ${document.category_name}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = document.road_address_name.ifBlank { document.address_name },
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (document.phone.isNotBlank()) {
            Text(
                text = "☎ ${document.phone}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        document.distance?.let {
            Text(
                text = "거리: ${it}m",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(16.dp))

        document.place_url?.let {
            Text(
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, document.place_url.toUri())
                    context.startActivity(intent)
                },
                text = "홈페이지: ${it}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.setState(MapViewModel.UiState.DrawRoute(document)) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("길찾기")
        }
    }

    // 즐겨찾기 옵션 다이얼로그
    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text("즐겨찾기 설정") },
            text = {
                Column {
                    Text("분류를 선택하세요 (하나만):")
                    Spacer(Modifier.height(8.dp))
                    // 집 / 회사 라디오 그룹
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == DocumentTag.HOME,
                            onClick = {
                                if(selectedType == DocumentTag.HOME) selectedType = null else selectedType = DocumentTag.HOME
                            }
                        )
                        Text("집", modifier = Modifier.clickable { selectedType = DocumentTag.HOME })
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = selectedType == DocumentTag.OFFICE,
                            onClick = {
                                if(selectedType == DocumentTag.OFFICE) selectedType = null else selectedType = DocumentTag.OFFICE
                            }
                        )
                        Text("회사", modifier = Modifier.clickable { selectedType = DocumentTag.OFFICE })
                    }
                    Spacer(Modifier.height(12.dp))
                    // 추가 옵션 체크박스
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isCustomFavorite,
                            onCheckedChange = {
                                isCustomFavorite = it
                            }
                        )
                        Text("즐겨찾기 지역")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedType != tag) {
                            viewModel.updateTagFromNetwork(document = document, tag = selectedType)
                        }
                        if(isCustomFavorite != isFav) {
                            viewModel.updateFavoriteFromNetwork(document = document, isFav = isCustomFavorite)
                        }

                        isDialogOpen = false
                        selectedType = null
                        isCustomFavorite = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isDialogOpen = false
                    selectedType = null
                    isCustomFavorite = false
                }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun SearchHistoryList(
    viewModel: MapViewModel
) {
    val searchHistory by viewModel.searchHistory.collectAsState()
    // 상태 변수: 전체 삭제 다이얼로그 표시 여부
    var showClearAllDialog by remember { mutableStateOf(false) }

    // 상태 변수: 개별 삭제 다이얼로그 표시 여부 및 삭제할 항목
    var showRemoveItemDialog by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<String?>(null) }

    // AlertDialog for clearing all history
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(text = "확인") },
            text = { Text(text = "검색 기록을 모두 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSearchHistory()
                    showClearAllDialog = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showRemoveItemDialog && itemToRemove != null) {
        AlertDialog(
            onDismissRequest = {
                showRemoveItemDialog = false
                itemToRemove = null
            },
            title = { Text(text = "확인") },
            text = { Text(text = "이 검색 기록을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSearchQuery(itemToRemove!!)
                    showRemoveItemDialog = false
                    itemToRemove = null
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRemoveItemDialog = false
                    itemToRemove = null
                }) {
                    Text("취소")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp)
    ) {
        // 헤더 추가: 검색 기록 레이블 및 전체 삭제 버튼
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .padding(end = 20.dp)
            ) {
                Text(
                    text = "검색기록 삭제",
                    modifier = Modifier
                        .align(Alignment.TopEnd)  // 우상단에 배치
                        .clickable {
                            showClearAllDialog = true
                        },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        // 검색 히스토리 리스트
        items(searchHistory) { historyItem ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = historyItem,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.addSearchQuery(historyItem)
                            viewModel.onEvent(MapViewModel.UiEvent.RequestSearch(historyItem))
                        },
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(
                    onClick = {
                        itemToRemove = historyItem
                        showRemoveItemDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "검색 기록 삭제",
                        tint = Color.Gray
                    )
                }
            }
            Divider(color = Color.LightGray, thickness = 1.dp)
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
    ) {
        items(items.size) { index ->
            val item = items[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddressItemClick(item) }
                    .padding(16.dp)
            ) {
                Text(text = item.place_name ?: "", style = MaterialTheme.typography.titleMedium)
                Text(text = "주소: ${item.address_name}", style = MaterialTheme.typography.bodySmall)
                //Text(text = "도로명 주소: ${item.road_address}", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }
    }
}


