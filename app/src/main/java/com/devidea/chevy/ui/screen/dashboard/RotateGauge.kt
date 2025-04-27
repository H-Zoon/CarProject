package com.devidea.chevy.ui.screen.dashboard

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
import com.devidea.chevy.ui.components.CarStatus
import com.devidea.chevy.ui.theme.SpeedMeterFontFamily
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Dashboard(carViewModel: CarViewModel = hiltViewModel()) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            SpeedIndicator(carViewModel)
            TempIndicator(carViewModel)
            RevolutionIndicator(carViewModel)
        }
        CarStatus()
    }
}

@Composable
fun SpeedIndicator(carViewModel: CarViewModel = hiltViewModel()) {
    val mGear by carViewModel.mGear.collectAsState()
    val mGearNum by carViewModel.mGearNum.collectAsState()
    val mSpeed by carViewModel.mSpeed.collectAsState()

    Column {
        Row {
            Text(
                text = "$mSpeed",
                modifier = Modifier.alignByBaseline(),
                style = TextStyle(
                    fontFamily = SpeedMeterFontFamily,
                    fontSize = 50.sp,
                    color = Color.Black,
                )
            )
            Text(
                text = " Km/h",
                modifier = Modifier.alignByBaseline(),
                style = TextStyle(
                    fontFamily = SpeedMeterFontFamily,
                    fontSize = 20.sp,
                    color = Color.Black,
                )
            )
        }

        Text(
            text = mGear + mGearNum,
            modifier = Modifier,
            style = TextStyle(
                fontFamily = SpeedMeterFontFamily,
                fontSize = 30.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun RevolutionIndicator(carViewModel: CarViewModel) {
    val mRotate by carViewModel.mRotate.collectAsState()
    var isAnimating by remember { mutableStateOf(true) }
    var rotateValue by remember { mutableIntStateOf(0) }


    LaunchedEffect(Unit) {
        if (isAnimating) {
            for (i in 8000 downTo 0 step 8000) {
                rotateValue = i
                delay(1500)
            }
            isAnimating = false
        }
    }

    val animatedSweepAngle by animateFloatAsState(
        targetValue = if (isAnimating) (rotateValue / 8000f) * 270f else (mRotate / 8000f) * 270f,
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
        for (i in 0..8) {
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
                style = TextStyle(
                    fontFamily = SpeedMeterFontFamily,
                    fontSize = 23.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun TempIndicator(carViewModel: CarViewModel) {
    val mTemp by carViewModel.mTemp.collectAsState()
    var isAnimating by remember { mutableStateOf(true) }
    var tempValue by remember { mutableIntStateOf(0) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        if (isAnimating) {
            for (i in 115 downTo 0 step 115) {
                tempValue = i
                delay(1500)
            }
            isAnimating = false
        }
    }

    val animatedSweepAngle by animateFloatAsState(
        targetValue = if (isAnimating) (tempValue / 115f) * -70f else (mTemp / 8000f) * -70f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

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
            color = Color.Black,
            startAngle = -280f,
            sweepAngle = -70f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            size = Size(arcSize, arcSize)
        )

        // Arc 그리기
        drawArc(
            topLeft = Offset(centerOffset, centerOffset),
            color = Color.Red,
            startAngle = -280f,
            sweepAngle = animatedSweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            size = Size(arcSize, arcSize)
        )

        for (i in 0..2) {
            val angle = -290f + i * -23.5f

            val angleRad = Math.toRadians(angle.toDouble())

            // 사각형의 중심 좌표를 계산
            val x = (radius * cos(angleRad)).toFloat()
            val y = (radius * sin(angleRad)).toFloat()

            val textOffset = Offset(
                x + centerOffset + radius - (strokeWidth * 1.5f) / 2,
                y + centerOffset + radius - (strokeWidth * 2.5f) / 2
            )

            drawText(
                textMeasurer,
                text = "$i",
                topLeft = textOffset,
                style = TextStyle(
                    fontFamily = SpeedMeterFontFamily,
                    fontSize = 15.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
