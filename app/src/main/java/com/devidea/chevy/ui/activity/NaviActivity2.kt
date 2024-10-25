package com.devidea.chevy.ui.activity

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
import androidx.lifecycle.lifecycleScope
import com.devidea.chevy.Logger
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.datas.navi.NavigateDocument
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec
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

@AndroidEntryPoint
class NaviActivity2 : AppCompatActivity(),
    KNGuidance_GuideStateDelegate,
    KNGuidance_SafetyGuideDelegate,
    KNGuidance_CitsGuideDelegate,
    KNGuidance_LocationGuideDelegate,
    KNGuidance_RouteGuideDelegate,
    KNGuidance_VoiceGuideDelegate,
    KNNaviView_GuideStateDelegate {

    private val viewModel: NaviViewModel by viewModels()
    private val TAG = "NaviActivity"

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            BluetoothModel.initBTModel(this@NaviActivity2)
        }

        KNSDK.sharedGuidance()?.apply {
            guideStateDelegate = this@NaviActivity2
            locationGuideDelegate = this@NaviActivity2
            routeGuideDelegate = this@NaviActivity2
            safetyGuideDelegate = this@NaviActivity2
            voiceGuideDelegate = this@NaviActivity2
            citsGuideDelegate = this@NaviActivity2
        }

        ToDeviceCodec.notifyIsNaviRunning(1)

        setContent {
            CarProjectTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Navigation") },
                            actions = {
                                IconButton(onClick = {
                                    // 로그 화면으로 이동 (필요 시 구현)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "View Logs"
                                    )
                                }
                            }
                        )
                    },
                    content = { paddingValues ->
                        NaviScreen(viewModel = viewModel, activity = this, modifier = Modifier.padding(paddingValues))
                    }
                )
            }
        }
    }

    @Composable
    fun NaviScreen(viewModel: NaviViewModel, activity: NaviActivity2, modifier: Modifier = Modifier) {
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
                    guideStateDelegate = activity
                }

                val document: NavigateDocument? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("document_key", NavigateDocument::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra("document_key")
                }

                document?.let {
                    val startKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = it.startX, aWgs84Lat = it.startY)
                    val goalKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = it.goalX, aWgs84Lat = it.goalY)

                    val startKNPOI = KNPOI("", startKATEC.x.toInt(), startKATEC.y.toInt(), aAddress = null)
                    val goalKNPOI = KNPOI("", goalKATEC.x.toInt(), goalKATEC.y.toInt(), it.address_name)

                    val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
                    val curAvoidOptions =
                        KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value

                    KNSDK.makeTripWithStart(aStart = startKNPOI, aGoal = goalKNPOI, aVias = null) { knError, knTrip ->
                        if (knError != null) {
                            Toast.makeText(activity, "경로 생성 에러(KNError: $knError", Toast.LENGTH_SHORT).show()
                        }
                        knTrip?.routeWithPriority(curRoutePriority, curAvoidOptions) { error, _ ->
                            if (error != null) {
                                Logger.d { "경로 요청 실패 : $error" }
                                Toast.makeText(activity, "경로 요청 실패 : $error", Toast.LENGTH_SHORT).show()
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

    // Delegate Methods

    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        Logger.d { "guidanceCheckingRouteChange" }
    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        Logger.d { "guidanceDidUpdateIndoorRoute" }
        // 필요한 UI 업데이트 구현
    }

    override fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        Logger.d { "guidanceDidUpdateRoutes" }
    }

    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        Logger.d { "guidanceGuideEnded" }
    }

    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        Logger.d { "guidanceGuideStarted" }
    }

    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        Logger.d { "guidanceOutOfRoute" }
    }

    override fun guidanceRouteChanged(
        aGuidance: KNGuidance,
        aFromRoute: KNRoute,
        aFromLocation: KNLocation,
        aToRoute: KNRoute,
        aToLocation: KNLocation,
        aChangeReason: KNGuideRouteChangeReason
    ) {
        Logger.d { "guidanceRouteChanged" }
    }

    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        Logger.d { "guidanceRouteUnchanged" }
    }

    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        Logger.d { "guidanceRouteUnchangedWithError" }
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        Logger.d { "didUpdateCitsGuide" }
    }

    override fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        Logger.d { "guidanceDidUpdateLocation" }
        aLocationGuide.location?.let { viewModel.updateCurrentLocation(it) }
    }

    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        Logger.d { "guidanceDidUpdateRouteGuide" }
        viewModel.updateRouteGuide(aRouteGuide)
    }

    override fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        Logger.d { "guidanceDidUpdateSafetyGuide" }
        viewModel.updateSafetyGuide(aSafetyGuide)
    }

    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        Logger.d { "guidanceDidUpdateAroundSafeties" }
        safeties?.forEach { safety ->
            Toast.makeText(
                this,
                "AroundSafety Safety type: ${safety.safetyType()}, description: ${safety.isOnStraightWay()}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ): Boolean {
        Logger.d { "shouldPlayVoiceGuide" }
        return true
    }

    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        Logger.d { "willPlayVoiceGuide" }
    }

    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        Logger.d { "didFinishPlayVoiceGuide" }
    }

    override fun naviViewGuideEnded() {
        Toast.makeText(this, "naviViewGuideEnded", Toast.LENGTH_SHORT).show()
        // 가이드 종료 로직 처리
    }

    override fun naviViewGuideState(state: KNGuideState) {
        Toast.makeText(this, "$state", Toast.LENGTH_SHORT).show()
    }
}