package com.devidea.chevy.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devidea.chevy.ui.activity.NaviActivity
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.datas.navi.NavigateDocument
import com.devidea.chevy.ui.activity.NaviActivity2
import com.devidea.chevy.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailBottomSheet(viewModel: MapViewModel, document: Document) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = false
        )
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
                    val navigateDocument = NavigateDocument(
                        address_name = document.address_name,
                        goalX = document.x,
                        goalY = document.y,
                        startX = viewModel.userLocation.value?.longitude ?: 0.0,
                        startY = viewModel.userLocation.value?.latitude ?: 0.0
                    )
                    val intent = Intent(context, NaviActivity2::class.java).apply {
                        putExtra("document_key", navigateDocument)
                    }
                    context.startActivity(intent)
                }) {
                    Text(text = "길안내")
                }
            }
        }
    ) {}
}