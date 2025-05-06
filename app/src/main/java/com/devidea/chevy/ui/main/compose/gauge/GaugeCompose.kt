package com.devidea.chevy.ui.main.compose.gauge

import android.util.Log
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
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
import com.devidea.chevy.bluetooth.DefaultPid
import com.devidea.chevy.ui.main.MainViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
        viewModel.startObserving(DefaultPid.RPM)
        onDispose { viewModel.stopObserving(DefaultPid.RPM) }
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
        viewModel.startObserving(DefaultPid.SPEED)
        onDispose { viewModel.stopObserving(DefaultPid.SPEED) }
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
        viewModel.startObserving(DefaultPid.ECT)
        onDispose { viewModel.stopObserving(DefaultPid.ECT) }
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
        viewModel.startObserving(DefaultPid.THROTTLE)
        onDispose { viewModel.stopObserving(DefaultPid.THROTTLE) }
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
        viewModel.startObserving(DefaultPid.LOAD)
        onDispose { viewModel.stopObserving(DefaultPid.LOAD) }
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
        viewModel.startObserving(DefaultPid.IAT)
        onDispose { viewModel.stopObserving(DefaultPid.IAT) }
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
        viewModel.startObserving(DefaultPid.MAF)
        onDispose { viewModel.stopObserving(DefaultPid.MAF) }
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
        viewModel.startObserving(DefaultPid.BATT)
        onDispose { viewModel.stopObserving(DefaultPid.BATT) }
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
        viewModel.startObserving(DefaultPid.FUEL_RATE)
        onDispose { viewModel.stopObserving(DefaultPid.FUEL_RATE) }
    }

    val fr by viewModel.fuelRate.collectAsStateWithLifecycle()
    GaugeCard(
        title = "Fuel Rate",
        icon = Icons.Default.LocalGasStation,
        value = fr.toDashString(1),
        unit = "L/h"
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
    GaugeItem("fuel_rate") { FuelRateGauge() }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GaugesGrid2(
    viewModel: MainViewModel = hiltViewModel()
) {
    // 1) state: 현재 화면에 띄울 게이지 목록
    val gauges by viewModel.gauges.collectAsState()
    var list by remember(gauges) { mutableStateOf(gauges) }

    // 2) 드래그 순서 변경을 위한 상태
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(
        lazyGridState = gridState
    ) { from, to ->
        list = list.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        viewModel.swap(from = from.index, to = to.index)
    }

    // 3) AddDialog 표시 여부
    var showAddDialog by remember { mutableStateOf(false) }

    // 4) Scaffold 로 Fab 추가
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Gauge"
                )
            }
        }
    ) { paddingValues ->
        // 5) 실제 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list.size, key = { list[it].id }) { idx ->
                ReorderableItem(
                    state = reorderState,
                    key = list[idx].id
                ) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    Surface(shadowElevation = elevation) {
                        Box(
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = { /* optional */ },
                                    onDragStopped = { /* optional */ }
                                )
                                .background(Color.White)
                                .fillMaxSize()
                        ) {
                            list[idx].content()
                        }
                    }
                }
            }
        }

        // 6) 다이얼로그 호출
        if (showAddDialog) {
            AddGaugeDialog(
                allItems = gaugeItems,
                selectedIds = list.map { it.id }.toSet(),
                onAdd = { gauge ->
                    viewModel.onGaugeToggle(gauge.id)
                    showAddDialog = false
                },
                onDismiss = {
                    showAddDialog = false
                }
            )
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