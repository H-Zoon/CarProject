package com.devidea.chevy

import com.devidea.chevy.viewmodel.CarViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.carsystem.OBDData
import com.devidea.chevy.codec.ToureDevCodec.sendStartFastDetect

@Composable
fun CarStatusScreen(carViewModel: CarViewModel = hiltViewModel()) {
    val obdData by carViewModel.obdData.collectAsState()
    val leftFront by carViewModel.leftFront.collectAsState()
    val rightFront by carViewModel.rightFront.collectAsState()
    val leftRear by carViewModel.leftRear.collectAsState()
    val rightRear by carViewModel.rightRear.collectAsState()
    val trunk by carViewModel.trunk.collectAsState()
    val mRotate by carViewModel.mRotate.collectAsState()
    val mRemainGas by carViewModel.mRemainGas.collectAsState()
    val mMileage by carViewModel.mMileage.collectAsState()
    val mHandBrake by carViewModel.mHandBrake.collectAsState()
    val mSeatbelt by carViewModel.mSeatbelt.collectAsState()
    val mGear by carViewModel.mGear.collectAsState()
    val mGearNum by carViewModel.mGearNum.collectAsState()
    val mErrCount by carViewModel.mErrCount.collectAsState()
    val mVoltage by carViewModel.mVoltage.collectAsState()
    val mSolarTermDoor by carViewModel.mSolarTermDoor.collectAsState()
    val mWaterTemperature by carViewModel.mWaterTemperature.collectAsState()
    val mThreeCatalystTemperatureBankOne1 by carViewModel.mThreeCatalystTemperatureBankOne1.collectAsState()
    val mThreeCatalystTemperatureBankOne2 by carViewModel.mThreeCatalystTemperatureBankOne2.collectAsState()
    val mThreeCatalystTemperatureBankTwo1 by carViewModel.mThreeCatalystTemperatureBankTwo1.collectAsState()
    val mThreeCatalystTemperatureBankTwo2 by carViewModel.mThreeCatalystTemperatureBankTwo2.collectAsState()
    val mMeterVoltage by carViewModel.mMeterVoltage.collectAsState()
    val mMeterRotate by carViewModel.mMeterRotate.collectAsState()
    val mSpeed by carViewModel.mSpeed.collectAsState()
    val mTemp by carViewModel.mTemp.collectAsState()
    val mGearFunOnoffVisible by carViewModel.mGearFunOnoffVisible.collectAsState()
    val mGearDLock by carViewModel.mGearDLock.collectAsState()
    val mGearPUnlock by carViewModel.mGearPUnlock.collectAsState()
    val carVIN by carViewModel.carVIN.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    LaunchedEffect(obdData) {
        if (obdData != null) {
            dialogMessage = displayObdData(obdData)
            showDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        item {
            Text(text = "Car Status", style = MaterialTheme.typography.titleLarge)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        item { DoorStatus("Left Front Door", leftFront) }
        item { DoorStatus("Right Front Door", rightFront) }
        item { DoorStatus("Left Rear Door", leftRear) }
        item { DoorStatus("Right Rear Door", rightRear) }
        item { DoorStatus("Trunk", trunk) }
        item { CarInfo("Rotate", mRotate) }
        item { CarInfo("Remaining Gas", mRemainGas) }
        item { CarInfo("Mileage", mMileage) }
        item { CarInfo("Hand Brake", mHandBrake) }
        item { CarInfo("Seatbelt", mSeatbelt) }
        item { CarInfo("Gear", mGear) }
        item { CarInfo("Gear Number", mGearNum) }
        item { CarInfo("Error Count", mErrCount) }
        item { CarInfo("Voltage", mVoltage) }
        item { CarInfo("Solar Term Door", mSolarTermDoor) }
        item { CarInfo("Water Temperature", mWaterTemperature) }
        item { CarInfo("Three Catalyst Temperature Bank One 1", mThreeCatalystTemperatureBankOne1) }
        item { CarInfo("Three Catalyst Temperature Bank One 2", mThreeCatalystTemperatureBankOne2) }
        item { CarInfo("Three Catalyst Temperature Bank Two 1", mThreeCatalystTemperatureBankTwo1) }
        item { CarInfo("Three Catalyst Temperature Bank Two 2", mThreeCatalystTemperatureBankTwo2) }
        item { CarInfo("Meter Voltage", mMeterVoltage) }
        item { CarInfo("Meter Rotate", mMeterRotate) }
        item { CarInfo("Speed", mSpeed) }
        item { CarInfo("Temperature", mTemp) }
        item { CarInfo("Gear Fun On/Off Visible", mGearFunOnoffVisible) }
        item { CarInfo("Gear D Lock", mGearDLock) }
        item { CarInfo("Gear P Unlock", mGearPUnlock) }
        item { CarInfo("Car VIN", carVIN) }

        item {
            Button(
                onClick = { sendStartFastDetect() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Refresh Data")
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("OBD Data") },
            text = { Text(dialogMessage) },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun DoorStatus(label: String, state: CarEventModule.DoorState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(text = state.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CarInfo(label: String, value: Any) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

fun displayObdData(obdData: OBDData): String {
    val builder = StringBuilder()
    for (pid in obdData.getPidDataList()) {
        builder.append("${pid.strName}: ${pid.strValue}\n")
    }
    return builder.toString()
}