package com.devidea.aicar

import com.google.common.truth.Truth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.devidea.aicar.storage.room.AppDatabase
import com.devidea.aicar.storage.room.drive.DrivingDao
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.devidea.aicar.storage.room.drive.DrivingSessionSummary
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@RunWith(AndroidJUnit4::class) // 안드로이드 환경에서 테스트 실행
@SmallTest // 단위 테스트임을 명시
@HiltAndroidTest // Hilt를 사용하는 테스트 클래스
class AppDaoTest {

    // Hilt가 테스트 규칙을 관리하도록 설정
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    // 테스트용 DB와 DAO를 주입받는다.
    /**
     * private로 선언 x
     * 의존성 주입은 Hilt가 생성한 별도의 코드가 테스트 클래스의 필드에 값을 할당해주는 방식으로 동작.
     * 필드가 private이면 클래스 외부에서 접근이 불가능하므로, Hilt가 값을 주입 불가.
     */
    @Inject
    lateinit var db: AppDatabase
    @Inject
    lateinit var dao: DrivingDao

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        // 1. 모든 테이블의 데이터를 삭제하여 다음 테스트에 영향을 주지 않도록 합니다.
        db.clearAllTables()
        // 2. 데이터베이스 연결을 닫습니다.
        db.close()
    }

    // 예시 테스트: 세션 삽입 및 조회 테스트
    @Test
    @Throws(Exception::class)
    fun insertSessionAndGetById() = runBlocking {
        // Arrange (준비)
        val session = DrivingSession(sessionId = 1, startTime = Instant.now().truncatedTo(ChronoUnit.MILLIS))
        dao.insertSession(session)

        // Act (실행)
        val retrievedSession = dao.getSessionById(1)

        // Assert (검증)
        assertThat(retrievedSession).isEqualTo(session)
    }

    // 예시 테스트: 월별 통계 쿼리 테스트
    @Test
    fun getMonthlyStats_calculatesCorrectly() = runBlocking {
        // Arrange (준비)
        // 2025년 7월에 해당하는 세션 2개 생성
        val session1 = DrivingSession(sessionId = 1, startTime = Instant.parse("2025-07-01T10:00:00Z"))
        val session2 = DrivingSession(sessionId = 2, startTime = Instant.parse("2025-07-05T12:00:00Z"))
        dao.insertSession(session1)
        dao.insertSession(session2)

        // 각 세션에 대한 요약 정보 삽입
        val summary1 = DrivingSessionSummary(sessionId = 1, totalDistanceKm = 10f, averageKPL = 15f, averageSpeedKmh=50f, accelEvent = 2, brakeEvent = 1, fuelCost = 2000)
        val summary2 = DrivingSessionSummary(sessionId = 2, totalDistanceKm = 20f, averageKPL = 13f, averageSpeedKmh=50f, accelEvent = 2, brakeEvent = 1, fuelCost = 4000)
        dao.insertSessionSummary(summary1)
        dao.insertSessionSummary(summary2)

        // 7월 1일 ~ 7월 31일 범위 설정 (밀리초)
        val startMillis = Instant.parse("2025-07-01T00:00:00Z").toEpochMilli()
        val endMillis = Instant.parse("2025-07-31T23:59:59Z").toEpochMilli()

        // Act (실행)
        val monthlyStats = dao.getMonthlyStats(startMillis, endMillis).first()

        // Assert (검증)
        assertThat(monthlyStats.totalDistanceKm).isEqualTo(30f) // 10 + 20
        assertThat(monthlyStats.averageKPL).isEqualTo(14f)      // (15 + 13) / 2
        assertThat(monthlyStats.totalFuelCost).isEqualTo(6000)  // 2000 + 4000
    }

    @Test
    fun getOngoingSessionId_returnsNull_whenAllSessionsAreFinished() = runBlocking {
        // given: 모든 세션이 종료 시간(endTime)을 가짐
        val finishedSession = DrivingSession(sessionId = 1, startTime = Instant.now(), endTime = Instant.now())
        dao.insertSession(finishedSession)

        // when: 진행 중인 세션 조회를 요청
        val ongoingSessionId = dao.getOngoingSessionId()

        // then: null이 반환되어야 함
        assertThat(ongoingSessionId).isNull()
    }

    @Test
    fun getOngoingSessionId_returnsLatestSessionId_whenMultipleOngoingSessionsExist() = runBlocking {
        // given: 여러 개의 진행 중 세션이 존재 (시간 순서가 다름)
        val oldSession = DrivingSession(sessionId = 1, startTime = Instant.now().minusSeconds(100))
        val latestSession = DrivingSession(sessionId = 2, startTime = Instant.now()) // 더 최신 세션
        dao.insertSession(oldSession)
        dao.insertSession(latestSession)

        // when: 진행 중인 세션 조회를 요청
        val ongoingSessionId = dao.getOngoingSessionId()

        // then: 가장 최근에 시작된 세션의 ID가 반환되어야 함 (ORDER BY startTime DESC)
        assertThat(ongoingSessionId).isEqualTo(2)
    }

    @Test
    fun getAllSessions_emitsNewList_whenSessionIsAdded() = runBlocking {
        // given: 초기에 세션이 하나 있음
        val initialSession = DrivingSession(sessionId = 1, startTime = Instant.now())
        dao.insertSession(initialSession)

        val flow = dao.getAllSessions()

        // then: 첫 번째 데이터는 세션 1개를 포함한 리스트여야 함
        val firstEmission = flow.first()
        assertThat(firstEmission).hasSize(1)
        assertThat(firstEmission.first().sessionId).isEqualTo(1)

        // when: 새로운 세션을 추가
        val newSession = DrivingSession(sessionId = 2, startTime = Instant.now())
        dao.insertSession(newSession)

        // then: Flow가 새로운 데이터를 방출하고, 리스트 크기는 2가 되어야 함
        val secondEmission = flow.first() // 다시 최신 값을 가져옴
        assertThat(secondEmission).hasSize(2)
    }
}