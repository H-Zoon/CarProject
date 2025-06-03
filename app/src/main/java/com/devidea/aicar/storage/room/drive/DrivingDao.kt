package com.devidea.aicar.storage.room.drive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DrivingDao {

    /**
     * 새로운 DrivingSession 엔티티를 데이터베이스에 삽입합니다.
     *
     * @param session: 삽입할 세션 객체
     * @return Long: 삽입된 세션의 고유 ID (sessionId)
     */
    @Insert
    suspend fun insertSession(session: DrivingSession): Long

    /**
     * 기존 DrivingSession 엔티티를 업데이트합니다.
     *
     * @param session: 업데이트할 세션 객체 (sessionId가 이미 존재해야 함)
     */
    @Update
    suspend fun updateSession(session: DrivingSession)

    /**
     * 새로운 DrivingDataPoint 엔티티를 데이터베이스에 삽입합니다.
     *
     * @param point: 삽입할 데이터 포인트 객체
     */
    @Insert
    suspend fun insertDataPoint(point: DrivingDataPoint)

    /**
     * 아직 종료되지 않은(끝난 시간이 없는) 세션의 ID를 하나만 가져옵니다.
     *
     * - endTime이 NULL인 세션 중 startTime 기준 최신 세션 하나의 sessionId만 선택
     * - 만약 진행 중인 세션이 없으면 null 반환
     */
    @Query(
        """
        SELECT sessionId 
        FROM DrivingSession
        WHERE endTime IS NULL
        ORDER BY startTime DESC
        LIMIT 1
        """
    )
    fun getOngoingSessionId(): Long?

    /**
     * 모든 DrivingSession 엔티티를 최신 순(startTime 내림차순)으로 Flow 형태로 가져옵니다.
     *
     * - 구독자가 있으면 실시간으로 데이터 변경을 통지
     *
     * @return Flow<List<DrivingSession>>: 모든 세션 리스트를 담은 Flow
     */
    @Query("SELECT * FROM DrivingSession ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<DrivingSession>>

    /**
     * 특정 세션ID에 속한 DrivingDataPoint 목록을 시간 순으로 가져옵니다.
     *
     * - sessionOwnerId가 sessionId인 모든 데이터 포인트를 timestamp 기준 오름차순 정렬
     * - Flow로 반환하여 실시간 업데이트 제공
     *
     * @param sessionId: 조회할 세션의 고유 ID
     * @return Flow<List<DrivingDataPoint>>: 해당 세션의 데이터 포인트 리스트를 담은 Flow
     */
    @Query("SELECT * FROM DrivingDataPoint WHERE sessionOwnerId = :sessionId ORDER BY timestamp")
    fun getDataPoints(sessionId: Long): Flow<List<DrivingDataPoint>>

    /**
     * 지정한 날짜(yyyy-MM-dd 형식)에 시작된 DrivingSession 목록을 최신 순으로 가져옵니다.
     *
     * - startTime 날짜 문자열이 dateStr과 동일한 세션을 모두 조회
     * - Flow로 반환하여 실시간 업데이트 제공
     *
     * @param dateStr: "yyyy-MM-dd" 형식의 날짜 문자열
     * @return Flow<List<DrivingSession>>: 해당 날짜 세션 리스트를 담은 Flow
     */
    @Query("SELECT * FROM DrivingSession WHERE DATE(startTime) = :dateStr ORDER BY startTime DESC")
    fun getSessionsByDate(dateStr: String): Flow<List<DrivingSession>>

    /**
     * 주어진 연도(yearStr)와 월(monthStr)에 해당하는 세션이 시작된 날짜 리스트를 중복 없이 가져옵니다.
     *
     * - DrivingSession 테이블에서 startTime의 연도/월을 추출하여
     *   substr(startTime,1,4) = yearStr AND substr(startTime,6,2) = monthStr 조건으로 필터링
     * - 중복 제거(DISTINCT)하여 날짜 문자열(String)만 반환
     * - Flow로 반환하여 실시간 업데이트 제공
     *
     * @param yearStr: 연도("yyyy" 형식) 문자열
     * @param monthStr: 월("MM" 형식) 문자열
     * @return Flow<List<String>>: 해당 월에 세션이 시작된 날짜 리스트
     */
    @Query(
        """
        SELECT DISTINCT DATE(startTime)
        FROM DrivingSession
        WHERE substr(startTime,1,4) = :yearStr
          AND substr(startTime,6,2) = :monthStr
        """
    )
    fun getSessionDatesInMonth(yearStr: String, monthStr: String): Flow<List<String>>

    /**
     * 단일 DrivingSession 엔티티를 ID로 조회합니다. (일회성 호출, suspend)
     *
     * @param id: 조회할 세션의 고유 ID (sessionId)
     * @return DrivingSession: 해당 세션 객체 (존재하지 않으면 예외 발생)
     */
    @Query("SELECT * FROM DrivingSession WHERE sessionId = :id")
    suspend fun getSessionById(id: Long): DrivingSession

    /**
     * 특정 세션의 DrivingDataPoint 목록을 한 번에 List로 가져옵니다. (일회성 호출, suspend)
     *
     * @param sessionId: 조회할 세션의 고유 ID
     * @return List<DrivingDataPoint>: 해당 세션의 데이터 포인트 리스트
     */
    @Query("SELECT * FROM DrivingDataPoint WHERE sessionOwnerId = :sessionId ORDER BY timestamp")
    suspend fun getDataPointsOnce(sessionId: Long): List<DrivingDataPoint>

    /**
     * 세션이 읽혔는지 여부를 표시하기 위해 isRead 컬럼을 1로 업데이트합니다.
     *
     * @param sessionId: 읽음 처리할 세션의 고유 ID
     */
    @Query("UPDATE DrivingSession SET isRead = 1 WHERE sessionId = :sessionId")
    suspend fun markAsRead(sessionId: Long)

    /**
     * 해당 세션을 원격 서버에 동기화했음을 표시하기 위해 isSynced 컬럼을 1로 업데이트합니다.
     *
     * @param sessionId: 동기화 완료로 표시할 세션의 고유 ID
     */
    @Query("UPDATE DrivingSession SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionSynced(sessionId: Long)

    /**
     * 특정 세션을 데이터베이스에서 삭제합니다.
     *
     * @param sessionId: 삭제할 세션의 고유 ID
     */
    @Query("DELETE FROM DrivingSession WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    /**
     * 모든 DrivingSession 엔티티를 데이터베이스에서 삭제합니다.
     */
    @Query("DELETE FROM DrivingSession")
    suspend fun deleteAllSessions()

    /**
     * 주어진 시간 범위(startMillis ~ endMillis, 밀리초 단위)에 시작된 세션 목록을 최신 순으로 가져옵니다.
     *
     * - startTime 값이 startMillis 이상, endMillis 이하인 세션을 조회
     * - Flow로 반환하여 실시간 업데이트 제공
     *
     * @param startMillis: 범위 시작 타임스탬프 (밀리초)
     * @param endMillis: 범위 끝 타임스탬프 (밀리초)
     * @return Flow<List<DrivingSession>>: 해당 범위 세션 리스트를 담은 Flow
     */
    @Query(
        """
      SELECT * FROM DrivingSession
      WHERE startTime BETWEEN :startMillis AND :endMillis
      ORDER BY startTime DESC
    """
    )
    fun getSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<DrivingSession>>

    // --- DrivingSessionSummary 관련 메서드 추가 ---

    /**
     * DrivingSessionSummary 엔티티를 삽입하거나, 이미 존재한다면 덮어씁니다.
     *
     * - onConflict = REPLACE 전략을 사용하여 동일한 sessionId가 있으면 업데이트
     *
     * @param summary: 삽입 또는 업데이트할 세션 요약 객체
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionSummary(summary: DrivingSessionSummary)

    /**
     * 단일 세션 요약 정보를 ID로 조회합니다. (일회성 호출, suspend)
     *
     * @param sessionId: 조회할 세션 요약의 고유 ID
     * @return DrivingSessionSummary?: 해당 세션 요약 객체 (존재하지 않으면 null)
     */
    @Query("SELECT * FROM DrivingSessionSummary WHERE sessionId = :sessionId")
    suspend fun getSessionSummaryOnce(sessionId: Long): DrivingSessionSummary?

    /**
     * 주어진 시간 범위(startMillis ~ endMillis) 내에 시작된 세션들의 요약 정보를 Flow로 가져옵니다.
     *
     * - DrivingSessionSummary 테이블과 DrivingSession 테이블을 INNER JOIN하여
     *   startTime이 범위 내에 속하는 세션들의 요약만 조회
     * - 세션 시작 시간 기준 내림차순 정렬
     * - Flow로 반환하여 실시간 업데이트 제공
     *
     * @param startMillis: 범위 시작 타임스탬프 (밀리초)
     * @param endMillis: 범위 끝 타임스탬프 (밀리초)
     * @return Flow<List<DrivingSessionSummary>>: 해당 범위 세션들의 요약 리스트를 담은 Flow
     */
    @Query(
        """
        SELECT summary.*
        FROM DrivingSessionSummary AS summary
        INNER JOIN DrivingSession AS session
          ON summary.sessionId = session.sessionId
        WHERE session.startTime BETWEEN :startMillis AND :endMillis
        ORDER BY session.startTime DESC
    """
    )
    fun getSummariesInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<DrivingSessionSummary>>

    // 월별 집계용 DTO
    data class MonthlyStats(
        val totalDistanceKm: Float,    // 누적 주행거리 (해당 기간 내 합계)
        val averageKPL: Float,         // 평균 연비 (세션별 평균 연비의 단순 평균)
        val totalFuelCost: Int         // 누적 유류비 (해당 기간 내 합계)
    )

    /**
     * 주어진 시간 범위(startMillis ~ endMillis) 내에 시작된 세션들의 요약 데이터를 집계하여 반환합니다.
     *
     * - DrivingSessionSummary 테이블과 DrivingSession 테이블을 INNER JOIN하여
     *   startTime이 범위 내에 속하는 세션들만 필터링
     * - SUM, AVG, SUM 집계 함수를 사용하여 totalDistanceKm, averageKPL, totalFuelCost를 계산
     * - Flow로 반환하여 실시간 업데이트 제공
     *
     * @param startMillis: 범위 시작 타임스탬프 (밀리초)
     * @param endMillis: 범위 끝 타임스탬프 (밀리초)
     * @return Flow<MonthlyStats>: 집계 결과를 담은 Flow
     */
    @Query(
        """
        SELECT
          IFNULL(SUM(s.totalDistanceKm), 0)   AS totalDistanceKm,
          IFNULL(AVG(s.averageKPL), 0)        AS averageKPL,
          IFNULL(SUM(s.fuelCost), 0)          AS totalFuelCost
        FROM DrivingSessionSummary AS s
        INNER JOIN DrivingSession   AS session
          ON s.sessionId = session.sessionId
        WHERE session.startTime BETWEEN :startMillis AND :endMillis
    """
    )
    fun getMonthlyStats(
        startMillis: Long,
        endMillis: Long
    ): Flow<MonthlyStats>

    /**
     * 특정 세션의 요약 정보를 Flow로 가져옵니다.
     *
     * - DrivingSessionSummary 테이블에서 sessionId와 일치하는 레코드를 구독(Flow) 형태로 반환
     * - 요약 정보가 업데이트되면 자동으로 구독자에게 변경 내용이 전달
     *
     * @param sessionId: 조회할 세션 요약의 고유 ID
     * @return Flow<DrivingSessionSummary?>: 해당 세션 요약 객체를 담은 Flow (없으면 null)
     */
    @Query("SELECT * FROM DrivingSessionSummary WHERE sessionId = :sessionId")
    fun getSessionSummaryFlow(sessionId: Long): Flow<DrivingSessionSummary?>
}