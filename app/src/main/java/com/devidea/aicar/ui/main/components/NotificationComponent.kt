package com.devidea.aicar.ui.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.storage.room.notification.NotificationEntity
import com.devidea.aicar.ui.main.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())

    // UI 상태
    var showMenu by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        // 선택 모드 중엔 돌아가기를 곧바로 호출하거나
                        if (selectionMode) {
                            selectedItems.clear()
                            selectionMode = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (selectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (selectionMode) "Cancel selection" else "Back",
                        )
                    }
                },
                title = {
                    Text(
                        text = if (selectionMode) "${selectedItems.size} selected" else "알림",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark all read") },
                                onClick = {
                                    viewModel.markAllAsRead()
                                    showMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    // 다이얼로그 없이 곧바로 선택 모드로
                                    selectionMode = true
                                    showMenu = false
                                },
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (selectionMode) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            selectedItems.forEach { viewModel.deleteNotification(it) }
                            val count = selectedItems.size
                            selectedItems.clear()
                            selectionMode = false
                            snackbarHostState.showSnackbar("Deleted $count sessions")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .fillMaxSize(),
        ) {
            // ───────── Select All 헤더 ─────────
            if (selectionMode) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val allIds = notifications.map { it.id }
                                    if (selectedItems.size < notifications.size) {
                                        selectedItems.clear()
                                        selectedItems.addAll(allIds)
                                    } else {
                                        selectedItems.clear()
                                    }
                                }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (selectedItems.size < notifications.size) "Select All" else "Deselect All",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    HorizontalDivider()
                }
            }

            items(notifications) { item ->
                SelectableNotificationCard(
                    item = item,
                    selecting = selectionMode,
                    isSelected = selectedItems.contains(item.id),
                    onSelect = { checked ->
                        if (checked) {
                            selectedItems.add(item.id)
                        } else {
                            selectedItems.remove(item.id)
                        }
                    },
                    onClick = {
                        if (!selectionMode) {
                            viewModel.markAsRead(item.id)
                            // TODO: 상세 화면 네비게이션
                        } else {
                            // 선택 모드: 클릭으로도 선택/해제
                            if (selectedItems.contains(item.id)) {
                                selectedItems.remove(item.id)
                            } else {
                                selectedItems.add(item.id)
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SelectableNotificationCard(
    item: NotificationEntity,
    selecting: Boolean,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // 선택 모드가 아닐 때만 일반 클릭 처리
                .then(
                    if (!selecting) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 선택 모드일 때만 체크박스 노출
        if (selecting) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelect,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(item.body, style = MaterialTheme.typography.bodyMedium)
        }

        // 읽지 않은 경우 ‘New’ 배지는 선택 모드가 아닐 때만 보여줌
        if (!item.isRead && !selecting) {
            Badge(modifier = Modifier.padding(start = 8.dp)) {
                Text("New")
            }
        }
    }
}
