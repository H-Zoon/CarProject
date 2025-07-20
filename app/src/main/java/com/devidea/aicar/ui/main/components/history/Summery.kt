package com.devidea.aicar.ui.main.components.history

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.ui.main.components.GaugeCard
import com.devidea.aicar.ui.main.viewmodels.HistoryViewModel

/** 요약 결과를 담는 DTO */
data class SessionSummary(
    val distance: Float,
    val avgSpeed: Float,
    val avgKPL: Float,
    val fuelPrice: Int,
    val accelEvent: Int,
    val brakeEvent: Int,
)

data class SummaryItem(
    val icon: ImageVector,
    val title: String,
    val value: String,
    val unit: String,
)

@Composable
fun SessionSummaryRoute(
    sessionId: Long,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    // 데이터 로드 및 요약 계산 (기존 로직을 그대로 가져옵니다)
    // val dataPoints by viewModel.getSessionData(sessionId).collectAsState(initial = emptyList()) // 지도 등에 필요 없다면 생략 가능

    val summary by viewModel
        .getSessionSummery(sessionId)
        .collectAsState(initial = null)

    // 로딩 중이거나 데이터가 없을 경우 처리
    if (summary == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator() // 또는 "데이터 없음" 메시지
        }
        return
    }

    // 주행 점수 계산
    val drivingFeedback =
        remember(summary) {
            getDrivingFeedback(
                avgKPL = summary?.averageKPL,
                hardAccel = summary?.accelEvent,
                hardBrake = summary?.brakeEvent,
                distanceKm = summary?.totalDistanceKm,
            )
        }

    // UI에 전달할 리스트 데이터 생성
    val summaryList =
        listOf(
            SummaryItem(
                icon = Icons.Default.DirectionsCar,
                title = "운행 거리",
                value = String.format("%.2f", summary?.totalDistanceKm),
                unit = "Km",
            ),
            SummaryItem(
                icon = Icons.Default.Speed,
                title = "평균 속도",
                value = String.format("%.1f", summary?.averageSpeedKmh),
                unit = "km/h",
            ),
            SummaryItem(
                icon = Icons.Default.LocalGasStation,
                title = "평균 연비",
                value = String.format("%.2f", summary?.averageKPL),
                unit = " km/L",
            ),
            SummaryItem(
                icon = Icons.Default.Wallet,
                title = "유류비",
                value = "${summary?.fuelCost}",
                unit = "원",
            ),
            SummaryItem(
                icon = Icons.Default.Timeline,
                title = "급가속",
                value = "${summary?.accelEvent}",
                unit = "회",
            ),
            SummaryItem(
                icon = Icons.Default.TrendingDown,
                title = "급감속",
                value = "${summary?.brakeEvent}",
                unit = "회",
            ),
        )

    // 상태 없는 UI 컴포저블 호출
    SessionSummaryScreen(
        drivingFeedback = drivingFeedback,
        summaryList = summaryList,
    )
}

@Composable
fun SessionSummaryScreen(
    drivingFeedback: DrivingFeedback,
    summaryList: List<SummaryItem>,
) {
    // LazyColumn을 LazyVerticalGrid로 교체합니다.
    LazyVerticalGrid(
        // 1. 컬럼(열)의 개수를 2개로 고정합니다.
        columns = GridCells.Fixed(2),
        modifier =
            Modifier
                .fillMaxSize(),
        // 2. 그리드 전체의 좌우/상하 패딩을 설정합니다.
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp),
        // 3. 아이템 사이의 수직(세로) 간격을 설정합니다.
        verticalArrangement = Arrangement.spacedBy(16.dp),
        // 4. 아이템 사이의 수평(가로) 간격을 설정합니다.
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 5. 헤더 아이템: 그리드의 최상단에 전체 너비를 차지하도록 설정합니다.
        item(
            // span을 사용하여 이 아이템이 차지할 칸 수를 지정합니다.
            // maxLineSpan은 현재 행의 최대 칸 수(여기서는 2)를 의미합니다.
            span = { GridItemSpan(maxLineSpan) },
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(0.4f)) {
                    DrivingScoreIndicator(drivingFeedback.driveScore)
                }
                Column(modifier = Modifier.weight(0.6f)) {
                    DrivingFeedbackView(drivingFeedback)
                }
            }
        }

        // 6. 나머지 summaryList 아이템들을 그리드에 배치합니다.
        // chunked, Row, Spacer 로직이 모두 필요 없어집니다.
        items(summaryList.size) { summary ->
            GaugeCard(
                title = summaryList[summary].title,
                icon = summaryList[summary].icon,
                value = summaryList[summary].value,
            )
        }
    }
}

@Composable
fun DrivingScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier,
) {
    // 애니메이션으로 0f -> score/100f 까지 증가
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec =
            tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing,
            ),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "주행 점수",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp),
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
            )
            Text(
                text = "${score.coerceIn(0, 100)}",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

data class DrivingFeedback(
    val driveScore: Int,
    val efficiencyMsg: String,
    val smoothnessMsg: String,
)

fun getDrivingFeedback(
    avgKPL: Float?,
    hardAccel: Int?,
    hardBrake: Int?,
    distanceKm: Float?,
): DrivingFeedback {
    if (avgKPL == null || hardAccel == null || hardBrake == null || distanceKm == null) {
        return DrivingFeedback(100, "주행 기록이 필요합니다.", "주행 기록이 필요합니다.")
    } else {
        // 1) 효율성(Efficiency) 평가
        val kplMin = 5f
        val kplMax = 30f
        val eNorm = ((avgKPL - kplMin) / (kplMax - kplMin)).coerceIn(0f, 1f)
        val efficiencyMsg =
            when {
                eNorm >= 0.8f -> "안정적인 속도 유지가 효과적이었네요."
                eNorm >= 0.5f -> "급가속·급감속을 줄이면 더 좋아집니다."
                else -> "급출발·급정지를 피하고 일정 속도 주행을 권장합니다."
            }

        // 2) 부드러움(Smoothness) 평가
        val events = (hardAccel + hardBrake).toFloat()
        // km당 1회 이벤트 기준
        val sNorm = (1f - (events / distanceKm)).coerceIn(0f, 1f)
        val smoothnessMsg =
            when {
                sNorm >= 0.8f -> "급격한 속도 변화가 거의 없었습니다."
                sNorm >= 0.5f -> "급가속·급감속을 조금만 줄여 보세요."
                else -> "가속과 제동을 신경쓰면 점수가 크게 올라갑니다."
            }

        val driveScore = ((eNorm * 0.5f + sNorm * 0.5f) * 100).toInt()

        return DrivingFeedback(driveScore, efficiencyMsg, smoothnessMsg)
    }
}

@Composable
fun DrivingFeedbackView(feedback: DrivingFeedback) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🌱 연비 효율성", style = MaterialTheme.typography.titleMedium)
        Text(feedback.efficiencyMsg, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Text("🛣️ 운전 부드러움", style = MaterialTheme.typography.titleMedium)
        Text(feedback.smoothnessMsg, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun SessionSummaryScreenPreview() {
    // 프리뷰에서 사용할 가짜 데이터 생성
    val previewFeedback = DrivingFeedback(driveScore = 95, efficiencyMsg = "급가속·급감속을 줄이면 더 좋아집니다.", smoothnessMsg = "급가속·급감속을 줄이면 더 좋아집니다.")
    val previewList =
        listOf(
            SummaryItem(Icons.Default.DirectionsCar, "운행 거리", "123.4", "km"),
            SummaryItem(Icons.Default.Speed, "평균 속도", "88.8", "km/h"),
            SummaryItem(Icons.Default.LocalGasStation, "평균 연비", "15.2", "km/L"),
            SummaryItem(Icons.Default.Wallet, "유류비", "9,870", "원"),
            SummaryItem(Icons.Default.Timeline, "급가속", "1", "회"),
            SummaryItem(Icons.Default.TrendingDown, "급감속", "0", "회"),
        )

    MaterialTheme {
        SessionSummaryScreen(
            drivingFeedback = previewFeedback,
            summaryList = previewList,
        )
    }
}
