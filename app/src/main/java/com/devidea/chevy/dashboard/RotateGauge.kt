package com.devidea.chevy.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devidea.chevy.ui.theme.SpeedMeterFontFamily

@Composable
fun EngineRotateGauge(
    modifier: Modifier = Modifier,
    progress: Float,
    strokeWidth: Float = 12f,
) {
    val animatedSweepAngle by animateFloatAsState(
        targetValue = progress * 180,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val diameter = size.minDimension
        val radius = diameter / 2
        val startAngle = -180f

        drawArc(
            color = Color.Blue,
            startAngle = startAngle,
            sweepAngle = animatedSweepAngle,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Butt),
            topLeft = Offset(0f, 0f),
            size = Size(diameter, diameter)
        )

        for (i in 0..8) {
            val angle = Math.toRadians(startAngle.toDouble() + i * 22.5)
            val x = (radius + (radius - strokeWidth) * Math.cos(angle)).toFloat()
            val y = (radius + (radius - strokeWidth) * Math.sin(angle)).toFloat()
            val textOffset = Offset(x - 10f, y - 10f)

            drawText(
                textMeasurer,
                text = "$i",
                topLeft = textOffset,
                style = TextStyle(fontFamily = SpeedMeterFontFamily, fontSize = 16.sp, color = Color.Black, textAlign = TextAlign.Center)
            )
        }

        for (i in 0..50) {
            val angle = startAngle + i * 3.6f
            val angleRad = Math.toRadians(angle.toDouble())
            val x = (radius + (radius - strokeWidth) * Math.cos(angleRad)).toFloat()
            val y = (radius + (radius - strokeWidth) * Math.sin(angleRad)).toFloat()
            val rectSize = 5.dp.toPx()
            val rectOffset = Offset(x - rectSize / 2, y - rectSize / 2)
            val rectColor = if (i in 45..50) {
                Color.Red
            } else {
                Color.Blue
            }

            rotate(degrees = angle + 90f, pivot = rectOffset + Offset(rectSize / 2, rectSize / 2)) {
                drawRect(
                    color = rectColor,
                    topLeft = rectOffset,
                    size = Size(rectSize, rectSize)
                )
            }
        }
    }
}