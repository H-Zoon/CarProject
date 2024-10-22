package com.devidea.chevy.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.viewmodel.MapViewModel

@Composable
fun SearchHistoryList(
    viewModel: MapViewModel,
    onHistoryItemClick: (String) -> Unit
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

    // AlertDialog for removing a single item
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
            .background(Color.White)
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
                        .clickable { onHistoryItemClick(historyItem) },
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
            .background(Color.White)
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
                Text(text = "도로명 주소: ${item.road_address}", style = MaterialTheme.typography.bodySmall)
            }
            Divider()
        }
    }
}