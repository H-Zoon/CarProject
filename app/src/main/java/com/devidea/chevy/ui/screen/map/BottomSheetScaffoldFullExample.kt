package com.devidea.chevy.ui.screen.map

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.ui.screen.navi.NavigateData
import com.devidea.chevy.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapBottomSheet(viewModel: MapViewModel, navHostController: NavHostController, background: (Boolean) -> Unit) {
    val cameraState by viewModel.cameraIsTracking.collectAsState()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false
        )
    )
    val viewState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current.screenHeightDp
    val coroutineScope = rememberCoroutineScope()
    val screenHeightPx = configuration * LocalDensity.current.density
    val bottomSheetHeight = (configuration * 0.45f)
    var bottomMarginFraction by remember { mutableStateOf(0) }


    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        background(scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
    }
    LaunchedEffect(scaffoldState.bottomSheetState) {
        snapshotFlow {
            scaffoldState.bottomSheetState.requireOffset()
        }.catch { e ->
            emit(0f)
        }.collect { offset ->
            bottomMarginFraction = round((((screenHeightPx - offset) / screenHeightPx) * 1000).coerceIn(0f, 1000f)).toInt() * -1
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        sheetShape = RoundedCornerShape(0.dp),
        sheetPeekHeight = bottomSheetHeight.dp,
        sheetContent = {
            when (viewState) {
                is MapViewModel.UiState.ShowDetail -> {
                    val selectedDocument = (viewState as MapViewModel.UiState.ShowDetail).item
                    State2(viewModel = viewModel, document = selectedDocument, navHostController = navHostController)
                }
                else -> {
                    State1(coroutineScope, scaffoldState)
                }
            }
        },
        content = { innerPadding ->
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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun State1(coroutineScope: CoroutineScope, scaffoldState: BottomSheetScaffoldState) {
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
                    onClick = { /* 버튼 클릭 이벤트 처리 */ },
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
fun State2(viewModel: MapViewModel, document: Document, navHostController: NavHostController) {
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
                val data = NavigateData(
                    addressName = document.address_name,
                    goalX = document.x,
                    goalY = document.y,
                    startX = viewModel.userLocation.value?.longitude ?: 0.0,
                    startY = viewModel.userLocation.value?.latitude ?: 0.0,
                )
                navHostController.currentBackStackEntry?.savedStateHandle?.set("aaaKey", data)
                navHostController.navigate("findLoad")
            },
        ) {
            Text(text = "길안내")
        }
    }
}
