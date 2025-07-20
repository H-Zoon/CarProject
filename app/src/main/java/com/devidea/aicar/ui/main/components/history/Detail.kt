package com.devidea.aicar.ui.main.components.history

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.ui.main.components.SessionTrackMap
import com.devidea.aicar.ui.main.viewmodels.HistoryViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay

@Composable
fun SessionDetailRoute(
    sessionId: Long,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    // ViewModel에서 데이터를 가져옵니다.
    val dataPoints by viewModel.getSessionData(sessionId).collectAsState(initial = emptyList())
    val sliderPosition by viewModel.sliderPosition.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // 데이터가 비어있을 경우 로딩 또는 빈 화면을 표시합니다.
    if (dataPoints.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // UI에 전달할 데이터를 가공합니다.
    val speedSeries = remember(dataPoints) { dataPoints.map { it.speed.toInt() } }
    val rpmSeries = remember(dataPoints) { dataPoints.map { it.rpm.toInt() } }
    val tempSeries = remember(dataPoints) { dataPoints.map { it.engineTemp.toInt() } }
    val instantKPLSeries = remember(dataPoints) { dataPoints.map { it.instantKPL.toFloat() } }
    val path = remember(dataPoints) { dataPoints.map { LatLng(it.latitude, it.longitude) } }
    val sliderRange = 0f..(dataPoints.lastIndex.toFloat())

    // 재생 로직을 처리하는 LaunchedEffect
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (viewModel.isPlaying.value) {
                val next = (viewModel.sliderPosition.value + 1).coerceAtMost(dataPoints.lastIndex.toFloat())
                viewModel.updateSlider(next)
                if (viewModel.sliderPosition.value >= dataPoints.lastIndex) {
                    viewModel.togglePlay()
                    break
                }
                delay(100L)
            }
        }
    }

    // 상태 없는 UI 컴포저블을 호출하며 필요한 데이터와 콜백을 전달합니다.
    SessionDetailScreen(
        speedSeries = speedSeries,
        rpmSeries = rpmSeries,
        tempSeries = tempSeries,
        instantKPLSeries = instantKPLSeries,
        path = path,
        sliderPosition = sliderPosition,
        sliderRange = sliderRange,
        isPlaying = isPlaying,
        onSliderChange = { viewModel.updateSlider(it) },
        onPlayPause = { viewModel.togglePlay() },
    )
}

@Composable
fun SessionDetailScreen(
    // ViewModel 대신 가공된 데이터와 콜백 함수를 파라미터로 받습니다.
    speedSeries: List<Int>,
    rpmSeries: List<Int>,
    tempSeries: List<Int>,
    instantKPLSeries: List<Float>,
    path: List<LatLng>,
    sliderPosition: Float,
    sliderRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean,
    onSliderChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .height(300.dp)
                    .shadow(elevation = 10.dp),
        ) {
            // SessionTrackMap은 이미 상태가 없으므로 그대로 사용
            SessionTrackMap(
                path = path,
                sliderPosition = sliderPosition,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp),
            )
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SpeedLineChart(
                    data = speedSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                )
            }
            item {
                RpmLineChart(
                    data = rpmSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                )
            }
            item {
                TempLineChart(
                    data = tempSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                )
            }
            item {
                InstantKPLChart(
                    data = instantKPLSeries,
                    markerPosition = sliderPosition.toInt(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = onSliderChange, // 콜백 함수 연결
                valueRange = sliderRange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
            )
            PlayControlRow(
                isPlaying = isPlaying,
                onPlayPause = onPlayPause, // 콜백 함수 연결
            )
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
    gridStrokeWidth: Dp = 1.dp,
) {
    if (data.isEmpty()) return
    val density = LocalDensity.current
    val paint =
        remember {
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
            )
        }

        // 2) 차트 그리기
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp),
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
                    strokeWidth = gridStrokeWidth.toPx(),
                )
                if (i == 0 || i == gridLineCount / 2 || i == gridLineCount) {
                    val value = minVal + (range / gridLineCount) * i
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toInt().toString(),
                        leftMargin - 4.dp.toPx(),
                        y - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2,
                        paint,
                    )
                }
            }
            drawLine(
                color = Color.Black,
                start = Offset(leftMargin, topOffset),
                end = Offset(leftMargin, size.height - bottomOffset),
                strokeWidth = 2.dp.toPx(),
            )

            // 데이터 라인
            val path =
                Path().apply {
                    moveTo(
                        leftMargin,
                        topOffset + (1f - (data[0] - minVal) / range) * plotHeight,
                    )
                    data.drop(1).forEachIndexed { idx, v ->
                        lineTo(
                            leftMargin + (idx + 1) * spacing,
                            topOffset + (1f - (v - minVal) / range) * plotHeight,
                        )
                    }
                }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )

            // 마커 라인
            markerPosition?.takeIf { it in data.indices }?.let { idx ->
                val x = leftMargin + idx * spacing
                drawLine(
                    color = Color.Red,
                    start = Offset(x, topOffset),
                    end = Offset(x, topOffset + plotHeight),
                    strokeWidth = 1.dp.toPx(),
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
    modifier: Modifier = Modifier,
) {
    LineChart(
        data = data,
        lineColor = MaterialTheme.colorScheme.primary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier,
    )
}

@Composable
fun RpmLineChart(
    data: List<Int>,
    title: String = "rpm",
    unit: String = "r/min",
    markerPosition: Int? = null,
    modifier: Modifier = Modifier,
) {
    LineChart(
        data = data,
        lineColor = MaterialTheme.colorScheme.secondary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier,
    )
}

@Composable
fun TempLineChart(
    data: List<Int>,
    title: String = "온도",
    unit: String = "°C",
    markerPosition: Int? = null,
    modifier: Modifier = Modifier,
) {
    LineChart(
        data = data,
        lineColor = MaterialTheme.colorScheme.tertiary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier,
    )
}

@Composable
fun InstantKPLChart(
    data: List<Float>,
    title: String = "순간연비",
    unit: String = "KM/L",
    markerPosition: Int? = null,
    modifier: Modifier = Modifier,
) {
    val intList1: List<Int> = data.map { it.toInt() }

    LineChart(
        data = intList1,
        lineColor = MaterialTheme.colorScheme.tertiary,
        title = title,
        unit = unit,
        markerPosition = markerPosition,
        modifier = modifier,
    )
}

@Composable
fun PlayControlRow(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(16.dp), horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = { onPlayPause() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true, name = "SessionDetailScreen Preview")
@Composable
fun SessionDetailScreenPreview() {
    // 프리뷰에서 사용할 가짜 데이터 생성
    val sampleData = List(100) { (it * 1.2f).toInt() }
    val sampleFloatData = List(100) { (it * 0.2f) }
    val samplePath = listOf(LatLng(37.5665, 126.9780), LatLng(37.5670, 126.9790))

    MaterialTheme {
        SessionDetailScreen(
            speedSeries = sampleData.map { (it + 20).coerceAtMost(120) },
            rpmSeries = sampleData.map { (it * 20 + 800).coerceAtMost(4000) },
            tempSeries = sampleData.map { (it / 2 + 70).coerceAtMost(95) },
            instantKPLSeries = sampleFloatData.map { (20f - it).coerceAtLeast(5f) },
            path = samplePath,
            sliderPosition = 50f,
            sliderRange = 0f..99f,
            isPlaying = false,
            onSliderChange = {},
            onPlayPause = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 150)
@Composable
fun SpeedLineChartPreview() {
    val sampleData = List(50) { (Math.random() * 80 + 20).toInt() }
    MaterialTheme {
        SpeedLineChart(data = sampleData, markerPosition = 25, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 150)
@Composable
fun RpmLineChartPreview() {
    val sampleData = List(50) { (Math.random() * 2000 + 800).toInt() }
    MaterialTheme {
        RpmLineChart(data = sampleData, markerPosition = 25, modifier = Modifier.padding(16.dp))
    }
}
