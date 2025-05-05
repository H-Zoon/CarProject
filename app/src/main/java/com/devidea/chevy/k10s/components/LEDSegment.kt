package com.devidea.chevy.k10s.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.k10s.dashboard.CarViewModel
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun LEDSeconds(carViewModel: CarViewModel = hiltViewModel()) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val mSpeed by carViewModel.mSpeed.collectAsState()
    var displaySpeed by remember { mutableStateOf(0) }
    var isAnimating by remember { mutableStateOf(true) }

    Box(
        Modifier
            .drawWithContent {
                drawContent()
            }
            .onSizeChanged {
                size = it
            }
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Box(
            Modifier
                .drawWithContent {
                    drawContent()
                }
                .fillMaxSize()
        )

        LaunchedEffect(Unit) {
            for (i in 0..999 step 111) {
                displaySpeed = i
                delay(300) // 애니메이션 속도를 조절할 수 있습니다.
            }
            isAnimating = false
            displaySpeed = mSpeed // 애니메이션이 끝나면 실제 속도로 변경
        }

        val width by remember {
            derivedStateOf {
                with(density) { (min(size.width, size.height) / 4).toDp() }
            }
        }

        val speedToDisplay = if (isAnimating) displaySpeed else mSpeed

        val hundreds = speedToDisplay / 100
        val tens = (speedToDisplay % 100) / 10
        val units = speedToDisplay % 10

        Row(
            modifier = Modifier
                .width(width * 3)
                .height(width * 2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(width * .25f)
        ) {
            // 각 자리수에 맞는 LEDDigit 호출
            LEDDigit(
                modifier = Modifier.weight(1f),
                digit = hundreds
            )
            LEDDigit(
                modifier = Modifier.weight(1f),
                digit = tens
            )
            LEDDigit(
                modifier = Modifier.weight(1f),
                digit = units
            )
        }
    }
}

@Composable
fun LEDDigit(
    modifier: Modifier = Modifier,
    digit: Int,
) {

    val stems = intMap[digit]

    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val width by remember {
        derivedStateOf {
            with(density) { size.width.toDp() }
        }
    }

    Box(
        modifier = modifier
            .padding(4.dp)
            .onSizeChanged { size = it }
    ) {

        LEDSegment( // TOP RIGHT
            modifier = Modifier
                .offset(x = width / 2, y = -width / 2)
                .rotate(-90f)
                .scale(-1f, 1f),
            on = stems?.getOrNull(2) == '1'
        )

        LEDSegment( // TOP
            modifier = Modifier.offset(y = -width),
            on = stems?.getOrNull(0) == '1'
        )

        LEDSegment( // BOTTOM RIGHT
            modifier = Modifier
                .offset(x = width / 2, y = width / 2)
                .rotate(-90f)
                .scale(-1f, 1f),
            on = stems?.getOrNull(5) == '1'
        )

        LEDSegment( // MIDDLE
            on = stems?.getOrNull(3) == '1'
        )

        LEDSegment( // TOP LEFT
            modifier = Modifier
                .offset(x = -width / 2, y = -width / 2)
                .rotate(-90f)
                .scale(-1f, 1f),
            on = stems?.getOrNull(1) == '1'
        )

        LEDSegment( // BOTTOM
            modifier = Modifier.offset(y = width),
            on = stems?.getOrNull(6) == '1'
        )

        LEDSegment( // BOTTOM LEFT
            modifier = Modifier
                .offset(x = -width / 2, y = width / 2)
                .rotate(-90f)
                .scale(-1f, 1f),
            on = stems?.getOrNull(4) == '1'
        )
    }
}


@Composable
private fun LEDSegment(
    modifier: Modifier = Modifier,
    on: Boolean = false,
) {

    val animatedStemShadowOffset by animateDpAsState(
        targetValue = if (on) 5.dp else 0.dp,
        if (on)
            spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            )
        else
            spring(stiffness = Spring.StiffnessLow)
    )

    val animatedShadow by animateDpAsState(
        targetValue = if (on) 4.dp else 1.dp,
        spring(
            stiffness = Spring.StiffnessLow,
        )
    )

    val animatedSheenAlpha by animateFloatAsState(
        targetValue = if (on) 1f else .1f,
        spring(
            stiffness = Spring.StiffnessLow,
        )
    )

    val animatedSheenInfluence by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
    )

    Box(
        modifier = modifier
            .fillMaxHeight(.12f)
            .fillMaxWidth(.9f)
    ) {
        Box(
            Modifier
                .offset(x = (-animatedStemShadowOffset / 2), y = (-animatedStemShadowOffset / 2))
                .fillMaxSize()
                .blur(animatedShadow, BlurredEdgeTreatment.Unbounded)
                .background(Color.White.copy(alpha = .5f), CutCornerShape(100))
        )

        Box(
            Modifier
                .offset(x = animatedStemShadowOffset, y = animatedStemShadowOffset)
                .fillMaxSize()
                .blur(animatedShadow, BlurredEdgeTreatment.Unbounded)
                .background(Color.Black.copy(alpha = .15f), CutCornerShape(100))
        )

        Box(
            Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = animatedSheenAlpha),
                            Color.Transparent,
                            Color.Black.copy(alpha = .04f),
                        )
                    ), shape = CutCornerShape(100)
                )
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            lerp(gray300, gray200, animatedSheenInfluence),
                            gray300,
                        )
                    ),
                    CutCornerShape(100)
                )
        )
    }
}


private val intMap = mapOf(
    Pair(0, "1110111"),
    Pair(1, "0010010"),
    Pair(2, "1011101"),
    Pair(3, "1011011"),
    Pair(4, "0111010"),
    Pair(5, "1101011"),
    Pair(6, "1101111"),
    Pair(7, "1010010"),
    Pair(8, "1111111"),
    Pair(9, "1111011"),
)

private val gray200 = Color(0xfffafafa)
private val gray300 = Color(0xffe5e5e5)