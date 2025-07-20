package com.devidea.aicar.ui.main.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.R
import com.devidea.aicar.service.ScannedDevice
import com.devidea.aicar.ui.main.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lastDevice by viewModel.lastSavedDevice.collectAsState(initial = null)
    val autoOnCharge by viewModel.autoConnectOnCharge.collectAsState(initial = false)
    val fuelCost by viewModel.fuelCost.collectAsState(initial = 0)
    var showConfirm by remember { mutableStateOf<ResetTarget?>(null) }
    var showFuelDialog by remember { mutableStateOf(false) }
    var newFuelCostText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_setting),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
        ) {
            // ───────────── 연결 설정 ─────────────
            item {
                SettingSectionHeader(title = "연결 설정", icon = Icons.Outlined.Bluetooth)
            }

            // 마지막 연결된 기기 정보
            item {
                LastDeviceCard(lastDevice = lastDevice)
            }

            // 충전 감지 시 자동 연결 설정
            item {
                AutoConnectSettingCard(
                    autoOnCharge = autoOnCharge,
                    onToggle = { viewModel.setAutoConnectOnCharge(it) },
                )
            }

            // ───────────── 주행 기록 설정 ─────────────
            item {
                SettingSectionHeader(title = "주행 기록 설정", icon = Icons.Outlined.DirectionsCar)
            }

            // 유류비 설정 카드
            item {
                FuelCostCard(
                    fuelCost = fuelCost,
                    onEditClick = {
                        newFuelCostText = fuelCost.toString()
                        showFuelDialog = true
                    },
                )
            }

            // ───────────── 초기화 ─────────────
            item {
                SettingSectionHeader(title = "데이터 관리", icon = Icons.Outlined.Refresh)
            }

            item {
                ResetOptionsCard(
                    onResetClick = { showConfirm = it },
                )
            }

            // ───────────── 도움 ─────────────
            item {
                SettingSectionHeader(title = "지원", icon = Icons.Outlined.Help)
            }

            item {
                SupportCard(
                    onSuggestionClick = {
                        val email = "developsidea@gmail.com"
                        val intent =
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:$email".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "앱 건의하기")
                                putExtra(Intent.EXTRA_TEXT, "AICAR")
                            }
                        val chooser = Intent.createChooser(intent, "앱 건의하기")
                        try {
                            context.startActivity(chooser)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "이메일 앱이 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }

        // 확인 다이얼로그
        showConfirm?.let { target ->
            ConfirmationDialog(
                target = target,
                onConfirm = {
                    when (target) {
                        ResetTarget.SAVED_DEVICE -> viewModel.resetSavedDevice()
                        ResetTarget.DRIVING_RECORD -> viewModel.resetDrivingRecord()
                        ResetTarget.WIDGET_SETTINGS -> viewModel.resetWidgetSettings()
                        ResetTarget.ALL -> viewModel.resetAll()
                    }
                    showConfirm = null
                },
                onDismiss = { showConfirm = null },
            )
        }

        // 유류비 수정 다이얼로그
        if (showFuelDialog) {
            FuelCostDialog(
                currentValue = newFuelCostText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 4) {
                        newFuelCostText = input
                    }
                },
                onSave = {
                    val cost = newFuelCostText.toIntOrNull() ?: 0
                    viewModel.setFuelCost(cost)
                    showFuelDialog = false
                },
                onDismiss = { showFuelDialog = false },
            )
        }
    }
}

@Composable
private fun SettingSectionHeader(
    title: String,
    icon: ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                ),
        )
    }
}

@Composable
private fun LastDeviceCard(lastDevice: ScannedDevice?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color =
                    if (lastDevice != null) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (lastDevice != null) Icons.Filled.Bluetooth else Icons.Outlined.BluetoothDisabled,
                    contentDescription = null,
                    tint =
                        if (lastDevice != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.padding(12.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "마지막 연결 기기",
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (lastDevice != null) {
                    Text(
                        text = lastDevice.name,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                    )
                    Text(
                        text = lastDevice.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "저장된 기기가 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoConnectSettingCard(
    autoOnCharge: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(12.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "충전 시 자동 연결",
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "기기 충전시 마지막 기기로 자동 연결합니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = autoOnCharge,
                onCheckedChange = onToggle,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            )
        }
    }
}

@Composable
private fun FuelCostCard(
    fuelCost: Int,
    onEditClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalGasStation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "유류비 설정",
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${fuelCost}원/L",
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                    )
                }
            }

            FilledTonalButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("유류비 수정")
            }
        }
    }
}

@Composable
private fun ResetOptionsCard(onResetClick: (ResetTarget) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResetOptionItem(
                title = "저장된 블루투스 기기",
                description = "연결된 기기 정보를 삭제합니다",
                icon = Icons.Outlined.BluetoothDisabled,
                onClick = { onResetClick(ResetTarget.SAVED_DEVICE) },
            )

            ResetOptionItem(
                title = "주행 기록",
                description = "모든 주행 데이터를 삭제합니다",
                icon = Icons.Outlined.DeleteOutline,
                onClick = { onResetClick(ResetTarget.DRIVING_RECORD) },
            )

            ResetOptionItem(
                title = "위젯 설정",
                description = "위젯 순서를 기본값으로 복원합니다",
                icon = Icons.Outlined.Widgets,
                onClick = { onResetClick(ResetTarget.WIDGET_SETTINGS) },
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            ResetOptionItem(
                title = "전체 설정 초기화",
                description = "모든 데이터와 설정을 삭제합니다",
                icon = Icons.Outlined.RestartAlt,
                onClick = { onResetClick(ResetTarget.ALL) },
                isDestructive = true,
            )
        }
    }
}

@Composable
private fun ResetOptionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color =
            if (isDestructive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(24.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color =
                                if (isDestructive) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        ),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isDestructive) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SupportCard(onSuggestionClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "개발자에게 연락하기",
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "버그 신고나 기능 제안을 보내주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onSuggestionClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("건의하기")
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    target: ResetTarget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = "초기화 확인",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
            )
        },
        text = {
            Text(
                text =
                    when (target) {
                        ResetTarget.SAVED_DEVICE -> "저장된 블루투스 기기를 정말 삭제하시겠습니까?"
                        ResetTarget.DRIVING_RECORD -> "모든 주행 기록을 정말 삭제하시겠습니까?"
                        ResetTarget.WIDGET_SETTINGS -> "위젯 순서를 기본값으로 되돌리시겠습니까?"
                        ResetTarget.ALL -> "모든 설정을 초기화하시겠습니까?\n이 작업은 되돌릴 수 없습니다."
                    },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            FilledTonalButton(onClick = onConfirm) {
                Text("초기화")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

@Composable
private fun FuelCostDialog(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "유류비 수정",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "유류비를 입력하세요 (최대 4자리)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text("원/L") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            FilledIconButton(onClick = onSave) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

private enum class ResetTarget {
    SAVED_DEVICE,
    DRIVING_RECORD,
    WIDGET_SETTINGS,
    ALL,
}
