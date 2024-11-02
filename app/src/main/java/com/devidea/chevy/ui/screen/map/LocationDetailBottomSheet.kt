package com.devidea.chevy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.datas.navi.NavigateDocument
import com.devidea.chevy.ui.screen.navi.NaviScreen
import com.devidea.chevy.viewmodel.MainViewModel
import com.devidea.chevy.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailBottomSheet(viewModel: MapViewModel, document: Document, mainViewModel: MainViewModel) {
    var showNaviScreen by remember { mutableStateOf(false) }
    var navigateDocument by remember { mutableStateOf<NavigateDocument?>(null) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = false
        )
    )
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 64.dp,
        sheetContent = {
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

                Button(onClick = {
                    mainViewModel.foundRoot(
                        addressName = document.address_name,
                        goalX = document.x,
                        goalY = document.y,
                        startX = viewModel.userLocation.value?.longitude ?: 0.0,
                        startY = viewModel.userLocation.value?.latitude ?: 0.0
                    ) {}
                }) {
                    Text(text = "길안내")
                }
            }
        }
    ) {}
}