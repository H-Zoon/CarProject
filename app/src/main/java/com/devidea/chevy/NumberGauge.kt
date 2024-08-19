package com.devidea.chevy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay


@Composable
fun HorizontalGauge() {
    var currentValue by remember { mutableStateOf(0) }
    val segmentCount = 80 // 9000을 1000으로 나누고 다시 10등분한 개수
    val segmentWidthFraction = 1f / segmentCount

    LaunchedEffect(Unit) {
        for (i in 0..9000) {
            currentValue = i
            delay(1) // 애니메이션 속도를 조절할 수 있습니다.
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.Gray, shape = RoundedCornerShape(8.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            for (i in 0 until segmentCount) {
                val segmentColor = if (i < currentValue / 100) Color.Blue else Color.LightGray
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(segmentWidthFraction)
                        .background(segmentColor, shape = RoundedCornerShape(4.dp))
                )
            }
        }
        // 현재 값 표시 텍스트
        Text(
            text = "$currentValue",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )
    }
}