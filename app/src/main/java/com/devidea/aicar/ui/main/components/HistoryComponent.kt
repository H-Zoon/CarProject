package com.devidea.aicar.ui.main.components


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.devidea.aicar.ui.main.MainViewModel
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryNavGraph(
    navController: NavHostController,
    onBackToList: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "sessionList"
    ) {
        // 1) 리스트 화면
        composable("sessionList") {
            SessionListScreen(
                onSessionClick = { sessionId ->
                    navController.navigate("sessionDetail/$sessionId")
                }
            )
        }
        // 2) 상세 화면
        composable(
            route = "sessionDetail/{sessionId}",
            arguments = listOf(navArgument("sessionId") {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments!!.getLong("sessionId")
            SessionDetailScreen(
                sessionId = sessionId,
                viewModel = hiltViewModel(), // 기본 인자
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    viewModel: MainViewModel = hiltViewModel(),
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
                    Text(
                        text = selectedDate?.format(titleFormatter)
                            ?: LocalDate.now().format(titleFormatter),
                        modifier = Modifier.clickable { showCalendar = true }
                    )
                },
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(vertical = 8.dp)
        ) {
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
        listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { day ->
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val dataPoints by viewModel.getSessionData(sessionId).collectAsState(initial = emptyList())

    val speedSeries = dataPoints.map { it.speed.toInt() }
    val rpmSeries = dataPoints.map { it.rpm.toInt() }
    val tempSeries = dataPoints.map { it.engineTemp.toInt() }
    val instantKPLSeries = dataPoints.map { it.instantKPL.toFloat() }
    val path = remember(dataPoints) {
        dataPoints.map { LatLng(it.latitude, it.longitude) }
    }

    val sliderPosition by viewModel.sliderPosition.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Auto-play effect: advance slider while playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (viewModel.isPlaying.value) {
                val next =
                    (viewModel.sliderPosition.value + 1).coerceAtMost((dataPoints.lastIndex).toFloat())
                viewModel.updateSlider(next)
                if (viewModel.sliderPosition.value >= dataPoints.lastIndex) {
                    viewModel.togglePlay()
                    break
                }
                delay(100L)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .height(300.dp)
            .shadow(elevation = 10.dp)) {
            SessionTrackMap(
                path = path,
                sliderPosition = sliderPosition,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SpeedLineChart(
                    data = speedSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            item {
                RpmLineChart(
                    data = rpmSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            item {
                TempLineChart(
                    data = tempSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            item {
                InstantKPLChart(
                    data = instantKPLSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = { viewModel.updateSlider(it) },
                valueRange = 0f..(dataPoints.lastIndex.coerceAtLeast(0)).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )
            PlayControlRow(isPlaying = isPlaying, onPlayPause = viewModel::togglePlay)
        }
    }
}

@Composable
private fun LineChart(
    data: List<Int>,
    lineColor: Color,
    title: String,
    unit: String,
    markerPosition: Int? = null,
    modifier: Modifier = Modifier,
    gridLineCount: Int = 4,
    gridColor: Color = Color.LightGray,
    gridStrokeWidth: Dp = 1.dp
) {
    if (data.isEmpty()) return
    val density = LocalDensity.current
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = density.run { 12.dp.toPx() }
            textAlign = Paint.Align.RIGHT
        }
    }

    // Column으로 단위 영역과 차트 영역 분리
    Column(modifier = modifier) {
        // 1) 차트 외부에 단위 표시
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black
            )
        }

        // 2) 차트 그리기
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            val leftMargin = with(density) { 40.dp.toPx() }
            val textHeight = paint.fontMetrics.run { descent - ascent }
            val topOffset = textHeight
            val bottomOffset = textHeight
            val plotHeight = size.height - topOffset - bottomOffset
            val w = size.width - leftMargin

            val maxVal = data.maxOrNull() ?: 0
            val minVal = data.minOrNull() ?: 0
            val range = (maxVal - minVal).toFloat()
            if (range <= 0f) return@Canvas

            val spacing = w / (data.size - 1).coerceAtLeast(1)

            // 그리드 및 축 라인
            for (i in 0..gridLineCount) {
                val y = topOffset + plotHeight * (gridLineCount - i) / gridLineCount
                drawLine(
                    color = gridColor,
                    start = Offset(leftMargin, y),
                    end = Offset(size.width, y),
                    strokeWidth = gridStrokeWidth.toPx()
                )
                if (i == 0 || i == gridLineCount / 2 || i == gridLineCount) {
                    val value = minVal + (range / gridLineCount) * i
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toInt().toString(),
                        leftMargin - 4.dp.toPx(),
                        y - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2,
                        paint
                    )
                }
            }
            drawLine(
                color = Color.Black,
                start = Offset(leftMargin, topOffset),
                end = Offset(leftMargin, size.height - bottomOffset),
                strokeWidth = 2.dp.toPx()
            )

            // 데이터 라인
            val path = Path().apply {
                moveTo(
                    leftMargin,
                    topOffset + (1f - (data[0] - minVal) / range) * plotHeight
                )
                data.drop(1).forEachIndexed { idx, v ->
                    lineTo(
                        leftMargin + (idx + 1) * spacing,
                        topOffset + (1f - (v - minVal) / range) * plotHeight
                    )
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 마커 라인
            markerPosition?.takeIf { it in data.indices }?.let { idx ->
                val x = leftMargin + idx * spacing
                drawLine(
                    color = Color.Red,
                    start = Offset(x, topOffset),
                    end = Offset(x, topOffset + plotHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun SpeedLineChart(
    data: List<Int>,
    title: String = "속도",
    unit: String = "km/h",
    markerPosition: Int? = null,
    modifier: Modifier = Modifier
) {
    LineChart(
        data = data,
        lineColor = MaterialTheme.colorScheme.primary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier
    )
}

@Composable
fun RpmLineChart(
    data: List<Int>,
    title: String = "rpm",
    unit: String = "r/min",
    markerPosition: Int? = null,
    modifier: Modifier = Modifier
) {
    LineChart(
        data = data,
        lineColor = MaterialTheme.colorScheme.secondary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier
    )
}

@Composable
fun TempLineChart(
    data: List<Int>,
    title: String = "온도",
    unit: String = "°C",
    markerPosition: Int? = null,
    modifier: Modifier = Modifier
) {
    LineChart(
        data = data,
        lineColor = MaterialTheme.colorScheme.tertiary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier
    )
}

@Composable
fun InstantKPLChart(
    data: List<Float>,
    title: String = "순간연비",
    unit: String = "KM/L",
    markerPosition: Int? = null,
    modifier: Modifier = Modifier
) {
    val intList1: List<Int> = data.map { it.toInt() }

    LineChart(
        data = intList1,
        lineColor = MaterialTheme.colorScheme.tertiary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier
    )
}

@Composable
fun PlayControlRow(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(16.dp), horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = { onPlayPause() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

