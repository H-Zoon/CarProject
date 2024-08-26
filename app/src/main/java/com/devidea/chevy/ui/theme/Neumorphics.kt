package com.devidea.chevy.ui.theme

import android.view.MotionEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    lightShadowColor: Color = MaterialTheme.colorScheme.outline,
    darkShadowColor: Color = MaterialTheme.colorScheme.tertiary,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Int = 20,
    blurRadius: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(lightShadowColor, backgroundColor)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(-shadowOffset, -shadowOffset) }
                .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(lightShadowColor, RoundedCornerShape(cornerRadius))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(shadowOffset, shadowOffset) }
                .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(darkShadowColor, RoundedCornerShape(cornerRadius))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(cornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun NeumorphicBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    lightShadowColor: Color = MaterialTheme.colorScheme.outline,
    darkShadowColor: Color = MaterialTheme.colorScheme.onTertiary,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Int = 20,
    blurRadius: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    NeumorphicSurface(
        modifier = modifier,
        backgroundColor = backgroundColor,
        lightShadowColor = lightShadowColor,
        darkShadowColor = darkShadowColor,
        cornerRadius = cornerRadius,
        shadowOffset = shadowOffset,
        blurRadius = blurRadius,
        content = content
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    lightShadowColor: Color = MaterialTheme.colorScheme.outline,
    darkShadowColor: Color = MaterialTheme.colorScheme.onTertiary,
    cornerRadius: Dp = 16.dp,
    pressedShadowOffset: Int = 5,
    defaultShadowOffset: Int = 15,
    pressedBlurRadius: Dp = 4.dp,
    defaultBlurRadius: Dp = 6.dp,
    content: @Composable () -> Unit
) {
    var isPressedState by remember { mutableStateOf(false) }
    val offset by animateIntAsState(targetValue = if (isPressedState) pressedShadowOffset else defaultShadowOffset)
    val blur by animateDpAsState(targetValue = if (isPressedState) pressedBlurRadius else defaultBlurRadius)
    var size by remember { mutableStateOf(Size.Zero) }
    var position by remember { mutableStateOf(Offset.Zero) }

    NeumorphicSurface(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                size = coordinates.size.toSize() // 뷰의 크기를 저장
                position = coordinates.positionInWindow()
            }
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressedState = true
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val touchX = motionEvent.rawX
                        val touchY = motionEvent.rawY
                        val isOutside = touchX < position.x || touchY < position.y ||
                                touchX > position.x + size.width ||
                                touchY > position.y + size.height

                        if (isOutside) {
                            isPressedState = false
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if(isPressedState){
                            isPressedState = false
                            onClick?.invoke()
                        }
                        true
                    }
                    else -> false
                }
            },
        backgroundColor = backgroundColor,
        lightShadowColor = lightShadowColor,
        darkShadowColor = darkShadowColor,
        cornerRadius = cornerRadius,
        shadowOffset = offset,
        blurRadius = blur,
        content = content
    )
}
