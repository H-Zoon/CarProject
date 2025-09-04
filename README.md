# AICar (Car Project)

AICar는 OBD-II 블루투스 스캐너를 통해 차량의 실시간 데이터를 확인하고, 주행 기록을 분석 및 관리할 수 있는 안드로이드 애플리케이션입니다.

## 스토어 링크
<a href='https://play.google.com/store/apps/details?id=com.devidea.aicar'><img alt='Google Play에서 다운로드' src='https://play.google.com/intl/en_us/badges/static/images/badges/ko_badge_web_generic.png' width="200"/></a>

##  주요 기능

- **실시간 대시보드**: RPM, 속도, 냉각수 온도, 엔진 부하 등 다양한 차량 데이터를 실시간으로 확인할 수 있는 맞춤형 게이지를 제공합니다.
- **주행 기록 및 분석**: 주행 경로, 속도, RPM, 연비 등 상세한 운행 데이터를 기록하고, 각 세션별 요약 정보와 그래프를 통해 운전 습관을 분석할 수 있습니다.
- **지도 연동 경로 추적**: Google Maps와 연동하여 주행 경로를 시각적으로 확인하고, 슬라이더를 통해 시간대별 위치와 데이터를 함께 볼 수 있습니다.
- **블루투스 OBD-II 스캐너 연동**: ELM327 호환 OBD-II 스캐너와 블루투스 SPP 통신을 통해 안정적으로 데이터를 수신합니다.
- **자동 주행 기록**: 충전 중 블루투스 기기 연결 시 주행 기록을 자동으로 시작하고, 연결이 끊기면 종료하는 기능을 제공합니다.
- **사용자 설정**: 유류비 설정, 위젯 순서 변경, 마지막 연결 기기 관리 등 개인화된 설정을 지원합니다.

## 기술

- **언어**: Kotlin
- **UI**: Jetpack Compose
- **아키텍처**: MVVM (Model-View-ViewModel)
- **비동기 처리**: Coroutines, Flow
- **의존성 주입**: Hilt
- **데이터베이스**: Room
- **사용자 설정 저장**: DataStore
- **화면 전환**: Navigation Compose
- **지도**: Google Maps Compose Library
- **차트**: MPAndroidChart

## 라이브러리

- **Jetpack Compose**: `androidx.compose.ui`, `androidx.compose.material`, `androidx.compose.runtime` 등 모던 UI 툴킷
- **Navigation**: `androidx.navigation:navigation-compose`를 사용한 선언적 화면 이동
- **Lifecycle**: `androidx.lifecycle:lifecycle-runtime-ktx`, `androidx.lifecycle:lifecycle-viewmodel-compose` 등 AAC 라이프사이클 관리
- **Hilt**: `com.google.dagger:hilt-android`를 통한 의존성 주입
- **Room**: `androidx.room:room-runtime`, `androidx.room:room-ktx`를 사용한 로컬 데이터베이스
- **DataStore**: `androidx.datastore:datastore-preferences`를 통한 Key-Value 데이터 저장
- **Accompanist**: `com.google.accompanist:accompanist-systemuicontroller`, `com.google.accompanist:accompanist-permissions` 등 Compose 확장 라이브러리
- **Google Maps**: `com.google.maps.android:maps-compose`를 통한 지도 UI 구현
- **MPAndroidChart**: `com.github.PhilJay:MPAndroidChart`를 사용한 데이터 시각화
- **Kotlinx Serialization**: `org.jetbrains.kotlinx:kotlinx-serialization-json`을 통한 JSON 처리

## CI/CD

- **GitHub Actions**를 사용하여 CI/CD 파이프라인을 구축했습니다.
- **Workflow**:
  - `master` 브랜치에 `push` 이벤트가 발생할 때마다 워크플로우가 실행됩니다.
  - **ktlint**: 코틀린 코드 스타일을 검사하여 일관성을 유지합니다.
  - **build**: 디버그 버전의 애플리케이션을 빌드하여 코드의 통합성을 검증합니다.

## 프로젝트 설정 및 빌드

1.  **API 키 설정**: `local.properties` 파일에 Google Maps API 키를 추가해야 합니다.
    ```properties
    MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
    ```

2.  **빌드**: 프로젝트 루트 디렉토리에서 아래 명령어를 실행하여 프로젝트를 빌드할 수 있습니다.
    ```shell
    ./gradlew build
    ```
