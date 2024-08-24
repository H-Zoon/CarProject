package com.devidea.chevy.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devidea.chevy.ui.theme.SpeedMeterFontFamily
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun a() {
    Box(modifier = Modifier.fillMaxSize()) {
        CustomArcWithDifferentColors()
    }
}

@Composable
fun CustomArcWithDifferentColors() {

    var isAnimating by remember { mutableStateOf(true) }
    var rotateValue by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (isAnimating) {
            // 0에서 8000으로 증가
            var i = 0
            while (i <= 8000) {
                rotateValue = i
                val increment = maxOf(1, (8000 - i) / 30)  // 목표값에 가까워질수록 증가폭 감소
                i += increment
                delay(3) // 애니메이션 속도를 조절할 수 있습니다.
            }
            delay(100)
            // 8000에서 0으로 감소
            while (i >= 0) {
                rotateValue = i
                val decrement = maxOf(1, i / 30)  // 목표값에 가까워질수록 감소폭 감소
                i -= decrement
                delay(3) // 애니메이션 속도를 조절할 수 있습니다.
            }
            isAnimating = false
            rotateValue= 3500
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
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .blur(
            radiusX = 5.dp,
            radiusY = 5.dp,
            edgeTreatment = BlurredEdgeTreatment.Unbounded
        )) {
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

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)) {

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
            color = Color.Blue,
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

        val rectWidth = 2f
        val rectHeight = 30f
        for (i in 1..6) {
            val angle = startAngle + i * 17.2f
            val angleRad = Math.toRadians(angle.toDouble())

            // 사각형의 중심 좌표를 계산
            val x = ((radius - 20f) * cos(angleRad)).toFloat()
            val y = ((radius - 5f) * sin(angleRad)).toFloat()

            // 사각형의 좌상단 좌표를 계산하기 위해 중심점에서 사각형의 반 크기를 뺌
            val rectOffset = Offset(
                x + centerOffset + radius - strokeWidth / 2,
                y + centerOffset + radius - strokeWidth / 2
            )

            // 사각형을 회전할 때 회전 중심을 사각형의 중심으로 설정
            rotate(degrees = (angle + 90f), pivot = rectOffset + Offset(strokeWidth - 110 / 2, strokeWidth / 2)) {
                drawRect(
                    color = Color.Black,
                    topLeft = rectOffset,
                    size = Size(rectWidth, rectHeight)
                )
            }
        }

        for (i in 7..13) {
            val angle = if(i == 7) startAngle + i * 17.5f else startAngle + i * 17.3f
            val angleRad = Math.toRadians(angle.toDouble())

            // 사각형의 중심 좌표를 계산
            val x = (radius * cos(angleRad)).toFloat()
            val y = (radius * sin(angleRad)).toFloat()

            // 사각형의 좌상단 좌표를 계산하기 위해 중심점에서 사각형의 반 크기를 뺌
            val rectOffset = Offset(
                x + centerOffset + radius - strokeWidth / 2,
                y + centerOffset + radius - strokeWidth / 2
            )

            // 사각형을 회전할 때 회전 중심을 사각형의 중심으로 설정
            rotate(degrees = (angle + 85f), pivot = rectOffset + Offset(strokeWidth / 2, strokeWidth / 2)) {
                drawRect(
                    color = Color.Black,
                    topLeft = rectOffset,
                    size = Size(rectWidth, rectHeight)
                )
            }
        }

        for (i in 14..15) {
            val angle = startAngle + i * 17f
            val angleRad = Math.toRadians(angle.toDouble())

            // 사각형의 중심 좌표를 계산
            val x = ((radius + 18f) * cos(angleRad)).toFloat()
            val y = ((radius + 8f) * sin(angleRad)).toFloat()

            // 사각형의 좌상단 좌표를 계산하기 위해 중심점에서 사각형의 반 크기를 뺌
            val rectOffset = Offset(
                x + centerOffset + radius - strokeWidth / 2,
                y + centerOffset + radius - strokeWidth / 2
            )

            // 사각형을 회전할 때 회전 중심을 사각형의 중심으로 설정
            rotate(degrees = (angle + 88f), pivot = rectOffset + Offset(strokeWidth - 110 / 2, strokeWidth / 2)) {
                drawRect(
                    color = Color.Red,
                    topLeft = rectOffset,
                    size = Size(rectWidth, rectHeight)
                )
            }
        }
    }
}

@Preview
@Composable

fun DrawArcExamplepew() {
    CustomArcWithDifferentColors()
}