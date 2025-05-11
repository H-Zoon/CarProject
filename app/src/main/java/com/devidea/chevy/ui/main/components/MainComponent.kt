package com.devidea.chevy.ui.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.ui.main.MainViewModel

const val TAG = "MainViewComponent"
// 탭 종류 정의

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    CenterAlignedTopAppBar(
        title = { Text("My Car", style = MaterialTheme.typography.titleLarge) },
        actions = {
            IconButton(onClick = { /* 알림 클릭 */ }) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}



@Composable
fun HomeScreen(viewModel: MainViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FuelGauge(currentPercent = 0.76f)
        Spacer(modifier = Modifier.height(24.dp))
        ActionButtons(viewModel)
        Spacer(modifier = Modifier.height(24.dp))
        MaintenanceSection(
            items = listOf(
                MaintenanceItem("Oil Change", "Due in 500 km"),
                MaintenanceItem("Tire Rotation", "Apr 25")
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

@Composable
fun FuelGauge(currentPercent: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            progress = currentPercent,
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape),
            strokeWidth = 12.dp,
            color = MaterialTheme.colorScheme.primary
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
fun ActionButtons(viewModel: MainViewModel) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = {  },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = "Lock")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lock")
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(
            onClick = {  },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.LockOpen, contentDescription = "Unlock")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlock")
        }
    }
}

data class MaintenanceItem(val title: String, val subtitle: String)

@Composable
fun MaintenanceSection(items: List<MaintenanceItem>) {
    Text(
        text = "Maintenance",
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


