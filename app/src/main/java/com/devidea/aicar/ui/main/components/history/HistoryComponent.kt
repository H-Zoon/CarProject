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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.devidea.aicar.ui.main.viewmodels.HistoryViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// HistoryViewModel을 사용하는 실제 앱의 진입점(Entry-point) 컴포저블
@Composable
fun HistoryRoute(
    // ViewModel은 Hilt를 통해 주입받습니다.
    viewModel: HistoryViewModel = hiltViewModel(),
    // 세션 클릭 시 화면 이동을 위한 콜백 함수
    onSessionClick: (Long) -> Unit,
) {
    // ViewModel에서 StateFlow로 관리되는 상태들을 수집합니다.
    val sessions by viewModel.sessions.collectAsState()
    val markedDates by viewModel.markedDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    // 상태가 없는(stateless) SessionListScreen을 호출합니다.
    // 상태와 이벤트 핸들러(람다)를 모두 ViewModel과 연결해줍니다.
    SessionListScreen(
        sessions = sessions,
        markedDates = markedDates,
        selectedDate = selectedDate,
        onSessionClick = { id ->
            viewModel.markAsRead(id)
            onSessionClick(id)
        },
        onDateSelected = { date -> viewModel.selectDate(date) },
        onMonthChanged = { month -> viewModel.changeMonth(month) },
        onDeleteSessions = { sessionIds ->
            sessionIds.forEach { viewModel.deleteSession(it) }
        },
        onRestoreSessions = { sessionsToRestore ->
            viewModel.restoreAllSessions(sessionsToRestore)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionOverviewScreen(
    sessionId: Long,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("요약", "상세")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Session #$sessionId",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> SessionSummaryRoute(sessionId = sessionId)
                1 -> SessionDetailRoute(sessionId = sessionId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SessionListScreen(
    // viewModel: HistoryViewModel = hiltViewModel(), // 프리뷰에서는 ViewModel을 직접 주입하지 않습니다.
    sessions: List<DrivingSession>, // 프리뷰를 위해 파라미터로 세션 리스트를 받도록 수정
    markedDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onSessionClick: (Long) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (Int) -> Unit,
    onDeleteSessions: (List<Long>) -> Unit,
    onRestoreSessions: (List<DrivingSession>) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    val titleFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val dateTimeFormatter =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())

    var showCalendar by remember { mutableStateOf(false) }
    val month = YearMonth.from(selectedDate) // selectedDate로부터 YearMonth를 계산
    var selectionMode by remember { mutableStateOf(false) }
    val selectedSessions = remember { mutableStateListOf<Long>() }
    var recentlyDeletedSessions by remember { mutableStateOf<List<DrivingSession>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 선택 모드가 비활성화될 때 선택된 목록 초기화
    LaunchedEffect(selectionMode) {
        if (!selectionMode) {
            selectedSessions.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon =
                    if (selectionMode) {
                        @Composable {
                            IconButton(
                                onClick = {
                                    selectedSessions.clear()
                                    selectionMode = false
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Cancel selection",
                                )
                            }
                        }
                    } else {
                        ({})
                    },
                title = {
                    AnimatedContent(
                        targetState =
                            if (selectionMode) {
                                "${selectedSessions.size}개 선택됨"
                            } else {
                                "주행 기록"
                            },
                        transitionSpec = {
                            slideInVertically { -it } + fadeIn() with
                                slideOutVertically { it } + fadeOut()
                        },
                        label = "",
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    if (!selectionMode) {
                        IconButton(
                            onClick = {
                                selectionMode = true
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete sessions")
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectionMode && selectedSessions.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            recentlyDeletedSessions =
                                sessions.filter { selectedSessions.contains(it.sessionId) }
                            onDeleteSessions(selectedSessions.toList()) // 삭제 콜백 호출
                            val count = recentlyDeletedSessions.size
                            selectedSessions.clear()
                            selectionMode = false

                            val result =
                                snackbarHostState.showSnackbar(
                                    message = "$count 개 기록을 삭제했습니다.",
                                    actionLabel = "복구",
                                    duration = SnackbarDuration.Long,
                                )
                            if (result == SnackbarResult.ActionPerformed) {
                                onRestoreSessions(recentlyDeletedSessions)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Confirm delete")
                }
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 개선된 날짜 헤더
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clickable { showCalendar = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedDate.format(titleFormatter),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Open calendar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // 세션 리스트
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 전체 선택/해제 버튼
                if (selectionMode) {
                    item {
                        Card(
                            modifier =
                                Modifier
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
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (selectedSessions.size < sessions.size) {
                                        Icons.Default.CheckBoxOutlineBlank
                                    } else {
                                        Icons.Default.CheckBox
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = if (selectedSessions.size < sessions.size) "모두 선택" else "모두 해제",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                            if (checked) {
                                selectedSessions.add(session.sessionId)
                            } else {
                                selectedSessions.remove(session.sessionId)
                            }
                        },
                        onClick = {
                            if (!selectionMode) {
                                // viewModel.markAsRead(session.sessionId)
                                onSessionClick(session.sessionId)
                            } else {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (selectedSessions.contains(session.sessionId)) {
                                    selectedSessions.remove(session.sessionId)
                                } else {
                                    selectedSessions.add(session.sessionId)
                                }
                            }
                        },
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            IconButton(onClick = { onMonthChanged(-1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                            }
                            Text(
                                text = "${month.year}.${String.format("%02d", month.monthValue)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(onClick = { onMonthChanged(1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ImprovedCalendarGrid(
                            month = month,
                            selectedDate = selectedDate,
                            markedDates = markedDates,
                            onDateClick = { date ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDateSelected(date)
                                showCalendar = false
                            },
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
    onClick: () -> Unit,
) {
    val start = dateTimeFormatter.format(session.startTime)
    val end = session.endTime?.let { dateTimeFormatter.format(it) } ?: "--"
    val isOngoing = session.endTime == null
    val isNew = !session.isRead && session.endTime != null

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (!selecting) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ).animateContentSize(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isOngoing -> MaterialTheme.colorScheme.errorContainer
                        isNew -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
            ),
        border =
            if (isSelected) {
                BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                )
            } else {
                null
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 선택 체크박스 또는 상태 아이콘
            if (selecting) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelect,
                    modifier = Modifier.padding(end = 12.dp),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(
                                when {
                                    isOngoing -> MaterialTheme.colorScheme.error
                                    isNew -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector =
                            when {
                                isOngoing -> Icons.Default.RadioButtonChecked
                                isNew -> Icons.Default.FiberNew
                                else -> Icons.Default.DriveEta
                            },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
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
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$start ~ $end",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                // 상태 정보
                if (isOngoing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "녹화 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
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
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.DriveEta,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "기록된 세션이 없습니다",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "첫 번째 드라이빙 세션을 시작해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ImprovedCalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    markedDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7
    val totalCells = startOffset + month.lengthOfMonth()
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()

    // 요일 헤더
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 날짜 그리드
    for (row in 0 until rows) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
                        modifier =
                            Modifier
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
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color =
                                when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                        )

                        if (hasSession && !isSelected) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = (-2).dp)
                                        .size(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiary,
                                            CircleShape,
                                        ),
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

// --- 제공된 코드 종료 ---

// =================================================================================================
// --- 프리뷰 코드 시작 ---
// =================================================================================================

// 프리뷰에서 사용할 더미 데이터
private val sampleDateTimeFormatter =
    DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

private val sampleSessions =
    listOf(
        DrivingSession(
            101,
            Instant.now().minusSeconds(3600),
            Instant.now().minusSeconds(1800),
            false,
        ), // 새로운 세션
        DrivingSession(
            100,
            Instant.now().minusSeconds(86400),
            Instant.now().minusSeconds(85000),
            true,
        ), // 읽은 세션
        DrivingSession(102, Instant.now(), null, false), // 진행중인 세션
    )

@Preview(showBackground = true, name = "SessionOverviewScreen")
@Composable
fun SessionOverviewScreenPreview() {
    MaterialTheme {
        SessionOverviewScreen(sessionId = 101, onBack = {})
    }
}

@Preview(showBackground = true, name = "SessionListScreen - 데이터 있음")
@Composable
fun SessionListScreenWithDataPreview() {
    var sessions by remember { mutableStateOf(sampleSessions) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val markedDates =
        sessions.map { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()

    MaterialTheme {
        SessionListScreen(
            sessions = sessions,
            markedDates = markedDates,
            selectedDate = selectedDate,
            onSessionClick = {},
            onDateSelected = { selectedDate = it },
            onMonthChanged = { selectedDate = selectedDate.plusMonths(it.toLong()) },
            onDeleteSessions = { idsToDelete ->
                sessions = sessions.filterNot { idsToDelete.contains(it.sessionId) }
            },
            onRestoreSessions = {},
        )
    }
}

@Preview(showBackground = true, name = "SessionListScreen - 데이터 없음")
@Composable
fun SessionListScreenEmptyPreview() {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    MaterialTheme {
        SessionListScreen(
            sessions = emptyList(),
            markedDates = emptySet(),
            selectedDate = selectedDate,
            onSessionClick = {},
            onDateSelected = { selectedDate = it },
            onMonthChanged = { selectedDate = selectedDate.plusMonths(it.toLong()) },
            onDeleteSessions = {},
            onRestoreSessions = {},
        )
    }
}

@Preview(name = "ImprovedSessionCard - 읽음")
@Composable
fun ImprovedSessionCardReadPreview() {
    MaterialTheme {
        Box(Modifier.padding(8.dp)) {
            ImprovedSessionCard(
                session = DrivingSession(1, Instant.now().minusSeconds(3600), Instant.now(), isRead = true),
                dateTimeFormatter = sampleDateTimeFormatter,
                selecting = false,
                isSelected = false,
                onSelect = {},
                onClick = {},
            )
        }
    }
}

@Preview(name = "ImprovedSessionCard - 새로움")
@Composable
fun ImprovedSessionCardNewPreview() {
    MaterialTheme {
        Box(Modifier.padding(8.dp)) {
            ImprovedSessionCard(
                session = DrivingSession(2, Instant.now().minusSeconds(3600), Instant.now(), isRead = false),
                dateTimeFormatter = sampleDateTimeFormatter,
                selecting = false,
                isSelected = false,
                onSelect = {},
                onClick = {},
            )
        }
    }
}

@Preview(name = "ImprovedSessionCard - 진행중")
@Composable
fun ImprovedSessionCardOngoingPreview() {
    MaterialTheme {
        Box(Modifier.padding(8.dp)) {
            ImprovedSessionCard(
                session = DrivingSession(3, Instant.now(), null, isRead = false),
                dateTimeFormatter = sampleDateTimeFormatter,
                selecting = false,
                isSelected = false,
                onSelect = {},
                onClick = {},
            )
        }
    }
}

@Preview(name = "ImprovedSessionCard - 선택됨")
@Composable
fun ImprovedSessionCardSelectedPreview() {
    MaterialTheme {
        Box(Modifier.padding(8.dp)) {
            ImprovedSessionCard(
                session = DrivingSession(4, Instant.now().minusSeconds(3600), Instant.now(), isRead = true),
                dateTimeFormatter = sampleDateTimeFormatter,
                selecting = true,
                isSelected = true,
                onSelect = {},
                onClick = {},
            )
        }
    }
}

@Preview(name = "EmptyStateCard")
@Composable
fun EmptyStateCardPreview() {
    MaterialTheme {
        Box(Modifier.padding(8.dp)) {
            EmptyStateCard()
        }
    }
}

@Preview(showBackground = true, name = "ImprovedCalendarGrid")
@Composable
fun ImprovedCalendarGridPreview() {
    val today = LocalDate.now()
    val markedDates = setOf(today.minusDays(3), today.minusDays(5), today.plusDays(2))
    MaterialTheme {
        Surface {
            Column(Modifier.padding(16.dp)) {
                ImprovedCalendarGrid(
                    month = YearMonth.from(today),
                    selectedDate = today,
                    markedDates = markedDates,
                    onDateClick = {},
                )
            }
        }
    }
}
