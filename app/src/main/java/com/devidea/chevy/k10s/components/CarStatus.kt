package com.devidea.chevy.k10s.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.devidea.chevy.k10s.dashboard.CarViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.chevy.k10s.obd.model.CarEventModel
import com.devidea.chevy.k10s.obd.protocol.pid.PIDListData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarStatus(carViewModel: CarViewModel = hiltViewModel()) {
    val obdData by carViewModel.obdData.collectAsStateWithLifecycle()
    val leftFront by carViewModel.leftFront.collectAsStateWithLifecycle()
    val rightFront by carViewModel.rightFront.collectAsStateWithLifecycle()
    val leftRear by carViewModel.leftRear.collectAsStateWithLifecycle()
    val rightRear by carViewModel.rightRear.collectAsStateWithLifecycle()
    val trunk by carViewModel.trunk.collectAsStateWithLifecycle()
    val mHandBrake by carViewModel.mHandBrake.collectAsStateWithLifecycle()
    val mSeatbelt by carViewModel.mSeatbelt.collectAsStateWithLifecycle()
    val mErrCount by carViewModel.mErrCount.collectAsStateWithLifecycle()
    val carVIN by carViewModel.carVIN.collectAsStateWithLifecycle()

    val mRotate by carViewModel.mRotate.collectAsStateWithLifecycle()
    val mRemainGas by carViewModel.mRemainGas.collectAsStateWithLifecycle()
    val mMileage by carViewModel.mMileage.collectAsStateWithLifecycle()
    val mGear by carViewModel.mGear.collectAsStateWithLifecycle()
    val mGearNum by carViewModel.mGearNum.collectAsStateWithLifecycle()

    val mMeterVoltage by carViewModel.mMeterVoltage.collectAsStateWithLifecycle()
    val mMeterRotate by carViewModel.mMeterRotate.collectAsStateWithLifecycle()
    val mSpeed by carViewModel.mSpeed.collectAsStateWithLifecycle()
    val mTemp by carViewModel.mTemp.collectAsStateWithLifecycle()
    val mGearFunOnoffVisible by carViewModel.mGearFunOnoffVisible.collectAsStateWithLifecycle()
    val mGearDLock by carViewModel.mGearDLock.collectAsStateWithLifecycle()
    val mGearPUnlock by carViewModel.mGearPUnlock.collectAsStateWithLifecycle()

    val carInfoList = listOf(
        "Rotate" to mRotate,
        "Remaining Gas" to "${mRemainGas} L",
        "Mileage" to "${mMileage} km",
        "Gear" to mGear,
        "Gear Number" to mGearNum,
        "Meter Voltage" to "${mMeterVoltage} V",
        "Meter Rotate" to mMeterRotate.toString(),
        "Speed" to "${mSpeed} km/h",
        "Temperature" to "${mTemp} °C",
        "Gear Fun On/Off Visible" to mGearFunOnoffVisible.toString(),
        "Gear D Lock" to mGearDLock.toString(),
        "Gear P Unlock" to mGearPUnlock.toString(),
    )

// Group the carInfoList into pages, each containing 4 items (2 columns * 2 rows)
    val itemsPerPage = 4
    val pages = carInfoList.chunked(itemsPerPage)

// Pager 상태 초기화
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxWidth() // 부모 Column의 너비를 채우도록 설정
            .height(300.dp) // 적절한 높이를 설정 (필요에 따라 조정)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f) // 남은 공간을 채우도록 설정
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 2행으로 분할
                val rows = pages[page].chunked(2)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (label, value) ->
                            CarInfoCard(
                                label = label,
                                value = value.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // 아이템이 2개 미만일 경우 빈 공간 추가
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(16.dp)
                )
            }
        }
    }
}


@Composable
fun CarInfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    NeumorphicBox(
        modifier = modifier
            .padding(10.dp)
            .height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 20.sp,
                    color = Color.Black,
                ),
                color = MaterialTheme.colorScheme.primary

            )

            Text(
                modifier = Modifier.align(Alignment.End),
                text = value,
                style = TextStyle(
                    fontSize = 20.sp,
                    color = Color.Black,
                ),
            )
        }
    }
}


@Composable
fun DoorStatus(label: String, state: CarEventModel.DoorState) {
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


fun displayObdData(obdData: List<PIDListData>): String {
    //sendStartFastDetect()
    val builder = StringBuilder()
    for (pid in obdData) {
        builder.append("${pid.strName}: ${pid.strValue}\n")
    }
    return builder.toString()
}