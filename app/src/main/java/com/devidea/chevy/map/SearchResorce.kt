package com.devidea.chevy.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devidea.chevy.response.Document

@Composable
fun SearchHistoryList(
    searchHistory: List<String>,
    onHistoryItemClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        items(searchHistory.size) { index ->
            val historyItem = searchHistory[index]
            Text(
                text = historyItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onHistoryItemClick(historyItem) }
            )
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