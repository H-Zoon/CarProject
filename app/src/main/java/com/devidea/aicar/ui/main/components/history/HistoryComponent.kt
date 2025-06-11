package com.devidea.aicar.ui.main.components.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                title = {
                    Text(
                        "Session #$sessionId",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SessionListScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onSessionClick: (Long) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState(initial = LocalDate.now())
    val sessions by viewModel.sessions.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current

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
                        IconButton(
                            onClick = {
                                selectedSessions.clear()
                                selectionMode = false
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Cancel selection"
                            )
                        }
                    }
                } else ({}),
                title = {
                    AnimatedContent(
                        targetState = if (selectionMode) "${selectedSessions.size} selected"
                        else stringResource(R.string.title_history),
                        transitionSpec = {
                            slideInVertically { -it } + fadeIn() with
                                    slideOutVertically { it } + fadeOut()
                        }
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (!selectionMode) {
                        IconButton(
                            onClick = {
                                selectionMode = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete sessions")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectionMode && selectedSessions.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            recentlyDeletedSessions = sessions.filter { selectedSessions.contains(it.sessionId) }
                            selectedSessions.forEach { viewModel.deleteSession(it) }
                            val count = recentlyDeletedSessions.size
                            selectedSessions.clear()
                            selectionMode = false

                            val result = snackbarHostState.showSnackbar(
                                message = "$count 개 기록을 삭제했습니다.",
                                actionLabel = "복구",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreAllSessions(recentlyDeletedSessions)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Confirm delete")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 개선된 날짜 헤더
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable { showCalendar = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(titleFormatter),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Open calendar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 세션 리스트
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 전체 선택/해제 버튼
                if (selectionMode) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val allIds = sessions.map { it.sessionId }
                                    if (selectedSessions.size < sessions.size) {
                                        selectedSessions.clear()
                                        selectedSessions.addAll(allIds)
                                    } else {
                                        selectedSessions.clear()
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (selectedSessions.size < sessions.size) Icons.Default.CheckBoxOutlineBlank
                                    else Icons.Default.CheckBox,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (selectedSessions.size < sessions.size) "모두 선택" else "모두 해제",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // 세션 카드들
                items(sessions, key = { it.sessionId }) { session ->
                    ImprovedSessionCard(
                        session = session,
                        dateTimeFormatter = dateTimeFormatter,
                        selecting = selectionMode,
                        isSelected = selectedSessions.contains(session.sessionId),
                        onSelect = { checked ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (checked) selectedSessions.add(session.sessionId)
                            else selectedSessions.remove(session.sessionId)
                        },
                        onClick = {
                            if (!selectionMode) {
                                viewModel.markAsRead(session.sessionId)
                                onSessionClick(session.sessionId)
                            } else {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (selectedSessions.contains(session.sessionId))
                                    selectedSessions.remove(session.sessionId)
                                else
                                    selectedSessions.add(session.sessionId)
                            }
                        }
                    )
                }

                // 빈 상태 표시
                if (sessions.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                }
            }
        }

        // 개선된 캘린더 다이얼로그
        if (showCalendar) {
            Dialog(onDismissRequest = { showCalendar = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { viewModel.changeMonth(-1) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous month")
                            }
                            Text(
                                text = "${month.year}.${String.format("%02d", month.monthValue)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.changeMonth(1) }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next month")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ImprovedCalendarGrid(
                            month = month,
                            selectedDate = selectedDate,
                            markedDates = markedDates,
                            onDateClick = { date ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
fun ImprovedSessionCard(
    session: DrivingSession,
    dateTimeFormatter: DateTimeFormatter,
    selecting: Boolean,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val start = dateTimeFormatter.format(session.startTime)
    val end = session.endTime?.let { dateTimeFormatter.format(it) } ?: "--"
    val isOngoing = session.endTime == null
    val isNew = !session.isRead && !selecting && session.endTime != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!selecting) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isOngoing -> MaterialTheme.colorScheme.errorContainer
                isNew -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 선택 체크박스 또는 상태 아이콘
            if (selecting) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelect,
                    modifier = Modifier.padding(end = 12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            when {
                                isOngoing -> MaterialTheme.colorScheme.error
                                isNew -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isOngoing -> Icons.Default.RadioButtonChecked
                            isNew -> Icons.Default.FiberNew
                            else -> Icons.Default.DriveEta
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 세션 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Session #${session.sessionId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$start ~ $end",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // 상태 정보
                if (isOngoing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "녹화 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 우측 액션 버튼
            if (!selecting) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Go to detail",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.DriveEta,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "기록된 세션이 없습니다",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "첫 번째 드라이빙 세션을 시작해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ImprovedCalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    markedDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7
    val totalCells = startOffset + month.lengthOfMonth()
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()

    // 요일 헤더
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 날짜 그리드
    for (row in 0 until rows) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (col in 0..6) {
                val index = row * 7 + col
                val day = index - startOffset + 1

                if (day in 1..month.lengthOfMonth()) {
                    val date = month.atDay(day)
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val hasSession = markedDates.contains(date)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .clickable { onDateClick(date) }
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (hasSession && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = (-2).dp)
                                    .size(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary,
                                        CircleShape
                                    )
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
