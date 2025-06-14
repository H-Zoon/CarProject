package com.devidea.aicar.ui.main.components

import android.R.attr.alpha
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TransitEnterexit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.devidea.aicar.R
import com.devidea.aicar.drive.PIDs
import com.devidea.aicar.ui.main.viewmodels.DashBoardViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/* ---------- 공통 포맷터 ---------- */
private fun Int.toDashString() = if (this == 0) "--" else toString()
private fun Float.toDashString(decimals: Int = 1): String =
    if (this == 0f) "--" else "%.${decimals}f".format(this)

/**
 * 현대적인 게이지 카드 - 글래스모피즘과 그라데이션 효과 적용
 *
 * @param title    카드 상단 라벨 (e.g. "RPM", "Speed")
 * @param icon     아이콘 ImageVector
 * @param value    표시할 값 (문자열)
 * @param unit      단위 문자열 (예: "km/h", "°C", "%", "L/h"), 필요 없으면 빈 문자열
 * @param color    테마 색상
 */
@Composable
fun GaugeCard(
    title: String,
    icon: ImageVector,
    value: String,
    unit: String = "",
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            color.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 타이틀
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(12.dp))

                // 아이콘과 값을 담는 Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 아이콘 배경 원형
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        color.copy(alpha = 0.2f),
                                        color.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier
                                .size(24.dp),
                            tint = color
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // 값과 단위
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = value,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,

                        )

                        if (unit.isNotEmpty()) {
                            Text(
                                text = unit,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),

                            )
                        }
                    }
                }
            }
        }
    }
}

// 개별 게이지들 - 각각에 고유한 색상 적용
@Composable
fun RpmGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val rpm by viewModel.rpm.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "RPM",
        icon = Icons.Default.Speed,
        value = rpm.toDashString(),
        unit = "r/min",
        color = Color(0xFF6366F1) // Indigo
    )
}

@Composable
fun SpeedGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val speed by viewModel.speed.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Speed",
        icon = Icons.Default.DirectionsCar,
        value = speed.toDashString(),
        unit = "km/h",
        color = Color(0xFF06B6D4) // Cyan
    )
}

@Composable
fun EctGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val ect by viewModel.ect.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Coolant Temp",
        icon = Icons.Default.Thermostat,
        value = ect.toDashString(),
        unit = "°C",
        color = Color(0xFF3B82F6) // Blue
    )
}

@Composable
fun ThrottleGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val thr by viewModel.throttle.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Throttle",
        icon = Icons.Default.Tune,
        value = thr.toDashString(1),
        unit = "%",
        color = Color(0xFF10B981) // Emerald
    )
}

@Composable
fun LoadGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val load by viewModel.load.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Engine Load",
        icon = Icons.Default.BarChart,
        value = load.toDashString(),
        unit = "%",
        color = Color(0xFF8B5CF6) // Violet
    )
}

@Composable
fun IATGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val iat by viewModel.iat.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Intake Temp",
        icon = Icons.Default.DeviceThermostat,
        value = iat.toDashString(),
        unit = "°C",
        color = Color(0xFFF59E0B) // Amber
    )
}

@Composable
fun MAFGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val maf by viewModel.maf.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Mass Air Flow",
        icon = Icons.Default.Air,
        value = maf.toDashString(1),
        unit = "g/s",
        color = Color(0xFF06B6D4) // Cyan
    )
}

@Composable
fun BatteryGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val batt by viewModel.batt.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Battery V",
        icon = Icons.Default.BatteryFull,
        value = batt.toDashString(2),
        unit = "V",
        color = Color(0xFF22C55E) // Green
    )
}

@Composable
fun FuelRateGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val fr by viewModel.fuelRate.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Fuel Rate",
        icon = Icons.Default.LocalGasStation,
        value = fr.toDashString(1),
        unit = "L/h",
        color = Color(0xFFEF4444) // Red
    )
}

@Composable
fun CurrentGearGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val gear by viewModel.currentGear.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Gear",
        icon = Icons.Default.TransitEnterexit,
        value = if (gear in 1..6) gear.toString() else "N",
        unit = "",
        color = Color(0xFF8B5CF6) // Violet
    )
}

@Composable
fun OilPressureGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val psi by viewModel.oilPressure.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Oil Pressure",
        icon = Icons.Default.LocalGasStation,
        value = psi.toDashString(),
        unit = "psi",
        color = Color(0xFFF97316) // Orange
    )
}

@Composable
fun OilTempGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val temp by viewModel.oilTemp.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Oil Temp",
        icon = Icons.Default.Thermostat,
        value = temp.toDashString(),
        unit = "°C",
        color = Color(0xFFDC2626) // Red
    )
}

@Composable
fun TransFluidTempGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val tft by viewModel.transFluidTemp.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Trans Fluid Temp",
        icon = Icons.Default.Thermostat,
        value = tft.toDashString(),
        unit = "°C",
        color = Color(0xFF7C3AED) // Purple
    )
}

typealias GaugeComposable = @Composable () -> Unit

data class GaugeItem(
    val id: String,
    val icon: ImageVector,
    val content: GaugeComposable,
)

val gaugeItems: List<GaugeItem> = listOf(
    GaugeItem(
        id = PIDs.RPM.name,
        icon = Icons.Filled.Speed,
        content = { RpmGauge() }
    ),
    GaugeItem(
        id = PIDs.SPEED.name,
        icon = Icons.Filled.Speed,
        content = { SpeedGauge() }
    ),
    GaugeItem(
        id = PIDs.ECT.name,
        icon = Icons.Filled.Thermostat,
        content = { EctGauge() }
    ),
    GaugeItem(
        id = PIDs.THROTTLE.name,
        icon = Icons.Filled.GasMeter,
        content = { ThrottleGauge() }
    ),
    GaugeItem(
        id = PIDs.ENGIN_LOAD.name,
        icon = Icons.Filled.Speed,
        content = { LoadGauge() }
    ),
    GaugeItem(
        id = PIDs.INTAKE_TEMP.name,
        icon = Icons.Filled.Thermostat,
        content = { IATGauge() }
    ),
    GaugeItem(
        id = PIDs.MAF.name,
        icon = Icons.Filled.Speed,
        content = { MAFGauge() }
    ),
    GaugeItem(
        id = PIDs.BATT.name,
        icon = Icons.Filled.BatteryChargingFull,
        content = { BatteryGauge() }
    ),
    GaugeItem(
        id = PIDs.CURRENT_GEAR.name,
        icon = Icons.Filled.Speed,
        content = { CurrentGearGauge() }
    ),
    GaugeItem(
        id = PIDs.OIL_PRESSURE.name,
        icon = Icons.Filled.Speed,
        content = { OilPressureGauge() }
    ),
    GaugeItem(
        id = PIDs.OIL_TEMP.name,
        icon = Icons.Filled.Thermostat,
        content = { OilTempGauge() }
    ),
    GaugeItem(
        id = PIDs.TRANS_FLUID_TEMP.name,
        icon = Icons.Filled.Thermostat,
        content = { TransFluidTempGauge() }
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenCard(
    modifier: Modifier = Modifier,
    viewModel: DashBoardViewModel = hiltViewModel()
) {
    // 해당 화면에 진입하면 폴링 시작, 떠나면 폴링 종료
    DisposableEffect(Unit) {
        viewModel.startPalling()
        onDispose {
            viewModel.stopPalling()
        }
    }

    val gauges by viewModel.gauges.collectAsState()
    var list by remember(gauges) { mutableStateOf(gauges) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 윈도우 좌표 기준 마지막 포인터 위치
    var lastPointer by remember { mutableStateOf(Offset.Zero) }
    // 윈도우 좌표 기준 삭제 영역 Rect
    val deleteZoneRect = remember { mutableStateOf<Rect?>(null) }

    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(
        lazyGridState = gridState,
        onMove = { from, to ->
            list = list.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            viewModel.swap(from = from.index, to = to.index)
        }
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_manage),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                modifier = Modifier.shadow(
                    elevation = 4.dp,
                    shape = RectangleShape,
                    clip = false
                )
            )
        }
    ) { paddingValues ->
        val topBarPaddingPx = with(LocalDensity.current) {
            paddingValues.calculateTopPadding().toPx()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(top = paddingValues.calculateTopPadding())
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val local = event.changes.first().position
                            lastPointer = Offset(local.x, local.y + topBarPaddingPx)
                        }
                    }
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp + 100.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(list, key = { _, it -> it.id }) { _, gauge ->
                    ReorderableItem(state = reorderState, key = gauge.id) { isDragging ->
                        val elevation by animateDpAsState(
                            if (isDragging) 16.dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        val scale by animateFloatAsState(
                            if (isDragging) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )

                        Surface(
                            shadowElevation = elevation,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .longPressDraggableHandle(
                                        onDragStarted = { /*...*/ },
                                        onDragStopped = {
                                            deleteZoneRect.value
                                                ?.takeIf { it.contains(lastPointer) }
                                                ?.let { viewModel.onGaugeToggle(gauge.id) }
                                        }
                                    )
                            ) {
                                gauge.content()
                            }
                        }
                    }
                }
            }

            // Modern FAB with gradient
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(64.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Gauge",
                    modifier = Modifier.size(28.dp)
                )
            }

            // 모던한 삭제 영역
            if (reorderState.isAnyItemDragging) {
                val deleteZoneAlpha by animateFloatAsState(
                    if (deleteZoneRect.value?.contains(lastPointer) == true) 0.9f else 0.6f
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Red.copy(alpha = deleteZoneAlpha)
                                )
                            )
                        )
                        .onGloballyPositioned { coords ->
                            deleteZoneRect.value = coords.boundsInWindow()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Drag here to delete",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Drop to Remove",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 추가 다이얼로그
        if (showAddDialog) {
            AddGaugeDialog(
                allItems = gaugeItems,
                selectedIds = list.map { it.id }.toSet(),
                onAdd = { gauge ->
                    viewModel.onGaugeToggle(gauge.id)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
fun AddGaugeDialog(
    modifier: Modifier = Modifier,
    allItems: List<GaugeItem>,
    selectedIds: Set<String>,
    onAdd: (GaugeItem) -> Unit,
    onDismiss: () -> Unit
) {
    val available = remember(allItems, selectedIds) {
        allItems.filter { it.id !in selectedIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .widthIn(min = 320.dp, max = 400.dp)
            .padding(horizontal = 24.dp),
        title = {
            Text(
                text = "Add Gauge",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(
                        count = available.size,
                        key = { index -> available[index].id }
                    ) { idx ->
                        val item = available[idx]
                        val cardColors = listOf(
                            Color(0xFF6366F1), Color(0xFF06B6D4), Color(0xFF3B82F6),
                            Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFFF59E0B),
                            Color(0xFF22C55E), Color(0xFFEF4444), Color(0xFFF97316),
                            Color(0xFFDC2626), Color(0xFF7C3AED)
                        )
                        val cardColor = cardColors[idx % cardColors.size]

                        Card(
                            modifier = Modifier
                                .size(140.dp)
                                .clickable {
                                    onAdd(item)
                                    onDismiss()
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                cardColor.copy(alpha = 0.15f),
                                                cardColor.copy(alpha = 0.05f)
                                            )
                                        )
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                cardColor.copy(alpha = 0.2f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = "${item.id} icon",
                                            modifier = Modifier.size(24.dp),
                                            tint = cardColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.id.replace("_", " "),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Cancel",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    )
}