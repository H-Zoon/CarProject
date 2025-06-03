package com.devidea.aicar.storage.room.drive

import com.devidea.aicar.storage.room.drive.DrivingDao.MonthlyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Default implementation of [DrivingRepository].
 * Uses Room for local storage and Retrofit for remote API.
 */
class DrivingRepositoryImpl @Inject constructor(
    private val dao: DrivingDao,
) : DrivingRepository {

    /**
     * 현재 기록 중인 세션을 조회합니다.
     *
     * @return DrivingSession? : 종료되지 않은 세션(끝난 시간이 없는 세션)이 있으면 해당 세션, 없으면 null
     */
    override fun getOngoingSessionId(): Long? =
        dao.getOngoingSessionId()

    /**
     * 모든 DrivingSession 엔티티를 최신 순으로 Flow 형태로 가져옵니다.
     *
     * @return Flow<List<DrivingSession>> : 모든 세션 리스트를 포함하는 Flow
     */
    override fun getAllSessions(): Flow<List<DrivingSession>> =
        dao.getAllSessions()

    /**
     * 특정 세션에 속한 DrivingDataPoint 목록을 Flow 형태로 가져옵니다.
     *
     * @param sessionId: 조회할 세션의 고유 ID
     * @return Flow<List<DrivingDataPoint>> : 해당 세션의 데이터 포인트 리스트를 포함하는 Flow
     */
    override fun getSessionData(sessionId: Long): Flow<List<DrivingDataPoint>> =
        dao.getDataPoints(sessionId)

    /**
     * 새로운 DrivingSession을 생성하고 시작 시간을 현재 시각으로 설정하여 데이터베이스에 삽입합니다.
     *
     * @return Long : 삽입된 세션의 고유 ID (sessionId)
     */
    override suspend fun startSession(): Long = withContext(Dispatchers.IO) {
        val session = DrivingSession(startTime = Instant.now())
        dao.insertSession(session)
    }

    /**
     * 지정된 sessionId를 가진 DrivingSession을 중지하고, 종료 시간을 현재 시각으로 업데이트합니다.
     *
     * @param sessionId: 종료할 세션의 고유 ID
     */
    override suspend fun stopSession(sessionId: Long) = withContext(Dispatchers.IO) {
        // 1) 기존 세션을 조회
        val session = dao.getSessionById(sessionId)
        // 2) endTime을 현재 시각으로 설정하여 새로운 객체 생성
        val updated = session.copy(endTime = Instant.now())
        // 3) 업데이트된 세션을 데이터베이스에 저장
        dao.updateSession(updated)
    }

    /**
     * 새로운 DrivingDataPoint를 데이터베이스에 삽입합니다.
     *
     * @param point: 삽입할 데이터 포인트 객체
     */
    override suspend fun saveDataPoint(point: DrivingDataPoint) = withContext(Dispatchers.IO) {
        dao.insertDataPoint(point)
    }

    /**
     * 지정된 세션을 읽음 처리(isRead = 1)합니다.
     *
     * @param id: 읽음 처리할 세션의 고유 ID
     */
    override suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
    }

    /**
     * 세션 데이터를 원격 서버와 동기화합니다.
     * (현재는 주석 처리되어 있으며, 실제 API 연동 로직을 추가해야 함)
     *
     * @param sessionId: 동기화할 세션의 고유 ID
     */
    override suspend fun syncSession(sessionId: Long) = withContext(Dispatchers.IO) {
        /*
        // 1. 세션 및 데이터 포인트 조회
        val session = dao.getSessionById(sessionId)
        val points = dao.getDataPointsOnce(sessionId)

        // 2. 세션 메타데이터 업로드
        api.uploadSession(session)

        // 3. 데이터 포인트 일괄 업로드
        api.uploadDataPoints(points)

        // 4. 동기화 완료로 표시
        dao.markSessionSynced(sessionId)
        */
    }

    /**
     * 지정된 세션을 데이터베이스에서 삭제합니다.
     *
     * @param sessionId: 삭제할 세션의 고유 ID
     */
    override suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteSessionById(sessionId)
    }

    /**
     * 데이터베이스의 모든 DrivingSession을 삭제합니다.
     */
    override suspend fun deleteAllSessions() = withContext(Dispatchers.IO) {
        dao.deleteAllSessions()
    }

    /**
     * 새로운 DrivingSession 엔티티를 데이터베이스에 삽입합니다.
     *
     * @param session: 삽입할 세션 객체
     */
    override suspend fun insertSession(session: DrivingSession) = withContext(Dispatchers.IO) {
        dao.insertSession(session)
    }

    /**
     * 주어진 시간 범위(startMillis ~ endMillis) 내에 시작된 DrivingSession 목록을 Flow로 반환합니다.
     *
     * @param startMillis: 범위 시작 타임스탬프 (밀리초)
     * @param endMillis: 범위 끝 타임스탬프 (밀리초)
     * @return Flow<List<DrivingSession>> : 해당 범위 세션 리스트를 포함하는 Flow
     */
    override fun getSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<DrivingSession>> =
        dao.getSessionsInRange(startMillis, endMillis)

    /**
     * DrivingSessionSummary 엔티티를 삽입 또는 업데이트합니다.
     *
     * @param summary: 삽입 또는 업데이트할 세션 요약 객체
     */
    override suspend fun saveSessionSummary(summary: DrivingSessionSummary) {
        dao.insertSessionSummary(summary)
    }

    /**
     * 단일 세션 요약 정보를 ID로 조회합니다. (일회성 호출)
     *
     * @param sessionId: 조회할 세션 요약의 고유 ID
     * @return DrivingSessionSummary? : 해당 세션 요약 객체, 없으면 null
     */
    override suspend fun getSessionSummaryOnce(sessionId: Long): DrivingSessionSummary? =
        dao.getSessionSummaryOnce(sessionId)

    /**
     * 특정 세션 요약 정보를 Flow 형태로 구독하여 가져옵니다.
     *
     * @param sessionId: 조회할 세션 요약의 고유 ID
     * @return Flow<DrivingSessionSummary?> : 해당 세션 요약 객체를 포함하는 Flow (없으면 null)
     */
    override fun getSessionSummaryFlow(sessionId: Long): Flow<DrivingSessionSummary?> =
        dao.getSessionSummaryFlow(sessionId)

    /**
     * 특정 기간(startMillis ~ endMillis)에 해당하는 한 달치 통계(월별 집계)를 Flow 형태로 반환합니다.
     *
     * @param startMillis: 통계 범위 시작 타임스탬프 (밀리초)
     * @param endMillis: 통계 범위 끝 타임스탬프 (밀리초)
     * @return Flow<MonthlyStats> : 월별 누적 거리, 평균 연비, 누적 유류비를 포함하는 Flow
     */
    override fun getSessionSummariesInRange(startMillis: Long, endMillis: Long): Flow<MonthlyStats> {
        return dao.getMonthlyStats(startMillis, endMillis)
    }
}