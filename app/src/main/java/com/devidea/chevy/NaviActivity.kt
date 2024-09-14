package com.devidea.chevy

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devidea.chevy.codec.ToDeviceCodec
import com.devidea.chevy.codec.ToDeviceCodec.sendLaneInfo
import com.devidea.chevy.databinding.ActivityNaviBinding
import com.devidea.chevy.navi.NavigationIconType
import com.devidea.chevy.navi.isCameraType
import com.devidea.chevy.response.Document
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNRGCode
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
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import com.kakaomobility.knsdk.ui.view.KNNaviView_GuideStateDelegate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

@AndroidEntryPoint
class NaviActivity : AppCompatActivity(), KNGuidance_GuideStateDelegate,
    KNGuidance_SafetyGuideDelegate, KNGuidance_CitsGuideDelegate, KNGuidance_LocationGuideDelegate,
    KNGuidance_RouteGuideDelegate, KNGuidance_VoiceGuideDelegate, KNNaviView_GuideStateDelegate {

    lateinit var binding: ActivityNaviBinding
    val TAG = "NaviActivity"

    private val _currentLocation = MutableStateFlow<KNLocation?>(null)
    val currentLocation: StateFlow<KNLocation?> = _currentLocation

    private val _routeGuide = MutableStateFlow<KNGuide_Route?>(null)
    val routeGuide: StateFlow<KNGuide_Route?> = _routeGuide

    private val _safetyGuide = MutableStateFlow<KNGuide_Safety?>(null)
    val safetyGuide: StateFlow<KNGuide_Safety?> = _safetyGuide

    init {
        KNSDK.sharedGuidance()?.apply {
            // 각 가이던스 델리게이트 등록
            guideStateDelegate = this@NaviActivity
            locationGuideDelegate = this@NaviActivity
            routeGuideDelegate = this@NaviActivity
            safetyGuideDelegate = this@NaviActivity
            voiceGuideDelegate = this@NaviActivity
            citsGuideDelegate = this@NaviActivity
        }

        CoroutineScope(Dispatchers.IO).launch {
            currentLocation.collect { location ->
                location?.let {
                    //안전운행 정보
                    val safetiesOnGuide = safetyGuide.value?.safetiesOnGuide

                    // safetiesOnGuide가 비어있거나 null인지 확인합니다.
                    if (safetiesOnGuide.isNullOrEmpty()) {
                        ToDeviceCodec.sendLimitSpeed(0, 0)
                        ToDeviceCodec.sendCameraDistance(0, 0, 0)
                        //ToDeviceCodec.sendCameraDistanceEx(0, 0, 0);
                    } else {
                        // safetiesOnGuide 리스트를 순회합니다.
                        for (safety in safetiesOnGuide) {
                            // 과속 단속 카메라 (코드 81, 82, 86, 100, 102, 103) 처리
                            if (isCameraType(safety.code.value)) {
                                val speedLimit = (safety as KNSafety_Camera).speedLimit
                                val cameraDistance = location.distToLocation(safety.location)
                                ToDeviceCodec.sendLimitSpeed(cameraDistance, speedLimit)
                                ToDeviceCodec.sendCameraDistance(cameraDistance, speedLimit, 1)
                                ToDeviceCodec.sendNextInfo(
                                    NavigationIconType.NONE.value,
                                    cameraDistance
                                )

                                //ToDeviceCodec.sendCameraDistanceEx(distance, speedLimit, 1)
                                return@collect
                            } else {
                                ToDeviceCodec.sendCameraDistance(0, 0, 0)
                                ToDeviceCodec.sendLimitSpeed(0, 0)
                            }
                        }
                    }

                    routeGuide.value?.let { routeGuide ->
                        // 목표의 거리와 진행 방향 계산
                        val distance =
                            routeGuide.curDirection?.location?.let { location.distToLocation(it) }
                                ?: 0
                        val guidanceAsset =
                            routeGuide.curDirection?.let { findGuideAsset(it.rgCode) }
                                ?: NavigationIconType.NONE

                        // 결과 전송
                        ToDeviceCodec.sendNextInfo(guidanceAsset.value, distance)

                        //차선 정보가 있다면 추천 차선 계산
                        routeGuide.lane?.laneInfos?.let {
                            val recommendArray = IntArray(it.size)

                            // 추천 차선이면 1, 아니면 0을 배열에 저장
                            for (i in it.indices) {
                                recommendArray[i] = if (it[i].suggest == 1.toByte()) 1 else 0
                            }
                            //결과 전송
                            sendLaneInfo(recommendArray)
                        }
                    } ?: run { ToDeviceCodec.sendNextInfo(0, 0) }
                } ?: return@collect
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ToDeviceCodec.notifyIsNaviRunning(1)
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        binding.naviView.sndVolume = 1f
        binding.naviView.useDarkMode = true
        binding.naviView.fuelType = KNCarFuel.KNCarFuel_Gasoline
        binding.naviView.carType = KNCarType.KNCarType_1
        binding.naviView.guideStateDelegate = this

        // API 33 이상인 경우
        val document: Document? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("document_key", Document::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("document_key")
        }

        document?.let {
            val startKatec = WGS84ToKATEC(LatLng(37.6541, 126.6825))
            val goalKatec = WGS84ToKATEC(LatLng(BigDecimal(it.y).setScale(4, RoundingMode.HALF_UP).toDouble(), BigDecimal(it.x).setScale(4, RoundingMode.HALF_UP).toDouble()))

            // 출발지 설정
            val start = KNPOI("home", startKatec.y.toInt(), startKatec.x.toInt(), "광주광역시 서구 상무중앙로78번길")
            // 목적지 설정
            val goal = KNPOI("company", goalKatec.y.toInt(), goalKatec.x.toInt(), it.address_name)

            // 경로 옵션 설정
            val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
            val curAvoidOptions =
                KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value

            KNSDK.makeTripWithStart(start, goal, null, null) { knError: KNError?, knTrip: KNTrip? ->
                if (knError != null) {
                    Toast.makeText(this, "경로 생성 에러(KNError: $knError", Toast.LENGTH_SHORT).show()
                }
                knTrip?.routeWithPriority(curRoutePriority, curAvoidOptions) { error, _ ->
                    // 경로 요청 실패
                    if (error != null) {
                        Log.d(TAG, "경로 요청 실패 : $error")
                        KNSDK.sharedGuidance()?.apply {
                            binding.naviView.initWithGuidance(
                                this,
                                null,
                                curRoutePriority,
                                curAvoidOptions
                            )
                        }
                    }
                    // 경로 요청 성공
                    else {
                        // 경로 요청 성공
                        KNSDK.sharedGuidance()?.apply {
                            binding.naviView.initWithGuidance(
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
    }

    // 경로 변경 시 호출. 교통 변화 또는 경로 이탈로 인한 재탐색 및 사용자 재탐색 시 전달
    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceCheckingRouteChange")

    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        Log.d(TAG, "guidanceDidUpdateIndoorRoute")
        binding.naviView.guidanceDidUpdateIndoorRoute(aGuidance, aRoute)
    }

    // 주행 중 기타 요인들로 인해 경로가 변경되었을 때 호출
    override fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        Log.d(TAG, "guidanceDidUpdateRoutes")
        binding.naviView.guidanceDidUpdateRoutes(aGuidance, aRoutes, aMultiRouteInfo)
    }

    // 길 안내 종료 시 호출
    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceGuideEnded")
        binding.naviView.guidanceGuideEnded(aGuidance)
    }

    // 길 안내 시작 시 호출
    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceGuideStarted")
        binding.naviView.guidanceGuideStarted(aGuidance)
    }

    // 경로에서 이탈한 뒤 새로운 경로를 요청할 때 호출
    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceOutOfRoute")
        binding.naviView.guidanceOutOfRoute(aGuidance)
    }

    // 수신 받은 새 경로가 기존의 안내된 경로와 다를 경우 호출. 여러 개의 경로가 있을 경우 첫 번째 경로를 주행 경로로 사용하고 나머지는 대안 경로로 설정됨
    override fun guidanceRouteChanged(
        aGuidance: KNGuidance,
        aFromRoute: KNRoute,
        aFromLocation: KNLocation,
        aToRoute: KNRoute,
        aToLocation: KNLocation,
        aChangeReason: KNGuideRouteChangeReason
    ) {
        Log.d(TAG, "guidanceRouteChanged")
        binding.naviView.guidanceRouteChanged(aGuidance)
    }

    // 수신 받은 새 경로가 기존의 안내된 경로와 동일할 경우 호출
    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceRouteUnchanged")
        binding.naviView.guidanceRouteUnchanged(aGuidance)
    }

    // 경로에 오류가 발생 시 호출
    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        Log.d(TAG, "guidanceRouteUnchangedWithError")
        binding.naviView.guidanceRouteUnchangedWithError(aGuidnace, aError)
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        Log.d(TAG, "didUpdateCitsGuide")
        binding.naviView.didUpdateCitsGuide(aGuidance, aCitsGuide)
    }


    // KNGuidance_LocationGuideDelegate

    // 위치 정보가 변경될 경우 호출. `locationGuide`의 항목이 1개 이상 변경 시 전달됨.
    override fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        Log.d(TAG, "guidanceDidUpdateLocation")
        binding.naviView.guidanceDidUpdateLocation(aGuidance, aLocationGuide)
        _currentLocation.value = aLocationGuide.location
    }

    // KNGuidance_RouteGuideDelegate

    // 경로 안내 정보 업데이트 시 호출. `routeGuide`의 항목이 1개 이상 변경 시 전달됨.
    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        Log.d(TAG, "guidanceDidUpdateRouteGuide")
        binding.naviView.guidanceDidUpdateRouteGuide(aGuidance, aRouteGuide)

        _routeGuide.value = aRouteGuide
    }

    // KNGuidance_SafetyGuideDelegate
    // 안전 운행 정보 업데이트 시 호출. `safetyGuide`의 항목이 1개 이상 변경 시 전달됨.
    // 안전운전 설정시 해당 함수로 이벤트 전달.
    override fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        binding.naviView.guidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide)

        _safetyGuide.value = aSafetyGuide
    }

    // 주변의 안전 운행 정보 업데이트 시 호출
    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        binding.naviView.guidanceDidUpdateAroundSafeties(guidance, safeties)

        // 업데이트된 주변 안전 정보 리스트가 null이 아닌 경우
        safeties?.let {
            for (safety in it) {
                Toast.makeText(
                    this,
                    "AroundSafety Safety type: ${safety.safetyType()}, description: ${safety.isOnStraightWay()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // KNGuidance_VoiceGuideDelegate
    // 음성 안내 사용 여부
    override fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ): Boolean {
        Log.d(TAG, "shouldPlayVoiceGuide")
        binding.naviView.shouldPlayVoiceGuide(aGuidance, aVoiceGuide, aNewData)
        return true
    }

    // 음성 안내 시작
    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        Log.d(TAG, "willPlayVoiceGuide")
        binding.naviView.willPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    // 음성 안내 종료
    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        Log.d(TAG, "didFinishPlayVoiceGuide")
        binding.naviView.didFinishPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    data class LatLng(val latitude: Double, val longitude: Double)
    data class Katech(val x: Double, val y: Double)

    fun WGS84ToKATEC(latLng: LatLng): Katech {
        // KATEC 좌표계 상수
        val a = 6378137.0 // WGS84 타원체 장축 반경 (단위: meter)
        val f = 1 / 298.257222101 // 타원체 편평률
        val e2 = f * (2 - f) // 제 1 이심률의 제곱

        // KATEC 기준
        val lon0 = 2.2166185948963 // 기준점 경도 (radian, 128.0 degrees)
        val lat0 = 0.6632251157578453 // 기준점 위도 (radian, 38.0 degrees)
        val k0 = 1.0 // 스케일 팩터
        val falseEasting = 1000000.0 // False Easting 값 (단위: meter)
        val falseNorthing = 2000000.0 // False Northing 값 (단위: meter)

        // WGS84 위도, 경도 (radians)
        val lat = latLng.latitude * Math.PI / 180.0
        val lon = latLng.longitude * Math.PI / 180.0

        // Transverse Mercator projection 공식 적용
        val n = a / sqrt(1 - e2 * sin(lat0).pow(2)) // Radius of curvature in the prime vertical
        val t = tan(lat).pow(2) // t = tan(latitude)^2
        val c = e2 / (1 - e2) * cos(lat).pow(2) // Second term of the meridian distance

        val A = (lon - lon0) * cos(lat) // Difference in longitude from the central meridian

        // M: the meridian arc length
        val m = a * ((1 - e2 / 4 - 3 * e2.pow(2) / 64 - 5 * e2.pow(3) / 256) * lat
                - (3 * e2 / 8 + 3 * e2.pow(2) / 32 + 45 * e2.pow(3) / 1024) * sin(2 * lat)
                + (15 * e2.pow(2) / 256 + 45 * e2.pow(3) / 1024) * sin(4 * lat)
                - (35 * e2.pow(3) / 3072) * sin(6 * lat))

        // X 좌표 계산
        val x = falseEasting + k0 * n * (A + (1 - t + c) * A.pow(3) / 6
                + (5 - 18 * t + t.pow(2) + 72 * c - 58 * e2) * A.pow(5) / 120)

        // Y 좌표 계산
        val y = falseNorthing + k0 * (m + n * tan(lat) * (A.pow(2) / 2
                + (5 - t + 9 * c + 4 * c.pow(2)) * A.pow(4) / 24
                + (61 - 58 * t + t.pow(2) + 600 * c - 330 * e2) * A.pow(6) / 720))

        return Katech(x, y)
    }

    fun calculateDistance(location: KNLocation, location1: KNLocation): Int {
        return sqrt(
            (location1.pos.x - location.pos.x).pow(2) + (location1.pos.y - location.pos.y).pow(
                2
            )
        ).toInt()
    }

    private fun findGuideAsset(code: KNRGCode): NavigationIconType {
        return when (code) {
            KNRGCode.KNRGCode_Start -> NavigationIconType.NONE
            KNRGCode.KNRGCode_Goal -> NavigationIconType.ARRIVED_DESTINATION
            KNRGCode.KNRGCode_Via -> NavigationIconType.ARRIVED_WAYPOINT
            KNRGCode.KNRGCode_Straight -> NavigationIconType.STRAIGHT

            // 좌회전 관련 방향
            KNRGCode.KNRGCode_LeftTurn -> NavigationIconType.LEFT

            KNRGCode.KNRGCode_LeftDirection,
            KNRGCode.KNRGCode_LeftOutHighway,
            KNRGCode.KNRGCode_LeftInHighway,
            KNRGCode.KNRGCode_LeftOutCityway,
            KNRGCode.KNRGCode_LeftInCityway,
            KNRGCode.KNRGCode_LeftStraight,
            KNRGCode.KNRGCode_ChangeLeftHighway -> NavigationIconType.LEFT_FRONT

            // 우회전 관련 방향
            KNRGCode.KNRGCode_RightTurn -> NavigationIconType.RIGHT

            KNRGCode.KNRGCode_RightDirection,
            KNRGCode.KNRGCode_RightOutHighway,
            KNRGCode.KNRGCode_RightInHighway,
            KNRGCode.KNRGCode_RightOutCityway,
            KNRGCode.KNRGCode_RightInCityway,
            KNRGCode.KNRGCode_RightStraight,
            KNRGCode.KNRGCode_ChangeRightHighway -> NavigationIconType.RIGHT_FRONT

            // 후방 방향
            KNRGCode.KNRGCode_Direction_7 -> NavigationIconType.LEFT_BACK
            KNRGCode.KNRGCode_Direction_5 -> NavigationIconType.RIGHT_BACK

            // U턴
            KNRGCode.KNRGCode_UTurn -> NavigationIconType.TURN_AROUND

            /*
            // 고속도로 출입구
            KNRGCode.KNRGCode_OutHighway -> NavigationIconType.OUT_ROUNDABOUT
            KNRGCode.KNRGCode_InHighway -> NavigationIconType.ENTER_ROUNDABOUT

            // 페리 항로
            KNRGCode.KNRGCode_InFerry -> NavigationIconType.ENTER_ROUNDABOUT
            KNRGCode.KNRGCode_OutFerry -> NavigationIconType.OUT_ROUNDABOUT
             */

            // 터널 및 톨게이트
            KNRGCode.KNRGCode_OverPath -> NavigationIconType.ARRIVED_TUNNEL
            KNRGCode.KNRGCode_Tollgate -> NavigationIconType.ARRIVED_TOLLGATE
            KNRGCode.KNRGCode_NonstopTollgate -> NavigationIconType.ARRIVED_TOLLGATE
            KNRGCode.KNRGCode_JoinAfterBranch -> NavigationIconType.ARRIVED_SERVICE_AREA

            // 로터리 방향
            KNRGCode.KNRGCode_RotaryDirection_1,
            KNRGCode.KNRGCode_RotaryDirection_2,
            KNRGCode.KNRGCode_RotaryDirection_3,
            KNRGCode.KNRGCode_RotaryDirection_4,
            KNRGCode.KNRGCode_RotaryDirection_5,
            KNRGCode.KNRGCode_RotaryDirection_6,
            KNRGCode.KNRGCode_RotaryDirection_7,
            KNRGCode.KNRGCode_RotaryDirection_8,
            KNRGCode.KNRGCode_RotaryDirection_9,
            KNRGCode.KNRGCode_RotaryDirection_10,
            KNRGCode.KNRGCode_RotaryDirection_11,
            KNRGCode.KNRGCode_RotaryDirection_12 -> NavigationIconType.ENTER_ROUNDABOUT

            // 기본적으로 매핑되지 않은 경우 null 반환
            else -> NavigationIconType.NONE
        }
    }

    override fun naviViewGuideEnded() {
        Toast.makeText(this, "naviViewGuideEnded", Toast.LENGTH_SHORT).show()
        binding.naviView.guideCancel()
        binding.naviView.guidance.stop()
        KNSDK.sharedGuidance()!!.stop()

        KNSDK.sharedGuidance()?.apply {
            Toast.makeText(this@NaviActivity, "init", Toast.LENGTH_SHORT).show()
            binding.naviView.initWithGuidance(
                this,
                null,
                KNRoutePriority.KNRoutePriority_Recommand,
                KNRouteAvoidOption.KNRouteAvoidOption_None.value
            )
        }
    }

    override fun naviViewGuideState(state: KNGuideState) {
        Toast.makeText(this, "$state", Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun MyComposeScreen() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Text(text = "Hello, Jetpack Compose!")
        }
    }

}