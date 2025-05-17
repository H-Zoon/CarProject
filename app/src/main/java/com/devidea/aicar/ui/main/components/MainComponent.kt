package com.devidea.aicar.ui.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.ui.main.CarBottomNavBar
import com.devidea.aicar.ui.main.NavItem
import com.devidea.aicar.ui.main.viewmodels.MainViewModel

const val TAG = "MainViewComponent"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: MainViewModel = hiltViewModel()) {
    val driveHistoryEnable by viewModel.driveHistoryEnable.collectAsState()
    val lastConnectionTime by viewModel.lastConnectDate.collectAsState()
    var notification by remember { mutableStateOf(false) }

    if (notification) NotificationScreen(onBack = {})

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Car", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { notification = true }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            FuelGauge(currentPercent = 0.76f)
            Spacer(modifier = Modifier.height(24.dp))
            BluetoothActionComponent()
            Spacer(modifier = Modifier.height(24.dp))
            DrivingRecordToggle(
                isEnabled = driveHistoryEnable,
                onToggle = { viewModel.setDrivingHistory(enabled = it) })
            Spacer(modifier = Modifier.height(24.dp))
            MaintenanceSection(
                items = listOf(
                    MaintenanceItem("마지막 연결시간", lastConnectionTime),
                    //MaintenanceItem("마지막 평군 연비", lastAverageEfficiency)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            TripHistorySection(
                trips = listOf(
                    TripItem("Apr 20", "58.3 km", "Seoul"),
                    TripItem("Apr 15", "231.6 km", "Incheon")
                )
            )
        }
    }
}

@Composable
fun FuelGauge(currentPercent: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            progress = { currentPercent },
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 12.dp
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(currentPercent * 100).toInt()}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Fuel",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DrivingRecordToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "주행 기록"
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

data class MaintenanceItem(val title: String, val subtitle: String)

@Composable
fun MaintenanceSection(items: List<MaintenanceItem>) {
    Text(
        text = "연결기록",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column {
        items.forEach { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class TripItem(val date: String, val distance: String, val location: String)

@Composable
fun TripHistorySection(trips: List<TripItem>) {
    Text(
        text = "Trip History",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column {
        trips.forEach { trip ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trip.date,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = trip.distance,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trip.location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


