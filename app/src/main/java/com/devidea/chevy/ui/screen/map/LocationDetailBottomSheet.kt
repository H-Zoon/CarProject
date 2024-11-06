package com.devidea.chevy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devidea.chevy.Logger
import com.devidea.chevy.repository.remote.Document
import com.devidea.chevy.datas.navi.NavigateDocument
import com.devidea.chevy.eventbus.GuidanceStartEvent
import com.devidea.chevy.eventbus.KNAVStartEventBus
import com.devidea.chevy.viewmodel.MapViewModel
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import kotlinx.coroutines.launch

fun foundRoot(
    addressName: String,
    goalX: Double,
    goalY: Double,
    startX: Double,
    startY: Double,
    success: ((KNTrip, KNRoutePriority, Int) -> Unit),
    failure: (() -> Unit)
) {
    val startKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = startX, aWgs84Lat = startY)
    val goalKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = goalX, aWgs84Lat = goalY)

    val startKNPOI = KNPOI("", startKATEC.x.toInt(), startKATEC.y.toInt(), aAddress = null)
    val goalKNPOI = KNPOI("", goalKATEC.x.toInt(), goalKATEC.y.toInt(), addressName)

    val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
    val curAvoidOptions =
        KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value

    KNSDK.makeTripWithStart(
        aStart = startKNPOI,
        aGoal = goalKNPOI,
        aVias = null
    ) { knError, knTrip ->
        if (knError != null) {
            Logger.e { "경로 생성 에러(KNError: $knError" }
        }
        knTrip?.routeWithPriority(curRoutePriority, curAvoidOptions) { error, _ ->
            if (error != null) {
                failure()
            } else {
                success(knTrip, curRoutePriority, curAvoidOptions)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailBottomSheet(viewModel: MapViewModel, document: Document) {
    var showNaviScreen by remember { mutableStateOf(false) }
    var navigateDocument by remember { mutableStateOf<NavigateDocument?>(null) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = false
        )
    )
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 64.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                document.place_name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    foundRoot(
                        addressName = document.address_name,
                        goalX = document.x,
                        goalY = document.y,
                        startX = viewModel.userLocation.value?.longitude ?: 0.0,
                        startY = viewModel.userLocation.value?.latitude ?: 0.0,
                        success = {it, it1, it2 ->
                            coroutineScope.launch {
                                KNAVStartEventBus.post(GuidanceStartEvent.RequestNavGuidance(it, it1, it2))
                            }
                        },
                        failure = {})
                }) {
                    Text(text = "길안내")
                }
            }
        }
    ) {}
}