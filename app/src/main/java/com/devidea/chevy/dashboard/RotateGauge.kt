package com.devidea.chevy.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.ui.theme.SpeedMeterFontFamily
import com.devidea.chevy.viewmodel.CarViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Dashboard(carViewModel: CarViewModel = hiltViewModel()) {
    Column {
        Box {
            SpeedIndicator(carViewModel)
            RevolutionIndicator(carViewModel)
        }
    }
}

@Composable
fun SpeedIndicator(carViewModel: CarViewModel) {
    val mGear by carViewModel.mGear.collectAsState()
    val mGearNum by carViewModel.mGearNum.collectAsState()
    val mSpeed by carViewModel.mSpeed.collectAsState()

    val gearArray = arrayOf("P", "R", "N", "D", "L")
    val gear2gArray = arrayOf("1", "2", "3", "4", "5", "6")

    var displaySpeed by remember { mutableIntStateOf(0) }
    var displayGear by remember { mutableStateOf("P") }
    var displayGearNum by remember { mutableStateOf("1") }
    var isAnimating by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // 첫 번째 반복문
        val job1 = launch {
            for (i in 0..999 step 111) {
                displaySpeed = i
                delay(100) // 애니메이션 속도를 조절할 수 있습니다.
            }
        }

        // 두 번째 반복문
        val job2 = launch {
            for (i in 0..4) {
                displayGear = gearArray[i]
                delay(500) // 애니메이션 속도를 조절할 수 있습니다.
            }
        }

        // 세 번째 반복문
        val job3 = launch {
            for (i in 0..5) {
                displayGearNum = gear2gArray[i]
                delay(500) // 애니메이션 속도를 조절할 수 있습니다.
            }
        }

        // 모든 작업이 완료될 때까지 대기
        joinAll(job1, job2, job3)

        isAnimating = false
        displaySpeed = mSpeed // 애니메이션이 끝나면 실제 속도로 변경
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
Row {

}
        Text(
            text = displaySpeed.toString(),
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(fontFamily = SpeedMeterFontFamily, fontSize = 50.sp, color = Color.Black, textAlign = TextAlign.Center),
            textAlign = TextAlign.Center
        )

        Text(
            text = "km/h",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 0.dp, start = 20.dp, bottom = 0.dp, end = 80.dp),
            style = TextStyle(fontFamily = SpeedMeterFontFamily, fontSize = 20.sp, color = Color.Black, textAlign = TextAlign.Center),
            textAlign = TextAlign.Right
        )

        Text(
            text = displayGear+displayGearNum,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 0.dp, start = 20.dp, bottom = 80.dp, end = 80.dp),
            style = TextStyle(fontFamily = SpeedMeterFontFamily, fontSize = 30.sp, color = Color.Black, textAlign = TextAlign.Center),
            textAlign = TextAlign.Right
        )
    }
}

@Composable
fun RevolutionIndicator(carViewModel: CarViewModel) {
    val mRotate by carViewModel.mRotate.collectAsState()
    var isAnimating by remember { mutableStateOf(true) }
    var rotateValue by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (isAnimating) {
            rotateValue = 8000
            delay(1000)
            rotateValue = 0
            isAnimating = false
            rotateValue = mRotate
        }
    }

    val animatedSweepAngle by animateFloatAsState(
        targetValue = (rotateValue / 8000f) * 270f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    val mainColor = MaterialTheme.colorScheme.background
    val lightShadowColor: Color = MaterialTheme.colorScheme.outline
    val darkShadowColor: Color = Color.Gray

    val startAngle = -270f
    val sweepAngle = 270f
    val textMeasurer = rememberTextMeasurer()

    // 캔버스를 기기의 가로 길이만큼의 정사각형으로 설정
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .blur(
                radiusX = 5.dp,
                radiusY = 5.dp,
                edgeTreatment = BlurredEdgeTreatment.Unbounded
            )
    ) {
        val canvasSizePx = size.width  // 정사각형이므로 width와 height가 동일
        val strokeWidth = canvasSizePx * 0.05f  // Stroke 두께를 캔버스 크기의 5%로 설정

        // 외부 원호 크기와 Offset 계산
        val outerSize = canvasSizePx * 0.8f
        val outerOffset = (canvasSizePx - outerSize) / 2

        // 내부 원호 크기와 Offset 계산
        val innerSize = canvasSizePx * 0.7f
        val innerOffset = (canvasSizePx - innerSize) / 2

        // 외부 원호 그리기
        drawArc(
            topLeft = Offset(outerOffset, outerOffset),
            color = lightShadowColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            size = Size(outerSize, outerSize)
        )

        // 내부 원호 그리기
        drawArc(
            topLeft = Offset(innerOffset, innerOffset),
            color = darkShadowColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            size = Size(innerSize, innerSize)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {

        val canvasSizePx = size.width  // 정사각형이므로 width와 height가 동일
        val arcSize = canvasSizePx * 0.75f  // Arc 크기를 캔버스 크기의 75%로 설정
        val strokeWidth = canvasSizePx * 0.07f  // Stroke 두께를 캔버스 크기의 7%로 설정
        val radius = arcSize / 2  // Arc의 반지름
        val centerOffset = (canvasSizePx - arcSize) / 2  // Arc를 중앙에 배치하기 위한 오프셋

        // Arc 그리기
        drawArc(
            topLeft = Offset(centerOffset, centerOffset),
            color = mainColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            size = Size(arcSize, arcSize)
        )

        drawArc(
            topLeft = Offset(centerOffset, centerOffset),
            color = Color.Gray,
            startAngle = startAngle,
            sweepAngle = animatedSweepAngle,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Butt),
            size = Size(arcSize, arcSize)
        )

        val increasedRadius = radius * 1.2f  // 기존 반지름보다 20% 증가시킴
        for (i in 1..8) {
            val angle = startAngle + i * 33.5f

            val angleRad = Math.toRadians(angle.toDouble())

            // 사각형의 중심 좌표를 계산
            val x = (increasedRadius * cos(angleRad)).toFloat()
            val y = (increasedRadius * sin(angleRad)).toFloat()

            val textOffset = Offset(
                x + centerOffset + increasedRadius - (strokeWidth * 2.5f) / 2,
                y + centerOffset + increasedRadius - (strokeWidth * 3.4f) / 2
            )

            drawText(
                textMeasurer,
                text = "$i",
                topLeft = textOffset,
                style = TextStyle(fontFamily = SpeedMeterFontFamily, fontSize = 23.sp, color = Color.Black, textAlign = TextAlign.Center)
            )
        }
    }
}
