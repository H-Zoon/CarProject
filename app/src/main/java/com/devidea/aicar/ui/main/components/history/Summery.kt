package com.devidea.aicar.ui.main.components.history

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.ui.main.viewmodels.HistoryViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Wallet

/** 요약 결과를 담는 DTO */
data class SessionSummary(
    val distanceKm: Float,
    val avgSpeed: Float,
    val avgKPL: Float,
    val fuelPrice: Int,
    val hardAccel: Int,
    val hardBrake: Int
)

data class SummaryItem(
    val icon: ImageVector,
    val title: String,
    val value: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    // 데이터 로드 및 요약 계산
    val dataPoints by viewModel.getSessionData(sessionId)
        .collectAsState(initial = emptyList())

    val summary = remember(dataPoints) {
        calculateSessionSummary(dataPoints)
    }

    // 주행 점수 계산 (효율성 50%, 부드러움 50%)
    val drivingFeedback = remember(summary) {
        getDrivingFeedback(
            avgKPL = summary.avgKPL,
            hardAccel = summary.hardAccel,
            hardBrake = summary.hardBrake,
            distanceKm = summary.distanceKm
        )
    }

    val summaryList = listOf(
        SummaryItem(
            icon = Icons.Default.DirectionsCar,
            title = "운행 거리",
            value = String.format("%.2f km", summary.distanceKm)
        ),
        SummaryItem(
            icon = Icons.Default.Speed,
            title = "평균 속도",
            value = String.format("%.1f km/h", summary.avgSpeed)
        ),
        SummaryItem(
            icon = Icons.Default.LocalGasStation,
            title = "평균 연비",
            value = String.format("%.2f km/L", summary.avgKPL)
        ),
        SummaryItem(
            icon = Icons.Default.Wallet,
            title = "유류비",
            value = "${summary.fuelPrice} 원"
        ),
        SummaryItem(
            icon = Icons.Default.Timeline,
            title = "급가속",
            value = "${summary.hardAccel} 회"
        ),
        SummaryItem(
            icon = Icons.Default.TrendingDown,
            title = "급감속",
            value = "${summary.hardBrake} 회"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DrivingScoreIndicator(drivingFeedback.driveScore)
                DrivingFeedbackView(drivingFeedback)
            }
        }

        items(summaryList.chunked(2)) { rowList ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowList.forEach { summary ->
                    SummaryCard(
                        icon = summary.icon,
                        title = summary.title,
                        value = summary.value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowList.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 세션 요약 데이터 계산
 */
private fun calculateSessionSummary(dataPoints: List<DrivingDataPoint>): SessionSummary {
    val dtSec = 1.0
    val distance = dataPoints
        .zipWithNext()
        .sumOf { (p1, p2) -> ((p1.speed + p2.speed) / 2.0) / 3600.0 * dtSec }
        .toFloat()
    var accel = 0
    var brake = 0
    dataPoints.zipWithNext().forEach { (p1, p2) ->
        val dvMs = (p2.speed - p1.speed) * (1000.0 / 3600.0)
        val a = dvMs / dtSec
        if (a > 2.5) accel++
        if (a < -2.5) brake++
    }
    val avgSpeed = dataPoints.map { it.speed }.average().toFloat()
    val totalFuel = dataPoints.sumOf { dp ->
        if (dp.instantKPL > 0f) (dp.speed / 3600.0) / dp.instantKPL else 0.0
    }
    val avgKPL = if (totalFuel > 0) (distance / totalFuel).toFloat() else 0f

    val fuelUsed = distance / avgKPL
    // 총 유류비 계산
    val fuelPrice = (fuelUsed * 1500).toInt()

    return SessionSummary(distance, avgSpeed, avgKPL, fuelPrice, accel, brake)
}

@Composable
fun SummaryCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun DrivingScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    // 애니메이션으로 0f -> score/100f 까지 증가
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "주행 점수",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
            )
            Text(
                text = "${score.coerceIn(0, 100)}",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

data class DrivingFeedback(
    val driveScore: Int,
    val efficiencyMsg: String,
    val smoothnessMsg: String
)

fun getDrivingFeedback(
    avgKPL: Float,
    hardAccel: Int,
    hardBrake: Int,
    distanceKm: Float
): DrivingFeedback {
    // 1) 효율성(Efficiency) 평가
    val kplMin = 5f
    val kplMax = 30f
    val eNorm = ((avgKPL - kplMin) / (kplMax - kplMin)).coerceIn(0f, 1f)
    val efficiencyMsg = when {
        eNorm >= 0.8f -> "안정적인 속도 유지가 효과적이었네요."
        eNorm >= 0.5f -> "급가속·급감속을 줄이면 더 좋아집니다."
        else -> "급출발·급정지를 피하고 일정 속도 주행을 권장합니다."
    }

    // 2) 부드러움(Smoothness) 평가
    val events = (hardAccel + hardBrake).toFloat()
    // km당 1회 이벤트 기준
    val sNorm = (1f - (events / distanceKm)).coerceIn(0f, 1f)
    val smoothnessMsg = when {
        sNorm >= 0.8f -> "급격한 속도 변화가 거의 없었습니다."
        sNorm >= 0.5f -> "급가속·급감속을 조금만 줄여 보세요."
        else -> "가속과 제동을 신경쓰면 점수가 크게 올라갑니다."
    }

    val driveScore = ((eNorm * 0.5f + sNorm * 0.5f) * 100).toInt()

    return DrivingFeedback(driveScore, efficiencyMsg, smoothnessMsg)
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
