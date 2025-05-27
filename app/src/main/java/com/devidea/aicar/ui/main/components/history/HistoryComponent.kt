package com.devidea.aicar.ui.main.components.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.devidea.aicar.R
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.devidea.aicar.ui.main.viewmodels.HistoryViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionOverviewScreen(
    sessionId: Long,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("요약", "상세")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session #$sessionId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> SessionSummaryScreen(sessionId)
                1 -> SessionDetailScreen(sessionId = sessionId, onBack = { /* not used */ })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onSessionClick: (Long) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState(initial = LocalDate.now())
    val sessions by viewModel.sessions.collectAsState()
    //val isRecoding by viewModel.ongoingSession.collectAsState()

    val titleFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val dateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    var showCalendar by remember { mutableStateOf(false) }
    val month by viewModel.month.collectAsState()
    val markedDates by viewModel.markedDates.collectAsState()
    var selectionMode by remember { mutableStateOf(false) }
    val selectedSessions = remember { mutableStateListOf<Long>() }
    var recentlyDeletedSessions by remember { mutableStateOf<List<DrivingSession>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = if (selectionMode) {
                    @Composable {
                        IconButton(onClick = {
                            selectedSessions.clear()
                            selectionMode = false
                        }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Cancel selection"
                            )
                        }
                    }
                } else ({}),
                title = {
                    Text(
                        text = if (selectionMode)
                            "${selectedSessions.size} selected"
                        else stringResource(R.string.title_history),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { selectionMode = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete sessions")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (selectionMode) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            // 선택된 세션을 삭제 전 백업
                            recentlyDeletedSessions =
                                sessions.filter { selectedSessions.contains(it.sessionId) }
                            // 삭제 실행
                            selectedSessions.forEach { viewModel.deleteSession(it) }
                            val count = recentlyDeletedSessions.size
                            // 상태 초기화
                            selectedSessions.clear()
                            selectionMode = false

                            // 스낵바로 복원 옵션 제공
                            val result = snackbarHostState.showSnackbar(
                                message = "$count sessions deleted",
                                actionLabel = "Undo"
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // 복원 처리
                                viewModel.restoreAllSessions(recentlyDeletedSessions)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Confirm delete")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedDate.format(titleFormatter),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .height(50.dp)
                    .clickable { showCalendar = true }
            )

            LazyColumn {
                if (selectionMode) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val allIds = sessions.map { it.sessionId }
                                    if (selectedSessions.size < sessions.size) {
                                        selectedSessions.clear()
                                        selectedSessions.addAll(allIds)
                                    } else {
                                        selectedSessions.clear()
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedSessions.size < sessions.size)
                                    "Select All" else "Deselect All",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Divider()
                    }
                }

                items(sessions, key = { it.sessionId }) { session ->
                    SelectableSessionCard(
                        session = session,
                        dateTimeFormatter = dateTimeFormatter,
                        selecting = selectionMode,
                        isSelected = selectedSessions.contains(session.sessionId),
                        onSelect = { checked ->
                            if (checked) selectedSessions.add(session.sessionId)
                            else selectedSessions.remove(session.sessionId)
                        },
                        onClick = {
                            if (!selectionMode) {
                                viewModel.markAsRead(session.sessionId)
                                onSessionClick(session.sessionId)
                            } else {
                                if (selectedSessions.contains(session.sessionId))
                                    selectedSessions.remove(session.sessionId)
                                else
                                    selectedSessions.add(session.sessionId)
                            }
                        }
                    )
                    Divider()
                }
            }
        }

        if (showCalendar) {
            Dialog(onDismissRequest = { showCalendar = false }) {
                Card(modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { viewModel.changeMonth(-1) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
                            }
                            Text(text = "${month.year}.${month.monthValue}")
                            IconButton(onClick = { viewModel.changeMonth(1) }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
                            }
                        }
                        CalendarGrid(
                            month = month,
                            selectedDate = selectedDate!!,
                            markedDates = markedDates,
                            onDateClick = { date ->
                                viewModel.selectDate(date)
                                showCalendar = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableSessionCard(
    session: DrivingSession,
    dateTimeFormatter: DateTimeFormatter,
    selecting: Boolean,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val start = dateTimeFormatter.format(session.startTime)
    val end = session.endTime?.let { dateTimeFormatter.format(it) } ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!selecting) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selecting) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelect,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("Session #${session.sessionId}", style = MaterialTheme.typography.titleMedium)
            Text("$start ~ $end", style = MaterialTheme.typography.bodyMedium)
        }

        if (!session.isRead && !selecting && session.endTime != null) {
            Badge(modifier = Modifier.padding(start = 8.dp)) {
                Text("New")
            }
        }

        if (!selecting && session.endTime == null) {
            Badge(modifier = Modifier.padding(start = 8.dp)) {
                Text("Recoding...")
            }
        }

        if (!selecting) {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Go to detail")
            }
        }
    }
}

@Composable
fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    markedDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7
    val totalCells = startOffset + month.lengthOfMonth()
    val rows = (totalCells + 6) / 7

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
    for (row in 0 until rows) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (col in 0..6) {
                val index = row * 7 + col
                val day = index - startOffset + 1
                if (day in 1..month.lengthOfMonth()) {
                    val date = month.atDay(day)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onDateClick(date) }
                            .then(
                                if (date == selectedDate)
                                    Modifier
                                        .padding(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = day.toString())
                        if (markedDates.contains(date)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(6.dp)
                                    .background(Color.Green, CircleShape)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
