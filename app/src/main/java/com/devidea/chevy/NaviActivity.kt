package com.devidea.chevy

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devidea.chevy.App.Companion.instance
import com.devidea.chevy.codec.ToDeviceCodec
import com.devidea.chevy.databinding.ActivityNaviBinding
import com.kakao.sdk.v2.common.BuildConfig.VERSION_NAME
import com.kakaomobility.knsdk.KNCarFuel
import com.kakaomobility.knsdk.KNCarType
import com.kakaomobility.knsdk.KNLanguageType
import com.kakaomobility.knsdk.KNRouteAvoidOption
import com.kakaomobility.knsdk.KNRoutePriority
import com.kakaomobility.knsdk.KNSDK
import com.kakaomobility.knsdk.common.objects.KNError
import com.kakaomobility.knsdk.common.objects.KNError_Code_C103
import com.kakaomobility.knsdk.common.objects.KNError_Code_C302
import com.kakaomobility.knsdk.common.objects.KNPOI
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_CitsGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_GuideStateDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_LocationGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_RouteGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_SafetyGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuidance_VoiceGuideDelegate
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideRouteChangeReason
import com.kakaomobility.knsdk.guidance.knguidance.citsguide.KNGuide_Cits
import com.kakaomobility.knsdk.guidance.knguidance.common.KNLocation
import com.kakaomobility.knsdk.guidance.knguidance.locationguide.KNGuide_Location
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.KNGuide_Route
import com.kakaomobility.knsdk.guidance.knguidance.routeguide.objects.KNMultiRouteInfo
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.KNGuide_Safety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafetyType
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import com.kakaomobility.knsdk.ui.view.KNNaviView
import java.lang.Math.pow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.tan

class NaviActivity : AppCompatActivity(), KNGuidance_GuideStateDelegate,
    KNGuidance_SafetyGuideDelegate, KNGuidance_CitsGuideDelegate, KNGuidance_LocationGuideDelegate,
    KNGuidance_RouteGuideDelegate, KNGuidance_VoiceGuideDelegate {

    lateinit var binding: ActivityNaviBinding
    val TAG = "NaviActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        binding.naviView.sndVolume = 1f
        binding.naviView.useDarkMode = true
        binding.naviView.fuelType = KNCarFuel.KNCarFuel_Gasoline
        binding.naviView.carType = KNCarType.KNCarType_1

        KNSDK.sharedGuidance()?.apply {
            Toast.makeText(this@NaviActivity,"init", Toast.LENGTH_SHORT).show()
            binding.naviView.initWithGuidance(
                this,
                null,
                KNRoutePriority.KNRoutePriority_Recommand,
                KNRouteAvoidOption.KNRouteAvoidOption_None.value
            )
        }

        val startKatec = wgs84ToKatech(37.6448294347533,126.691187475643)
        val goalKatec = wgs84ToKatech(37.389573859018185,127.09082543849945)
        // 출발지 설정
        val start = KNPOI("home", startKatec.first.toInt(), startKatec.second.toInt(), null)

        // 목적지 설정
        val goal = KNPOI("company", 957909, 1898496, null)

        // 경로 옵션 설정
        val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
        val curAvoidOptions =
            KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value or KNRouteAvoidOption.KNRouteAvoidOption_SZone.value

        KNSDK.makeTripWithStart(start, goal, null, null) { knError: KNError?, knTrip: KNTrip? ->
            if (knError != null) {
                Log.d(TAG, "경로 생성 에러(KNError: $knError")
            }
            knTrip?.routeWithPriority(curRoutePriority, curAvoidOptions) { error, _ ->
                // 경로 요청 실패
                KNSDK.sharedGuidance()?.apply {
                    // 각 가이던스 델리게이트 등록
                    guideStateDelegate = this@NaviActivity
                    locationGuideDelegate = this@NaviActivity
                    routeGuideDelegate = this@NaviActivity
                    safetyGuideDelegate = this@NaviActivity
                    voiceGuideDelegate = this@NaviActivity
                    citsGuideDelegate = this@NaviActivity
                }
                if (error != null) {
                    Log.d(TAG, "경로 요청 실패 : $error")
                }
                // 경로 요청 성공
                else {
                    // 경로 요청 성공
                    KNSDK.sharedGuidance()?.apply {
                       /* binding.naviView.initWithGuidance(
                            this,
                            knTrip,
                            curRoutePriority,
                            curAvoidOptions
                        )*/
                    }
                }
            }
        }
    }

    // 주변의 안전 운행 정보가 업데이트될 때 호출됩니다.
    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        binding.naviView.guidanceDidUpdateAroundSafeties(guidance,safeties)
        // 업데이트된 주변 안전 정보 리스트가 null이 아닌 경우
        safeties?.let {
            for (safety in it) {
                // 예: 각 안전 정보의 타입과 설명을 로그로 출력
                Log.d(
                    "AroundSafety",
                    "Safety type: ${safety.safetyType()}, description: ${safety.isOnStraightWay()}"
                )
            }
        }
    }


    // 안전 운행 정보가 업데이트될 때 호출됩니다.
    override fun guidanceDidUpdateSafetyGuide(guidance: KNGuidance, safetyGuide: KNGuide_Safety?) {
        binding.naviView.guidanceDidUpdateSafetyGuide(guidance,safetyGuide)
        if (safetyGuide == null) {
            println("안전 가이드 정보가 없습니다.")
            return
        }

        // safetiesOnGuide에서 speedLimit 정보를 가져옴
        safetyGuide.safetiesOnGuide?.forEach { safety ->
            if (safety is KNSafety_Camera) {
                val speedLimit = safety.speedLimit
                val distance = safety.location.distFromS
                Toast.makeText(
                    this@NaviActivity,
                    "제한 속도: $speedLimit km/h, $distance",
                    Toast.LENGTH_SHORT
                ).show()
                ToDeviceCodec.sendLimitSpeed(
                    assistantDistance = distance,
                    assistantLimitedSpeed = speedLimit
                )
                ToDeviceCodec.sendCameraDistance(distance, 1, 0)
            }
        }
    }

    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceCheckingRouteChange")
        binding.naviView.guidanceRouteChanged(aGuidance)
    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        Log.d(TAG, "guidanceDidUpdateIndoorRoute")
    }

    override fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        Log.d(TAG, "guidanceDidUpdateRoutes")
        binding.naviView.guidanceDidUpdateRoutes(aGuidance, aRoutes, aMultiRouteInfo)
    }

    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceGuideEnded")
        binding.naviView.guidanceGuideEnded(aGuidance)
    }

    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceGuideStarted")
        binding.naviView.guidanceGuideStarted(aGuidance)
    }

    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceOutOfRoute")
        binding.naviView.guidanceOutOfRoute(aGuidance)
    }

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

    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        Log.d(TAG, "guidanceRouteUnchanged")
        binding.naviView.guidanceRouteUnchanged(aGuidance)
    }

    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        Log.d(TAG, "guidanceRouteUnchangedWithError")
        binding.naviView.guidanceRouteUnchangedWithError(aGuidnace, aError)
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        Log.d(TAG, "didUpdateCitsGuide")
        binding.naviView.didUpdateCitsGuide(aGuidance,aCitsGuide)
    }

    override fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        Log.d(TAG, "guidanceDidUpdateLocation")
        binding.naviView.guidanceDidUpdateLocation(aGuidance, aLocationGuide)
    }

    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        Log.d(TAG, "guidanceDidUpdateRouteGuide")
        binding.naviView.guidanceDidUpdateRouteGuide(aGuidance, aRouteGuide)
    }

    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        Log.d(TAG, "didFinishPlayVoiceGuide")
        binding.naviView.didFinishPlayVoiceGuide(aGuidance,aVoiceGuide)
    }

    override fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ): Boolean {
        Log.d(TAG, "shouldPlayVoiceGuide")
        binding.naviView.shouldPlayVoiceGuide(aGuidance,aVoiceGuide,aNewData)
        return true
    }

    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        Log.d(TAG, "willPlayVoiceGuide")
        binding.naviView.willPlayVoiceGuide(aGuidance,aVoiceGuide)
    }

    fun wgs84ToKatech(latitude: Double, longitude: Double): Pair<Double, Double> {
        val RE = 6378137.0            // WGS84 반경
        val GRID = 5.0                // 격자 간격
        val SLAT1 = 30.0              // 표준위도1
        val SLAT2 = 60.0              // 표준위도2
        val OLON = 127.5              // 기준점 경도
        val OLAT = 38.0               // 기준점 위도
        val XO = 200000.0             // 기준점 X좌표
        val YO = 500000.0             // 기준점 Y좌표

        // 라디안으로 변환
        val DEGRAD = PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = pow(sf, sn) * cos(slat1) / sn
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / pow(ro, sn)

        val ra = tan(PI * 0.25 + latitude * DEGRAD * 0.5)
        val raTransformed = re * sf / pow(ra, sn)
        var theta = longitude * DEGRAD - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        val x = raTransformed * sin(theta) + XO
        val y = ro - raTransformed * cos(theta) + YO

        return Pair(x, y)
    }

}