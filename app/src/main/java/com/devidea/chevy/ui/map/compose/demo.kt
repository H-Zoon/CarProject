package com.devidea.chevy.ui.map.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import io.morfly.compose.bottomsheet.material3.*
import kotlinx.coroutines.CoroutineScope

class BottomSheetNestedScrollConnection<T : Any> @OptIn(ExperimentalFoundationApi::class) constructor(
    private val scope: CoroutineScope,
    private val state: BottomSheetState<T>,
    private val orientation: Orientation,
    private val onFling: (velocity: Float) -> Unit
) : NestedScrollConnection {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Only handle scroll in the sheet’s primary orientation
        val delta = if (orientation == Orientation.Vertical) available.y else available.x
        if (delta < 0f) {
            // User is scrolling “up” inside the sheet content → expand sheet
            scope.launch { state.draggableState.dispatchRawDelta(-delta) }
            // consume the portion that we applied to the sheet
            return if (orientation == Orientation.Vertical) Offset(0f, delta) else Offset(delta, 0f)
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(
        consumed: androidx.compose.ui.unit.Velocity,
        available: androidx.compose.ui.unit.Velocity
    ): androidx.compose.ui.unit.Velocity {
        // Hand off any remaining fling velocity to the sheet’s settle logic
        onFling(if (orientation == Orientation.Vertical) available.y else available.x)
        return androidx.compose.ui.unit.Velocity.Zero
    }
}

// 1) 먼저 state 정의
enum class CustomSheetValue { Collapsed, HalfExpanded, Expanded }

// 2) SubcomposeLayout 기반 Scaffold
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlogStyleBottomSheetScaffold(
    sheetState: BottomSheetState<CustomSheetValue>,
    modifier: Modifier = Modifier,
    sheetContent: @Composable ColumnScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val collapsedYState = remember { mutableStateOf(0f) }
    // Nested scroll connection (optional, for 내부 스크롤이 시트에 전달될 때)
    val nested = remember(sheetState) {
        BottomSheetNestedScrollConnection(
            scope = scope,
            state = sheetState,
            orientation = Orientation.Vertical,
            onFling = { v -> scope.launch { sheetState.draggableState.settle(v) } }
        )
    }

    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val layoutWidth  = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        // 1) BODY 측정 — sheetOffset - collapsedYState.value 로 실시간 위치 조정
        val bodyPlaceable = subcompose("body") {
            Box(
                Modifier
                    .fillMaxSize()
                    .offset {
                        val sheetOffset = sheetState.draggableState.requireOffset().toInt()
                        IntOffset(0, sheetOffset - collapsedYState.value.toInt())
                    }
                    .nestedScroll(nested)
            ) {
                content()
            }
        }[0].measure(constraints)

        // 2) SHEET 측정 — 피크 높이(peekPx)를 이용해 collapsedY 계산
        val sheetPlaceable = subcompose("sheet") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { sheetSize ->
                        // (a) 사용 중인 peek 높이(dp)를 px로 변환
                        val peekPx = 100.dp.roundToPx()

                        // (b) collapsed 상태의 Y 위치
                        //    → 화면 아래에서 peek만큼 올라온 지점
                        val collapsedY = (layoutHeight - peekPx).toFloat()
                        collapsedYState.value = collapsedY

                        // (c) 다른 앵커들도 재설정
                        val halfY     = (layoutHeight * 0.5f)
                        val expandedY = maxOf(layoutHeight - sheetSize.height, 0).toFloat()
                        sheetState.draggableState.updateAnchors(
                            DraggableAnchors<CustomSheetValue> {
                                CustomSheetValue.Collapsed    at collapsedY
                                CustomSheetValue.HalfExpanded at halfY
                                CustomSheetValue.Expanded     at expandedY
                            },
                            sheetState.currentValue
                        )
                    }
                    .offset { IntOffset(0, sheetState.draggableState.requireOffset().toInt()) }
                    .anchoredDraggable(
                        state       = sheetState.draggableState,
                        orientation = Orientation.Vertical
                    )
            ) {
                sheetContent()
            }
        }[0].measure(constraints)

        // (C) 두 개를 합쳐서 배치
        layout(layoutWidth, layoutHeight) {
            // 바텀 시트
            sheetPlaceable.placeRelative(0, 0)
            // 외부 콘텐츠
            bodyPlaceable.placeRelative(0, 0)
        }
    }
}

// 3) 사용 예시
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomBottomSheetDemo() {
    val sheetState = rememberBottomSheetState<CustomSheetValue>(
        initialValue = CustomSheetValue.Collapsed,
        defineValues = { /* 더 이상 필요 없음, onSizeChanged 에서 처리 */ }
    )
    val scope = rememberCoroutineScope()

    BlogStyleBottomSheetScaffold(
        sheetState = sheetState,
        sheetContent = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Button(onClick = {
                    scope.launch {
                        // 중간 단계로
                        sheetState.draggableState.animateTo(CustomSheetValue.HalfExpanded)
                    }
                }) {
                    Text("🚀 중간 단계로")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        // 최대한 확장
                        sheetState.draggableState.animateTo(CustomSheetValue.Expanded)
                    }
                }) {
                    Text("⬆️ 최대한 확장")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        // 접기
                        sheetState.draggableState.animateTo(CustomSheetValue.Collapsed)
                    }
                }) {
                    Text("🔽 접기")
                }
            }
        },
        content = {
            // (6) 메인 콘텐츠
            Box(
                Modifier
                    .fillMaxSize()
                    .padding()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("👋 Main Content", style = MaterialTheme.typography.bodyLarge)
            }
        }
    )
}