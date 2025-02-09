package com.devidea.chevy.ui.screen.navi

import android.graphics.Color
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.devidea.chevy.Logger
import com.devidea.chevy.datas.navi.NavigationIconType
import com.devidea.chevy.datas.navi.findGuideAsset
import com.devidea.chevy.datas.navi.isCameraType
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendCameraDistance
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendLaneInfo
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendLimitSpeed
import com.devidea.chevy.datas.obd.protocol.codec.Msgs.sendNextInfo
import com.devidea.chevy.service.BleServiceManager
import com.devidea.chevy.ui.screen.map.rememberMapViewWithLifecycle
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.route.RouteLineLayer
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
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
import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety_Camera
import com.kakaomobility.knsdk.guidance.knguidance.voiceguide.KNGuide_Voice
import com.kakaomobility.knsdk.trip.kntrip.KNTrip
import com.kakaomobility.knsdk.trip.kntrip.knroute.KNRoute
import com.kakaomobility.knsdk.ui.view.KNNaviView
import com.kakaomobility.knsdk.ui.view.KNNaviView_GuideStateDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject


@OptIn(FlowPreview::class)
class KNavi @Inject constructor(
    private val serviceManager: BleServiceManager
) : KNGuidance_GuideStateDelegate,
    KNGuidance_SafetyGuideDelegate,
    KNGuidance_CitsGuideDelegate,
    KNGuidance_LocationGuideDelegate,
    KNGuidance_RouteGuideDelegate,
    KNGuidance_VoiceGuideDelegate,
    KNNaviView_GuideStateDelegate {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var collectionJob: Job? = null

    fun startLocationCollection() {
        if (collectionJob?.isActive == true) return

        collectionJob = scope.launch {
            currentLocation
                .sample(1500) // 1500ms마다 샘플링
                .collect { location ->
                    sendSafety(location)
                    delay(500L)
                    // sendLane()
                    sendGuide(location)
                }
        }
    }

    fun stopLocationCollection() {
        collectionJob?.cancel()
        collectionJob = null
    }

    var lastSafetyState: Boolean = false

    var knNaviView: KNNaviView? = null
    private val bleService by lazy { serviceManager.getService() }

    private val _currentLocation = MutableSharedFlow<KNLocation?>()
    val currentLocation: SharedFlow<KNLocation?> = _currentLocation

    private val _routeGuide = MutableStateFlow<KNGuide_Route?>(null)
    val routeGuide: StateFlow<KNGuide_Route?> = _routeGuide.asStateFlow()

    private val _safetyGuide = MutableStateFlow<KNGuide_Safety?>(null)
    val safetyGuide: StateFlow<KNGuide_Safety?> = _safetyGuide.asStateFlow()

    private val _naviGuideState = MutableStateFlow<KNGuideState?>(null)
    val naviGuideState: StateFlow<KNGuideState?> = _naviGuideState.asStateFlow()

    private fun makeTrip(
        data: NavigateData,
        success: ((KNTrip) -> Unit),
        failure: (() -> Unit)
    ) {

        val addressName = data.addressName
        val goalX = data.goalX
        val goalY = data.goalY
        val startX = data.startX
        val startY = data.startY

        val startKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = startX, aWgs84Lat = startY)
        val goalKATEC = KNSDK.convertWGS84ToKATEC(aWgs84Lon = goalX, aWgs84Lat = goalY)

        val startKNPOI = KNPOI("", startKATEC.x.toInt(), startKATEC.y.toInt(), aAddress = null)
        val goalKNPOI = KNPOI("", goalKATEC.x.toInt(), goalKATEC.y.toInt(), addressName)

        KNSDK.makeTripWithStart(
            aStart = startKNPOI,
            aGoal = goalKNPOI,
            aVias = null
        ) { knError, knTrip ->
            if (knError != null) {
                Logger.e { "경로 생성 에러(KNError: $knError" }
            } else {
                if (knTrip != null) {
                    success(knTrip)
                }
            }
        }
    }

    private fun makeRoute(
        knTrip: KNTrip,
        priority: KNRoutePriority,
        success: ((MutableList<KNRoute>) -> Unit),
        failure: (() -> Unit)
    ) {
        val curRoutePriority = KNRoutePriority.KNRoutePriority_Recommand
        val curAvoidOptions =
            KNRouteAvoidOption.KNRouteAvoidOption_RoadEvent.value

        knTrip.routeWithPriority(priority, curAvoidOptions) { error, route ->
            if (error != null) {
                failure()
            } else {
                if (route != null) {
                    success(route)
                }
            }
        }
    }

    // enum 값을 사람이 읽기 좋은 문자열로 변환하는 헬퍼 함수
    fun getRoutePriorityName(priority: KNRoutePriority?): String {
        return when (priority) {
            KNRoutePriority.KNRoutePriority_Recommand -> "추천 경로"
            KNRoutePriority.KNRoutePriority_Time -> "시간 우선"
            KNRoutePriority.KNRoutePriority_Distance -> "거리 우선"
            KNRoutePriority.KNRoutePriority_HighWay -> "고속도로 우선"
            KNRoutePriority.KNRoutePriority_WideWay -> "큰 길 우선"
            else -> "알 수 없음"
        }
    }

    @Composable
    fun RouteList(
        routes: List<KNRoute>,
        // 포커싱된 카드가 재터치되었을 때 데이터를 전달할 콜백
        onRouteFocused: (KNRoute) -> Unit,
        onRouteSelected: (KNRoute) -> Unit
    ) {
        // routes가 비어있지 않다면 첫 번째 항목을 초기값으로 사용
        var focusedRoute by remember { mutableStateOf(routes.firstOrNull()) }

        LazyRow {
            items(routes) { route ->
                RouteCard(
                    route = route,
                    // 현재 포커싱된 카드와 일치하면 true
                    isFocused = if (focusedRoute != null) {
                        (focusedRoute == route)
                    } else {
                        focusedRoute = route
                        onRouteFocused(route)
                        true
                    },
                    // 카드 클릭 시 처리
                    onCardClick = { clickedRoute ->
                        if (focusedRoute == clickedRoute) {
                            // 이미 포커싱된 카드를 다시 터치하면 데이터를 전달
                            onRouteSelected(clickedRoute)
                        } else {
                            // 다른 카드를 터치하면 해당 카드로 포커싱
                            focusedRoute = clickedRoute
                            onRouteFocused(clickedRoute)
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun RouteCard(
        route: KNRoute,
        isFocused: Boolean,
        // 카드 클릭 이벤트 콜백
        onCardClick: (KNRoute) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                // 포커싱된 카드인 경우 테두리로 강조
                .then(
                    if (isFocused) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
                    else Modifier
                )
                // 클릭 이벤트 처리
                .clickable { onCardClick(route) }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "경로 우선순위: ${getRoutePriorityName(route.priority)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "경로 거리: ${route.totalDist} m")
                Text(text = "경로 시간: ${route.totalTime} 초")
                Text(text = "경로 요금: ${route.totalCost} 원")
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    fun FindLoadScreen(data: NavigateData) {
        val stateList = remember { mutableStateListOf<KNRoute>() }
        var focusedRoute by remember { mutableStateOf<KNRoute?>(null) }
        var selectedRoute by remember { mutableStateOf<KNRoute?>(null) }
        var knTrip by remember { mutableStateOf<KNTrip?>(null) }

        val context = LocalContext.current
        var kakaoMap by remember { mutableStateOf<com.kakao.vectormap.KakaoMap?>(null) }

        // Compose의 Lifecycle에 맞게 MapView 생성
        val mapView = rememberMapViewWithLifecycle(context) { map ->
            kakaoMap = map
            //onMapReady(map)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
            RouteList(stateList, onRouteFocused = { focusedRoute = it }, onRouteSelected = { selectedRoute = it })
        }

        // 상태에 따라 NaviScreen(또는 상세 화면)을 표시합니다.
        focusedRoute?.let { route ->
            kakaoMap?.let { drawRouteLine(it, route.routePolylineWGS84()) }
        }

        selectedRoute?.let { route ->
            NaviScreen(knTrip, route.priority!!, route.avoidOptions)
        }

        LaunchedEffect(data) {
            makeTrip(
                data,
                success = { result ->
                    knTrip = result
                    val routePriorities = listOf(
                        KNRoutePriority.KNRoutePriority_Recommand,
                        KNRoutePriority.KNRoutePriority_Time,
                        KNRoutePriority.KNRoutePriority_Distance,
                        KNRoutePriority.KNRoutePriority_HighWay,
                        KNRoutePriority.KNRoutePriority_WideWay
                    )

                    for (priority in routePriorities) {
                        makeRoute(
                            result,
                            priority,
                            success = { routes -> stateList.addAll(routes) },
                            failure = {}
                        )
                    }
                },
                failure = {}
            )
        }
    }

    fun mapStatusToStyle(status: Int): RouteLineStyle {
        return when (status) {
            // 교통 상태 정보 없음(0) 또는 원활(4) -> 파란색
            0, 4 -> RouteLineStyle.from(16f, Color.BLUE)
            // 교통 서행(3) -> 청색 (Cyan)
            3 -> RouteLineStyle.from(16f, Color.CYAN)
            // 교통 지체(2) -> 노란색
            2 -> RouteLineStyle.from(16f, Color.YELLOW)
            // 교통 정체(1) 또는 교통사고(6) -> 빨간색
            1, 6 -> RouteLineStyle.from(16f, Color.RED)
            else -> RouteLineStyle.from(16f, Color.BLUE)
        }
    }

    @Composable
    fun drawRouteLine(
        map: com.kakao.vectormap.KakaoMap,
        mapList: List<Map<String, Number>>?
    ) {

        // 1. RouteLineLayer 가져오기
        val layer: RouteLineLayer = map.routeLineManager!!.layer
        layer.removeAll()

        // 2. 교통 흐름에 따라 사용할 스타일을 미리 생성
        // (여기서는 스타일셋에 4개의 스타일을 등록하지만,
        //  실제 세그먼트에는 각 세그먼트별 교통 상태에 맞는 색상을 직접 지정함)
        val styles1 = RouteLineStyles.from(RouteLineStyle.from(16f, Color.BLUE))
        val styles2 = RouteLineStyles.from(RouteLineStyle.from(16f, Color.CYAN))
        val styles3 = RouteLineStyles.from(RouteLineStyle.from(16f, Color.YELLOW))
        val styles4 = RouteLineStyles.from(RouteLineStyle.from(16f, Color.RED))
        val stylesSet = RouteLineStylesSet.from(styles1, styles2, styles3, styles4)

        // 3. routePolylineWGS84 데이터를 바탕으로 RouteLineSegment 생성하기
        val segments = mutableListOf<RouteLineSegment>()

        if (mapList.isNullOrEmpty().not()) {
            // 첫 번째 데이터의 trfSt 값을 초기 교통 상태로 사용
            var currentStatus = mapList!![0]["trfSt"]?.toInt() ?: 0
            val currentPoints = mutableListOf<LatLng>()

            for (point in mapList) {
                val lat = point["y"]?.toDouble() ?: 0.0
                val lng = point["x"]?.toDouble() ?: 0.0
                val latLng = LatLng.from(lat, lng)
                val status = point["trfSt"]?.toInt() ?: currentStatus

                if (status != currentStatus && currentPoints.size >= 2) {
                    // 기존 세그먼트 생성
                    segments.add(
                        RouteLineSegment.from(
                            currentPoints,
                            mapStatusToStyle(currentStatus)
                        )
                    )
                    // 마지막 좌표를 새로운 세그먼트의 시작점으로 유지
                    val lastPoint = currentPoints.last()
                    currentPoints.clear()
                    currentPoints.add(lastPoint)
                    currentStatus = status
                }
                currentPoints.add(latLng)
            }
            // 마지막 남은 좌표들로 세그먼트 생성 (필요한 경우)
            if (currentPoints.size >= 2) {
                segments.add(
                    RouteLineSegment.from(
                        currentPoints,
                        mapStatusToStyle(currentStatus)
                    )
                )
            }
        }

        // 4. RouteLineOptions 생성: segments와 스타일셋을 지정
        val options = RouteLineOptions.from(segments)
            .setStylesSet(stylesSet)

        // 5. RouteLineLayer에 options를 추가하여 지도에 경로 라인 생성하기
        val routeLine = layer.addRouteLine(options)
        val cameraUpdate =
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(
                    (mapList?.get((mapList.size)/2))?.get("y")!!.toDouble(),
                        (mapList.get(mapList.size/2)).get("x")!!.toDouble()
                )
            )
        val zoom = CameraUpdateFactory.zoomTo(
            when (mapList.size) {
                in 1..150 -> 15
                in 151..500 -> 10
                else -> 7
            }
        )
        map.moveCamera(cameraUpdate)
        map.moveCamera(zoom)
    }

    @Composable
    fun NaviScreen(
        knTrip: KNTrip?,
        curRoutePriority: KNRoutePriority = KNRoutePriority.KNRoutePriority_Recommand,
        curAvoidOptions: Int = 0
    ) {
        AndroidView(
            factory = {
                KNNaviView(it).apply {
                    sndVolume = 1f
                    useDarkMode = true
                    fuelType = KNCarFuel.KNCarFuel_Gasoline
                    carType = KNCarType.KNCarType_1
                    guideStateDelegate = this@KNavi
                }.also { view ->
                    knNaviView = view
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(Unit) {
            KNSDK.sharedGuidance()?.apply {
                guideStateDelegate = this@KNavi
                locationGuideDelegate = this@KNavi
                routeGuideDelegate = this@KNavi
                safetyGuideDelegate = this@KNavi
                voiceGuideDelegate = this@KNavi
                citsGuideDelegate = this@KNavi
            }
        }

        LaunchedEffect(knNaviView) {
            KNSDK.sharedGuidance()?.apply {
                knNaviView?.initWithGuidance(
                    this,
                    knTrip,
                    curRoutePriority,
                    curAvoidOptions
                )
            }
        }
    }

    // Delegate Methods
    override fun guidanceCheckingRouteChange(aGuidance: KNGuidance) {
        knNaviView?.guidanceCheckingRouteChange(aGuidance)
    }

    override fun guidanceDidUpdateIndoorRoute(aGuidance: KNGuidance, aRoute: KNRoute?) {
        knNaviView?.guidanceDidUpdateIndoorRoute(aGuidance, aRoute)
    }

    override fun guidanceDidUpdateRoutes(
        aGuidance: KNGuidance,
        aRoutes: List<KNRoute>,
        aMultiRouteInfo: KNMultiRouteInfo?
    ) {
        knNaviView?.guidanceDidUpdateRoutes(
            aGuidance,
            aRoutes,
            aMultiRouteInfo
        )
    }

    override fun guidanceGuideEnded(aGuidance: KNGuidance) {
        knNaviView?.guidanceGuideEnded(aGuidance)
    }

    override fun guidanceGuideStarted(aGuidance: KNGuidance) {
        knNaviView?.guidanceGuideStarted(aGuidance)
    }

    override fun guidanceOutOfRoute(aGuidance: KNGuidance) {
        knNaviView?.guidanceOutOfRoute(aGuidance)
    }

    override fun guidanceRouteChanged(
        aGuidance: KNGuidance,
        aFromRoute: KNRoute,
        aFromLocation: KNLocation,
        aToRoute: KNRoute,
        aToLocation: KNLocation,
        aChangeReason: KNGuideRouteChangeReason
    ) {
        knNaviView?.guidanceRouteChanged(aGuidance)
    }

    override fun guidanceRouteUnchanged(aGuidance: KNGuidance) {
        knNaviView?.guidanceRouteUnchanged(aGuidance)
    }

    override fun guidanceRouteUnchangedWithError(aGuidnace: KNGuidance, aError: KNError) {
        knNaviView?.guidanceRouteUnchangedWithError(aGuidnace, aError)
    }

    override fun didUpdateCitsGuide(aGuidance: KNGuidance, aCitsGuide: KNGuide_Cits) {
        knNaviView?.didUpdateCitsGuide(aGuidance, aCitsGuide)
    }

    override fun guidanceDidUpdateLocation(
        aGuidance: KNGuidance,
        aLocationGuide: KNGuide_Location
    ) {
        knNaviView?.guidanceDidUpdateLocation(aGuidance, aLocationGuide)
        CoroutineScope(Dispatchers.IO).launch {
            aLocationGuide.location?.let { updateCurrentLocation(it) }
        }
    }

    override fun guidanceDidUpdateRouteGuide(aGuidance: KNGuidance, aRouteGuide: KNGuide_Route) {
        knNaviView?.guidanceDidUpdateRouteGuide(aGuidance, aRouteGuide)
        updateRouteGuide(aRouteGuide)
    }

    override fun guidanceDidUpdateSafetyGuide(
        aGuidance: KNGuidance,
        aSafetyGuide: KNGuide_Safety?
    ) {
        knNaviView?.guidanceDidUpdateSafetyGuide(aGuidance, aSafetyGuide)
        updateSafetyGuide(aSafetyGuide)
    }

    override fun guidanceDidUpdateAroundSafeties(guidance: KNGuidance, safeties: List<KNSafety>?) {
        knNaviView?.guidanceDidUpdateAroundSafeties(guidance, safeties)
    }

    override fun shouldPlayVoiceGuide(
        aGuidance: KNGuidance,
        aVoiceGuide: KNGuide_Voice,
        aNewData: MutableList<ByteArray>
    ): Boolean {
        knNaviView?.shouldPlayVoiceGuide(aGuidance, aVoiceGuide, aNewData)
        return true
    }

    override fun willPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        knNaviView?.willPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun didFinishPlayVoiceGuide(aGuidance: KNGuidance, aVoiceGuide: KNGuide_Voice) {
        knNaviView?.didFinishPlayVoiceGuide(aGuidance, aVoiceGuide)
    }

    override fun naviViewGuideEnded() {
        knNaviView?.guideCancel()
        knNaviView?.guidance?.stop()
    }

    override fun naviViewGuideState(state: KNGuideState) {
        startLocationCollection()
        Logger.i { "$state" }
    }

    private fun sendGuide(currentLocation: KNLocation?) {
        val distance =
            routeGuide.value?.curDirection?.location?.let { currentLocation?.distToLocation(it) }
                ?: 0
        val guidanceAsset =
            routeGuide.value?.curDirection?.let { findGuideAsset(it.rgCode) }
                ?: NavigationIconType.NONE

        if (!lastSafetyState) {
            bleService?.sendNaviMsg(sendNextInfo(icon = guidanceAsset.value, distance = distance))
            Logger.w(shouldUpdate = true) { "안내전송 : ${guidanceAsset.value}, 거리:$distance" }
        }

    }

    private fun sendLane() {
        // laneInfos가 존재하고 비어있지 않은지 확인
        val laneInfos = routeGuide.value?.lane?.laneInfos

        if (laneInfos.isNullOrEmpty()) {
            // 요구조건 1 & 2: routeGuide가 null이거나 laneInfos가 비어있는 경우
            Logger.w(shouldUpdate = true) { "routeGuide가 null이거나 laneInfos가 비어있습니다. 기본값을 전송합니다." }
            bleService?.sendNaviMsg(sendLaneInfo(intArrayOf(0)))
            return
        } else {
            // laneInfos가 존재하고 비어있지 않은 경우
            val recommendArray =
                laneInfos.map { if (it.suggest == 1.toByte()) 1 else 0 }.toIntArray()
            Logger.w(shouldUpdate = true) { "추천차선 : ${recommendArray[0]}" }
            bleService?.sendNaviMsg(sendLaneInfo(recommendArray))
        }
    }

    // 상태 업데이트 메서드
    suspend fun updateCurrentLocation(location: KNLocation) {
        _currentLocation.emit(location)
    }

    fun updateRouteGuide(routeGuide: KNGuide_Route?) {
        _routeGuide.value = routeGuide
    }

    fun updateSafetyGuide(safetyGuide: KNGuide_Safety?) {
        _safetyGuide.value = safetyGuide
    }


    //TODO Test
    private fun sendSafety(currentLocation: KNLocation?) {
        if (safetyGuide.value == null || currentLocation == null) {
            sendDefaultSafetyState()
            return
        }

        val cameraSafeties = safetyGuide.value?.safetiesOnGuide
            ?.filterIsInstance<KNSafety_Camera>()
            ?.filter { it.isCameraType() }
            ?: emptyList()

        if (cameraSafeties.isNotEmpty()) {
            val closestSafety = findClosestSafety(cameraSafeties, currentLocation)
            if (closestSafety != null) {
                sendSafetyMessages(currentLocation, closestSafety)
                return
            } else {
                sendDefaultSafetyState()
                return
            }
        } else {
            sendDefaultSafetyState()
            return
        }
    }

    private fun findClosestSafety(
        safeties: List<KNSafety_Camera>,
        currentLocation: KNLocation
    ): KNSafety_Camera? {
        return safeties.minByOrNull { safety ->
            currentLocation.distToLocation(safety.location)
        }
    }

    private fun sendSafetyMessages(currentLocation: KNLocation, safety: KNSafety_Camera) {
        val speedLimit = safety.speedLimit
        val cameraDistance = currentLocation.distToLocation(safety.location)

        Logger.w(shouldUpdate = true) { "가장 가까운 카메라 - 속도 제한: $speedLimit, 거리: $cameraDistance" }

        bleService?.let { service ->
            service.sendNaviMsg(sendCameraDistance(cameraDistance, 1, 1))
            service.sendNaviMsg(sendLimitSpeed(cameraDistance, speedLimit))
            service.sendNaviMsg(sendNextInfo(icon = 0, distance = cameraDistance))
        } ?: run {
            Logger.e { "BLE 서비스가 초기화되지 않았습니다." }
        }
        lastSafetyState = true
    }

    private fun sendDefaultSafetyState() {
        if (lastSafetyState) {
            Logger.w(shouldUpdate = true) { "기본값을 전송합니다." }
            bleService?.let { service ->
                service.sendNaviMsg(sendCameraDistance(0, 0, 0))
                service.sendNaviMsg(sendLimitSpeed(0, 0))
            } ?: run {
                Logger.e { "BLE 서비스가 초기화되지 않았습니다." }
            }
            lastSafetyState = false
        }
    }
}

