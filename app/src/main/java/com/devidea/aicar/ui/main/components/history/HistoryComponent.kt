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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
    // 탭 상태: 0 = Summary, 1 = Detail
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
            // 1) 탭바
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 2) 탭 컨텐츠
            when (selectedTab) {
                0 -> SessionSummaryScreen(sessionId)
                1 -> SessionDetailScreen(
                    sessionId = sessionId,
                    onBack = { /* 사용 안 함, 뒤로는 위상단백 버튼으로 */ }
                )
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
    // State flows
    val selectedDate by viewModel.selectedDate.collectAsState(initial = LocalDate.now())
    //val selectedDate by viewModel.selectedDate.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val month by viewModel.month.collectAsState()
    val markedDates by viewModel.markedDates.collectAsState()
    var showCalendar by remember { mutableStateOf(false) }

    // Snackbar & dialog states
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var targetSession by remember { mutableStateOf<DrivingSession?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var recentlyDeletedSession by remember { mutableStateOf<DrivingSession?>(null) }
    var recentlyDeletedSessions by remember { mutableStateOf<List<DrivingSession>>(emptyList()) }

    // Date formatters
    val titleFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.title_history)) },
                actions = {
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete all sessions"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedDate?.format(titleFormatter)
                    ?: LocalDate.now().format(titleFormatter),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .height(50.dp)
                    .clickable { showCalendar = true }
            )

            LazyColumn(modifier = Modifier) {
                items(sessions, key = { it.sessionId }) { session ->
                    val start = dateTimeFormatter.format(session.startTime)
                    val end = session.endTime?.let { dateTimeFormatter.format(it) } ?: "--"
                    ListItem(
                        headlineContent = { Text("Session #${session.sessionId}") },
                        supportingContent = { Text("$start ~ $end") },
                        trailingContent = {
                            IconButton(onClick = {
                                targetSession = session
                                showDeleteDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete session"
                                )
                            }
                        },
                        modifier = Modifier
                            .clickable { onSessionClick(session.sessionId) }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Divider()
                }
            }
        }
    }
    // Single delete dialog
    if (showDeleteDialog && targetSession != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session") },
            text = { Text("Are you sure you want to delete session #${targetSession!!.sessionId}? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    recentlyDeletedSession = targetSession
                    viewModel.deleteSession(targetSession!!.sessionId)
                    showDeleteDialog = false
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "삭제되었습니다.",
                            actionLabel = "복구하기"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            recentlyDeletedSession?.let { viewModel.restoreSession(it) }
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete all dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Sessions") },
            text = { Text("Are you sure you want to delete all sessions? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    recentlyDeletedSessions = sessions
                    viewModel.deleteAllSessions()
                    showDeleteAllDialog = false
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "모두 삭제되었습니다.",
                            actionLabel = "복구하기"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreAllSessions(recentlyDeletedSessions)
                        }
                    }
                }) { Text("Delete All") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
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

    // Weekday headers
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
    // Dates grid
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
                            // Highlight selected date
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
                        // Show hint for dates with data
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