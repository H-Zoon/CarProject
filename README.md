네, 바로 복사해서 사용하실 수 있도록 전체 내용을 마크다운 코드 블록으로 제공해 드립니다.

````markdown
# 🚗 AICar - 당신의 스마트 드라이빙 비서
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6.2-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Google Play](https://img.shields.io/badge/Google_Play-Download-48B983?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.devidea.aicar)

**"어떻게 하면 내 운전 습관을 개선하고 유류비를 아낄 수 있을까?"** 라는 현실적인 고민에서 시작된 프로젝트입니다. 운전을 즐기는 개발자가 원하는 모든 기능을 담아 가장 직관적인 운전 비서 앱을 직접 만들었습니다.

<br/>

<p align="center">
  <img src="YOUR_APP_PREVIEW_IMAGE.gif" alt="AICar App Preview" width="300"/>
</p>

<br/>

## ✨ 주요 기능 (Core Features)

* **📈 실시간 주행 데이터 분석**: 엔진 부하, 속도, 연료 분사량 등 차량 데이터를 실시간으로 분석하여 평균 연비, 누적 유류비, 급가속/급감속 횟수를 제공합니다.
* **💯 주행 점수 및 피드백**: 누적된 주행 데이터를 기반으로 '주행 점수'를 산출하고, 구체적인 피드백을 통해 실질적인 운전 습관 개선을 돕습니다.
* **🎨 사용자 커스텀 대시보드**: RPM, 속도 등 원하는 정보 위젯을 드래그 앤 드롭으로 자유롭게 배치하고 삭제하며 나만의 대시보드를 만들 수 있습니다.
* **🔄 자동 기록 및 세션 관리**: 앱이 백그라운드에 있어도 주행 시작과 종료를 자동으로 감지하여 모든 데이터를 안정적으로 기록하고, 일별/월별로 기록을 조회하고 관리할 수 있습니다.

## 🛠️ 기술 스택 및 아키텍처 (Tech Stack & Architecture)

* **Language**: Kotlin
* **Architecture**: MVVM (Model-View-ViewModel)
* **UI**: 100% Jetpack Compose, Canvas, Material3
* **Asynchronous Processing**: Coroutine, Flow (StateFlow, SharedFlow)
* **Dependency Injection**: Hilt
* **Database**: Room, Proto DataStore
* **Communication**: Bluetooth SPP
* **Testing**: JUnit4, Truth, Hilt-Testing
* **CI/CD**: Git, GitHub, GitHub Actions

## 💡 주요 구현 내용 (Key Implementation Details)

### 🎨 User Experience (UX/UI)

#### 1. 동적 데이터 시각화: Canvas와 지도를 연동한 인터랙티브 분석
Jetpack Compose의 **`Canvas`** API를 직접 활용하여 시계열 데이터를 표현하는 커스텀 라인 차트를 개발했습니다. 사용자가 재생 슬라이더를 조작하면, **`LaunchedEffect`**와 **`StateFlow`**를 통해 해당 시점의 데이터가 차트와 지도 위 마커에 실시간으로 연동되어, 특정 주행 경로와 운전 패턴을 직관적으로 분석할 수 있습니다.

```kotlin
// file: app/src/main/java/com/devidea/aicar/ui/main/components/MapComponent.kt

// 슬라이더 위치 변화 시 카메라 애니메이션
LaunchedEffect(sliderPosition, path) {
    // 경로상 현재 위치(target)를 찾아 카메라를 애니메이션과 함께 이동
    path.getOrNull(sliderPosition.toInt())?.let { target ->
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLng(target),
            durationMs = animationDurationMs,
        )
    }
}
```

#### 2. 사용자 맞춤형 대시보드: ReorderableLazyGrid를 활용한 개인화 UX
**`ReorderableLazyGrid`**를 적용하여 RPM, 속도 등 다양한 게이지 위젯을 사용자가 직접 드래그 앤 드롭으로 재정렬할 수 있도록 했습니다. 또한, **`pointerInput`**과 **`onGloballyPositioned`** Modifier를 조합하여, 드래그 중인 아이템을 특정 영역으로 끌어다 놓으면 삭제되는 직관적인 제스처 기반의 UX를 구현했습니다.

```kotlin
// file: app/src/main/java/com/devidea/aicar/ui/main/components/DashboardComponent.kt

Box(
    modifier = Modifier
        .longPressDraggableHandle(
            onDragStarted = { /*...*/ },
            onDragStopped = {
                // 드래그가 끝났을 때, 마지막 포인터 위치가 삭제 영역에 포함되면
                // 해당 게이지를 토글(삭제)하는 뷰모델 함수 호출
                deleteZoneRect.value
                    ?.takeIf { it.contains(lastPointer) }
                    ?.let { viewModel.onGaugeToggle(gauge.id) }
            },
        ),
) {
    gauge.content()
}
```

#### 3. 상태 변화에 반응하는 UI: AnimatedContent로 구현한 자연스러운 전환
앱의 상태 변화를 사용자에게 명확하고 자연스럽게 전달하기 위해 **`AnimatedContent`**와 **`AnimatedVisibility`**를 적극 활용했습니다. 주행 기록 화면에서 '선택 모드'로 진입하면, 상단 바의 타이틀이 "N개 선택됨"으로 부드럽게 전환되고 삭제 버튼이 나타나는 등, 사용자가 현재 맥락을 쉽게 인지할 수 있도록 구현했습니다.

```kotlin
// file: app/src/main/java/com/devidea/aicar/ui/main/components/history/HistoryComponent.kt

title = {
    AnimatedContent(
        targetState =
            if (selectionMode) {
                "${selectedSessions.size}개 선택됨"
            } else {
                "주행 기록"
            },
        transitionSpec = {
            slideInVertically { -it } + fadeIn() with
                slideOutVertically { it } + fadeOut()
        },
        label = "",
    ) { title ->
        Text(text = title, ...)
    }
},
```

---

### ⚙️ Technical Deep Dive

#### 1. 안정적인 백그라운드 데이터 처리 아키텍처
앱이 보이지 않는 상태에서도 데이터 수집이 안정적으로 동작하도록 **Foreground Service** 기반의 아키텍처를 설계했습니다.
* **`SppService`**: 블루투스 연결, 데이터 송수신 등 하드웨어 통신만 전담.
* **`PollingService`**: 차량 데이터 폴링, 위치 정보 결합, DB 저장 등 핵심 비즈니스 로직을 처리.

**Hilt**를 통해 각 컴포넌트의 의존성을 주입하여 결합도를 낮추고, 역할 분리를 통해 복잡한 백그라운드 작업의 안정성과 유지보수성을 크게 향상시켰습니다.

```kotlin
// file: app/src/main/java/com/devidea/aicar/service/PollingService.kt

@AndroidEntryPoint
class PollingService : Service() {

    @Inject
    lateinit var sppClient: SppClient

    @Inject
    lateinit var pollingManager: PollingManager

    // ... (서비스 로직)
}
```

#### 2. Coroutine과 Flow를 활용한 반응형 프로그래밍
끊임없이 변화하는 차량 데이터와 블루투스 연결 상태를 효과적으로 처리하기 위해 Kotlin **Coroutine**과 **Flow**를 핵심 비동기 처리 도구로 사용했습니다. **`StateFlow`**와 **`SharedFlow`**로 데이터 스트림을 모델링하고, UI 레이어에서는 `collectAsStateWithLifecycle()`를 사용하여 생명주기를 인지하는 안전한 데이터 구독이 가능하도록 구현했습니다.

```kotlin
// file: app/src/main/java/com/devidea/aicar/drive/PollingManager.kt

@Singleton
class PollingManager @Inject constructor(...) {
    // 내부에서만 수정 가능한 MutableStateFlow
    private val _rpm = MutableStateFlow(0)

    // 외부에는 읽기 전용 StateFlow를 노출하여 데이터 안정성 보장
    val rpm: StateFlow<Int> get() = _rpm.asStateFlow()

    private fun startPolling() {
        // ...
    }
}
```

#### 3. Room과 DataStore를 이용한 체계적인 데이터 영속성 관리
데이터의 성격에 따라 두 가지 다른 저장소를 사용하여 효율성과 안정성을 모두 확보했습니다.
* **Room Database**: 주행 세션, GPS 좌표 등 관계형 구조를 가지는 대용량 데이터를 저장합니다. 복잡한 집계 쿼리를 DAO에 작성하여 DB 레벨에서 효율적인 연산을 수행하도록 설계했습니다.
* **Proto DataStore**: 위젯 순서, 자동 연결 여부 등 사용자의 간단한 설정값을 저장합니다. Flow 기반 API를 통해 설정값 변경 시 UI가 반응형으로 업데이트되도록 구현했습니다.

```kotlin
// file: app/src/main/java/com/devidea/aicar/storage/room/drive/DrivingDao.kt

@Dao
interface DrivingDao {
    // 월별 통계를 계산하는 복잡한 집계 쿼리
    @Query("""
        SELECT
          IFNULL(SUM(s.totalDistanceKm), 0) AS totalDistanceKm,
          IFNULL(AVG(s.averageKPL), 0) AS averageKPL,
          IFNULL(SUM(s.fuelCost), 0) AS totalFuelCost
        FROM DrivingSessionSummary AS s ...
    """)
    fun getMonthlyStats(startMillis: Long, endMillis: Long): Flow<MonthlyStats>
}

// file: app/src/main/java/com/devidea/aicar/storage/datastore/DataStoreRepository.kt

class DataStoreRepository @Inject constructor(...) {
    // 위젯 순서 데이터를 Flow<List<String>> 형태로 제공
    val selectedGaugeIds: Flow<List<String>> =
        dataStore.data.map { prefs -> ... }
}
```

## 🏆 성과 (Achievements)

* **안정적인 서비스 운영**: Foreground Service를 통해 평균 세션 시간 60분의 안정적인 서비스를 구축했으며, 10,000km 이상의 장거리 주행 데이터에서도 통계가 정확하게 계산됨을 검증했습니다.
* **사용자 중심 UI/UX**: 복잡한 차량 데이터를 직관적인 UI로 시각화하여 "간결하고 사용하기 편하다"는 긍정적인 사용자 피드백을 확보했습니다.
* **정량적 성과**: 별도의 마케팅 없이 **DAU 100명**을 달성하며 서비스의 효용성을 입증했습니다.

## 🚀 성장 및 학습 (Growth & Learning)

* **문제 해결 능력**:
    * **블루투스 통신 병목 현상**: 여러 PID를 하나의 명령어로 묶어 전송하는 `MultiPidUtils`를 구현하여 통신 횟수를 획기적으로 줄여 문제 해결.
    * **Hilt 테스트 환경 구축**: `CustomTestRunner`를 직접 구현하여 Hilt 의존성 주입 실패 문제 해결.
* **CI/CD 자동화**: **GitHub Actions**를 처음 도입하여 코드 푸시 시 `ktlint` 검사와 자동 빌드가 실행되는 CI 파이프라인을 구축하며 개발 프로세스 효율화 경험.
* **Jetpack Compose 역량 심화**: **100% Compose**로 UI를 구현하고, Canvas와 Reorderable 라이브러리를 활용하여 커스텀 UI를 구현하며 Compose에 대한 깊이 있는 활용 능력 확보.

## 📅 향후 계획 (Future Plans)

수집된 주행 데이터를 활용하여 "코너링 시 감속 패턴", "특정 구간 연비 저하 원인" 등 **개인화되고 구체적인 운전 습관 개선점을 제안**하는 기능을 개발할 계획입니다.
````
