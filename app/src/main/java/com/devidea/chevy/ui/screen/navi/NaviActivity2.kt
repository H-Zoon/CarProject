package com.devidea.chevy.ui.screen.navi

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.devidea.chevy.Logger
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.datas.navi.NavigateDocument
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.ui.theme.CarProjectTheme
import com.devidea.chevy.viewmodel.NaviViewModel
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_CitsGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_GuideStateDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_LocationGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_RouteGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_SafetyGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_VoiceGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideState
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import com.kakaomobility.knsdk.ui.view.KNNaviView
import com.kakaomobility.knsdk.ui.view.KNNaviView_GuideStateDelegate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun NaviScreen(viewModel: NaviViewModel = hiltViewModel(), modifier: Modifier = Modifier, document: NavigateDocument) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val safetyGuide by viewModel.safetyGuide.collectAsState()
    val routeGuide by viewModel.routeGuide.collectAsState()

    AndroidView(
        factory = { context ->
            val naviView = KNNaviView(context).apply {
                sndVolume = 1f
                useDarkMode = true
                fuelType = KNCarFuel.KNCarFuel_Gasoline
                carType = KNCarType.KNCarType_1
                guideStateDelegate = MainActivity()
            }

            document.let {
                val startKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = it.startX, aWgs84Lat = it.startY)
                val goalKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = it.goalX, aWgs84Lat = it.goalY)

                val startKNPOI = KNPOI("", startKATEC.x.toInt(), startKATEC.y.toInt(), aAddress = null)
                val goalKNPOI = KNPOI("", goalKATEC.x.toInt(), goalKATEC.y.toInt(), it.address_name)

                val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
                val curAvoidOptions =
                    KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value

                KNSDK.makeTripWithStart(aStart = startKNPOI, aGoal = goalKNPOI, aVias = null) { knError, knTrip ->
                    if (knError != null) {
                        Logger.e { "경로 생성 에러(KNError: $knError" }
                    }
                    knTrip?.routeWithPriority(curRoutePriority, curAvoidOptions) { error, _ ->
                        if (error != null) {
                            Logger.e { "경로 요청 실패 : $error" }

                        } else {
                            KNSDK.sharedGuidance()?.apply {
                                naviView.initWithGuidance(
                                    this,
                                    knTrip,
                                    curRoutePriority,
                                    curAvoidOptions
                                )
                            }
                        }
                    }
                }
            }

            naviView
        },
        modifier = modifier.fillMaxSize(),
        update = { naviView ->
            // 필요 시 업데이트 로직 구현
            currentLocation?.let { location ->
                // naviView 업데이트 예시
            }

            safetyGuide?.let { guide ->
                // naviView 업데이트 예시
            }

            routeGuide?.let { guide ->
                // naviView 업데이트 예시
            }
        }
    )
}
