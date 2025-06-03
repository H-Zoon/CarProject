package com.devidea.aicar.ui.main.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
 * 공통 게이지 카드.
 *
 * @param title    카드 상단 라벨 (e.g. "RPM", "Speed")
 * @param iconRes  아이콘 리소스 ID
 * @param value    표시할 값 (문자열)
 * @param unit      단위 문자열 (예: "km/h", "°C", "%", "L/h"), 필요 없으면 빈 문자열
 */

@Composable
fun GaugeCard(
    title: String,
    icon: ImageVector,
    value: String,
    unit: String = ""
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun RpmGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val rpm by viewModel.rpm.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "RPM",
        icon = Icons.Default.Speed,
        value = rpm.toDashString(),
        unit = "r/min"
    )
}

@Composable
fun SpeedGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val speed by viewModel.speed.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Speed",
        icon = Icons.Default.DirectionsCar,
        value = speed.toDashString(),
        unit = "km/h"
    )
}

@Composable
fun EctGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val ect by viewModel.ect.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Coolant Temp",
        icon = Icons.Default.Thermostat,
        value = ect.toDashString(),
        unit = "°C"
    )
}

@Composable
fun ThrottleGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val thr by viewModel.throttle.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Throttle",
        icon = Icons.Default.Tune,
        value = thr.toDashString(1),
        unit = "%"
    )
}

@Composable
fun LoadGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val load by viewModel.load.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Engine Load",
        icon = Icons.Default.BarChart,
        value = load.toDashString(),
        unit = "%"
    )
}

@Composable
fun IATGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val iat by viewModel.iat.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Intake Temp",
        icon = Icons.Default.DeviceThermostat,
        value = iat.toDashString(),
        unit = "°C"
    )
}

@Composable
fun MAFGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val maf by viewModel.maf.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Mass Air Flow",
        icon = Icons.Default.Air,
        value = maf.toDashString(1),
        unit = "g/s"
    )
}

@Composable
fun BatteryGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val batt by viewModel.batt.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Battery V",
        icon = Icons.Default.BatteryFull,
        value = batt.toDashString(2),
        unit = "V"
    )
}

@Composable
fun FuelRateGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val fr by viewModel.fuelRate.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Fuel Rate",
        icon = Icons.Default.LocalGasStation,
        value = fr.toDashString(1),
        unit = "L/h"
    )
}

@Composable
fun CurrentGearGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val gear by viewModel.currentGear.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Gear",
        icon = Icons.Default.TransitEnterexit, // 적절한 기어 아이콘으로 교체하세요
        value = if (gear in 1..6) gear.toString() else "N",
        unit = ""
    )
}

@Composable
fun OilPressureGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val psi by viewModel.oilPressure.collectAsStateWithLifecycle(initialValue = 0f)
    GaugeCard(
        title = "Oil Pressure",
        icon = Icons.Default.LocalGasStation,
        value = psi.toDashString(),
        unit = "psi"
    )
}

@Composable
fun OilTempGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val temp by viewModel.oilTemp.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Oil Temp",
        icon = Icons.Default.Thermostat,
        value = temp.toDashString(),
        unit = "°C"
    )
}

@Composable
fun TransFluidTempGauge(viewModel: DashBoardViewModel = hiltViewModel()) {
    val tft by viewModel.transFluidTemp.collectAsStateWithLifecycle(initialValue = 0)
    GaugeCard(
        title = "Trans Fluid Temp",
        icon = Icons.Default.Thermostat,
        value = tft.toDashString(),
        unit = "°C"
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
        icon = Icons.Filled.Speed,     // RPM용 예시 아이콘
        content = { RpmGauge() }
    ),
    GaugeItem(
        id = PIDs.SPEED.name,
        icon = Icons.Filled.Speed,     // Speed용 아이콘 (원한다면 다른 아이콘으로 교체)
        content = { SpeedGauge() }
    ),
    GaugeItem(
        id = PIDs.ECT.name,
        icon = Icons.Filled.Thermostat, // ECT(엔진 냉각수 온도)용 예시 아이콘
        content = { EctGauge() }
    ),
    GaugeItem(
        id = PIDs.THROTTLE.name,
        icon = Icons.Filled.GasMeter,  // 스로틀 위치용 아이콘
        content = { ThrottleGauge() }
    ),
    GaugeItem(
        id = PIDs.ENGIN_LOAD.name,
        icon = Icons.Filled.Speed,     // Engine Load용 아이콘 (원한다면 로드 아이콘으로 교체)
        content = { LoadGauge() }
    ),
    GaugeItem(
        id = PIDs.INTAKE_TEMP.name,
        icon = Icons.Filled.Thermostat, // IAT(Intake Air Temp)용 아이콘
        content = { IATGauge() }
    ),
    GaugeItem(
        id = PIDs.MAF.name,
        icon = Icons.Filled.Speed,     // MAF용 아이콘 (예시)
        content = { MAFGauge() }
    ),
    GaugeItem(
        id = PIDs.BATT.name,
        icon = Icons.Filled.BatteryChargingFull, // 배터리 전압용 아이콘
        content = { BatteryGauge() }
    ),
    GaugeItem(
        id = PIDs.CURRENT_GEAR.name,
        icon = Icons.Filled.Speed,     // 기어 표시용 아이콘 (원한다면 기어 아이콘으로 교체)
        content = { CurrentGearGauge() }
    ),
    GaugeItem(
        id = PIDs.OIL_PRESSURE.name,
        icon = Icons.Filled.Speed,     // 오일 압력용 아이콘 (예시)
        content = { OilPressureGauge() }
    ),
    GaugeItem(
        id = PIDs.OIL_TEMP.name,
        icon = Icons.Filled.Thermostat, // 오일 온도용 아이콘
        content = { OilTempGauge() }
    ),
    GaugeItem(
        id = PIDs.TRANS_FLUID_TEMP.name,
        icon = Icons.Filled.Thermostat, // 변속기 오일 온도용 아이콘
        content = { TransFluidTempGauge() }
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
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

    // 윈도우 좌표 기준 마지막 포인터 위치 (로컬 y → 윈도우 y로 변환해줄 예정)
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
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        // 1) 툴바 높이만큼의 top padding을 기억해 둡니다.
        val topBarPaddingPx = with(LocalDensity.current) {
            paddingValues.calculateTopPadding().toPx()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                // 툴바 높이만큼 아래로 내려놓음
                .padding(top = paddingValues.calculateTopPadding())
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            // 2) event.changes.first().position 은 Box 내부 로컬 좌표
                            //    이를 윈도우 좌표로 바꾸려면 y에 topBarPaddingPx를 더해주면 된다.
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
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    // 하단에 삭제 영역 높이(100.dp) + 16.dp 패딩 포함
                    bottom = 16.dp + 100.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(list, key = { _, it -> it.id }) { _, gauge ->
                    ReorderableItem(state = reorderState, key = gauge.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                        Surface(shadowElevation = elevation) {
                            Box(
                                modifier = Modifier
                                    .longPressDraggableHandle(
                                        onDragStarted = { /*...*/ },
                                        onDragStopped = {
                                            // deleteZoneRect와 비교할 때, lastPointer는 이미 윈도우 좌표가 됨
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

            // FAB을 우측 하단에 배치
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Gauge")
            }

            // 드래그 중일 때만 보이는 삭제 영역
            if (reorderState.isAnyItemDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Color.Red.copy(
                                alpha = if (deleteZoneRect.value?.contains(lastPointer) == true) 0.8f else 0.4f
                            )
                        )
                        .onGloballyPositioned { coords ->
                            // 3) 여기는 윈도우 좌표로 저장됨
                            deleteZoneRect.value = coords.boundsInWindow()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Drag here to delete",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
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
        // 플랫폼 기본 너비 제한 해제
        properties = DialogProperties(usePlatformDefaultWidth = false),
        // 외부 modifier는 여기만 사용
        modifier = modifier
            .widthIn(min = 300.dp, max = 360.dp)
            .padding(horizontal = 24.dp),

        title = {
            Text(
                text = "Add Gauge",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            // ➌ 내부 전용 Modifier는 항상 새로 생성
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(
                        count = available.size,
                        key = { index -> available[index].id }
                    ) { idx ->
                        val item = available[idx]
                        Card(
                            modifier = Modifier
                                .size(120.dp)
                                .clickable {
                                    onAdd(item)
                                    onDismiss()
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = "${item.id} icon",
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.id.uppercase(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(12.dp)
    )
}