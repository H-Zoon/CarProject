package com.devidea.chevy.ui.main.compose.gauge

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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TransitEnterexit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.chevy.ui.main.MainViewModel
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devidea.chevy.drive.PIDs
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

/* ---------- 개별 Gauge 컴포즈 ---------- */

@Composable
fun RpmGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.RPM)
        onDispose { viewModel.stopObserving(PIDs.RPM) }
    }

    val rpm by viewModel.rpm.collectAsStateWithLifecycle()
    GaugeCard(
        title = "RPM",
        icon = Icons.Default.Speed,
        value = rpm.toDashString(),
        unit = "r/min"
    )
}

@Composable
fun SpeedGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.SPEED)
        onDispose { viewModel.stopObserving(PIDs.SPEED) }
    }

    val speed by viewModel.speed.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Speed",
        icon = Icons.Default.DirectionsCar,
        value = speed.toDashString(),
        unit = "km/h"
    )
}

@Composable
fun EctGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.ECT)
        onDispose { viewModel.stopObserving(PIDs.ECT) }
    }

    val ect by viewModel.ect.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Coolant Temp",
        icon = Icons.Default.Thermostat,
        value = ect.toDashString(),
        unit = "°C"
    )
}

@Composable
fun ThrottleGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.THROTTLE)
        onDispose { viewModel.stopObserving(PIDs.THROTTLE) }
    }

    val thr by viewModel.throttle.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Throttle",
        icon = Icons.Default.Tune,
        value = thr.toDashString(),
        unit = "%"
    )
}

@Composable
fun LoadGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.LOAD)
        onDispose { viewModel.stopObserving(PIDs.LOAD) }
    }

    val load by viewModel.load.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Engine Load",
        icon = Icons.Default.BarChart,
        value = load.toDashString(),
        unit = "%"
    )
}

@Composable
fun IATGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.IAT)
        onDispose { viewModel.stopObserving(PIDs.IAT) }
    }

    val iat by viewModel.iat.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Intake Temp",
        icon = Icons.Default.DeviceThermostat,
        value = iat.toDashString(),
        unit = "°C"
    )
}

@Composable
fun MAFGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.MAF)
        onDispose { viewModel.stopObserving(PIDs.MAF) }
    }

    val maf by viewModel.maf.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Mass Air Flow",
        icon = Icons.Default.Air,
        value = maf.toDashString(1),
        unit = "g/s"
    )
}

@Composable
fun BatteryGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.BATT)
        onDispose { viewModel.stopObserving(PIDs.BATT) }
    }

    val batt by viewModel.batt.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Battery V",
        icon = Icons.Default.BatteryFull,
        value = batt.toDashString(2),
        unit = "V"
    )
}

@Composable
fun FuelRateGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.FUEL_RATE)
        onDispose { viewModel.stopObserving(PIDs.FUEL_RATE) }
    }

    val fr by viewModel.fuelRate.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Fuel Rate",
        icon = Icons.Default.LocalGasStation,
        value = fr.toDashString(1),
        unit = "L/h"
    )
}

@Composable
fun CurrentGearGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.CURRENT_GEAR)
        onDispose { viewModel.stopObserving(PIDs.CURRENT_GEAR) }
    }
    val gear by viewModel.currentGear.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Gear",
        icon = Icons.Default.TransitEnterexit, // 적절한 기어 아이콘으로 교체하세요
        value = if (gear in 1..6) gear.toString() else "N",
        unit = ""
    )
}

@Composable
fun OilPressureGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.OIL_PRESSURE)
        onDispose { viewModel.stopObserving(PIDs.OIL_PRESSURE) }
    }
    val psi by viewModel.oilPressure.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Oil Pressure",
        icon = Icons.Default.LocalGasStation,
        value = psi.toDashString(),
        unit = "psi"
    )
}

@Composable
fun OilTempGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.OIL_TEMP)
        onDispose { viewModel.stopObserving(PIDs.OIL_TEMP) }
    }
    val temp by viewModel.oilTemp.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Oil Temp",
        icon = Icons.Default.Thermostat,
        value = temp.toDashString(),
        unit = "°C"
    )
}

@Composable
fun TransFluidTempGauge(viewModel: MainViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startObserving(PIDs.TRANS_FLUID_TEMP)
        onDispose { viewModel.stopObserving(PIDs.TRANS_FLUID_TEMP) }
    }
    val tft by viewModel.transFluidTemp.collectAsStateWithLifecycle()
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
    val content: GaugeComposable
)

val gaugeItems: List<GaugeItem> = listOf(
    GaugeItem("rpm") { RpmGauge() },
    GaugeItem("speed") { SpeedGauge() },
    GaugeItem("ect") { EctGauge() },
    GaugeItem("throttle") { ThrottleGauge() },
    GaugeItem("load") { LoadGauge() },
    GaugeItem("iat") { IATGauge() },
    GaugeItem("maf") { MAFGauge() },
    GaugeItem("battery") { BatteryGauge() },
    GaugeItem("fuel_rate") { FuelRateGauge() },

    GaugeItem("current_gear")   { CurrentGearGauge() },
    GaugeItem("oil_pressure")   { OilPressureGauge() },
    GaugeItem("oil_temp")       { OilTempGauge() },
    GaugeItem("trans_fluid_temp"){ TransFluidTempGauge() }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GaugesGrid2(
    viewModel: MainViewModel = hiltViewModel()
) {
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Gauge")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // 전역 포인터 감시: 항상 마지막 위치를 기록
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            lastPointer = event.changes.first().position
                        }
                    }
                }
        ) {
            // 그리드…
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(list, key = { _, it -> it.id }) { idx, gauge ->
                    ReorderableItem(state = reorderState, key = gauge.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                        Surface(shadowElevation = elevation) {
                            Box(
                                modifier = Modifier
                                    .longPressDraggableHandle(
                                        onDragStarted = { /*…*/ },
                                        onDragStopped = {
                                            deleteZoneRect.value
                                                ?.takeIf { it.contains(lastPointer) }
                                                ?.let { viewModel.onGaugeToggle(gauge.id) }
                                        }
                                    )
                                    .background(Color.White)
                                    .fillMaxSize()
                            ) {
                                gauge.content()
                            }
                        }
                    }
                }
            }

            // “삭제 중”일 때만 보이는 삭제 영역
            if (reorderState.isAnyItemDragging) {
                // 포인터가 영역 안에 있는지 체크
                val inZone = deleteZoneRect.value?.contains(lastPointer) == true

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            // 안에 들어가면 불투명도 높이고, 아니면 낮게
                            Color.Red.copy(alpha = if (inZone) 0.8f else 0.4f)
                        )
                        .onGloballyPositioned { coords ->
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

            // AddGaugeDialog…
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
    allItems: List<GaugeItem>,
    selectedIds: Set<String>,
    onAdd: (GaugeItem) -> Unit,
    onDismiss: () -> Unit
) {
    // 1) 선택되지 않은 것들만
    val available = remember(allItems, selectedIds) {
        allItems.filter { it.id !in selectedIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Gauge",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            // 2) 상단에 가로 스크롤 LazyRow
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(available.size, key = { available[it].id }) { gauge ->
                        Card(
                            modifier = Modifier
                                .size(100.dp)
                                .clickable {
                                    onAdd(available[gauge])
                                    onDismiss()
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(modifier = Modifier.size(48.dp)) {
                                    available[gauge].content()
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = available[gauge].id.uppercase(),
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
        // 필요시 배경 모서리나 크기 조정
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
}