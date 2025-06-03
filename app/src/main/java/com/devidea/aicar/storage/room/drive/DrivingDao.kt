package com.devidea.aicar.storage.room.drive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing driving sessions and data points in Room database.
 */
@Dao
interface DrivingDao {

    @Insert
    suspend fun insertSession(session: DrivingSession): Long

    @Update
    suspend fun updateSession(session: DrivingSession)

    @Insert
    suspend fun insertDataPoint(point: DrivingDataPoint)

    /** 아직 종료되지 않은(끝난 시간이 없는) 세션을 하나만 가져오기 */
    @Query("""
      SELECT * FROM DrivingSession
      WHERE endTime IS NULL
      ORDER BY startTime DESC
      LIMIT 1
    """)
    fun getOngoingSession(): DrivingSession?

    @Query("SELECT * FROM DrivingSession ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<DrivingSession>>

    @Query("SELECT * FROM DrivingDataPoint WHERE sessionOwnerId = :sessionId ORDER BY timestamp")
    fun getDataPoints(sessionId: Long): Flow<List<DrivingDataPoint>>


    @Query("SELECT * FROM DrivingSession WHERE DATE(startTime) = :dateStr ORDER BY startTime DESC")
    fun getSessionsByDate(dateStr: String): Flow<List<DrivingSession>>

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
     * Retrieve a single session by its ID.
     */
    @Query("SELECT * FROM DrivingSession WHERE sessionId = :id")
    suspend fun getSessionById(id: Long): DrivingSession

    /**
     * Get all data points for a session as a one-time list.
     */
    @Query("SELECT * FROM DrivingDataPoint WHERE sessionOwnerId = :sessionId ORDER BY timestamp")
    suspend fun getDataPointsOnce(sessionId: Long): List<DrivingDataPoint>

    /** 기록 확인여부 */
    @Query("UPDATE DrivingSession SET isRead = 1 WHERE sessionId = :sessionId")
    suspend fun markAsRead(sessionId: Long)
    /**
     * Mark a session as synced with the remote server.
     * Requires DrivingSession.isSynced column in the entity.
     */
    @Query("UPDATE DrivingSession SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionSynced(sessionId: Long)

    /** 개별 세션 삭제 */
    @Query("DELETE FROM DrivingSession WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    /** 전체 세션 삭제 */
    @Query("DELETE FROM DrivingSession")
    suspend fun deleteAllSessions()

    @Query("""
  SELECT * FROM DrivingSession
  WHERE startTime BETWEEN :startMillis AND :endMillis
  ORDER BY startTime DESC
""")
    fun getSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<DrivingSession>>

    // --- DrivingSessionSummary 관련 메서드 추가 ---

    /** 요약 정보 삽입 또는 업데이트 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionSummary(summary: DrivingSessionSummary)

    /** 특정 세션의 요약 정보 조회 (일회성) */
    @Query("SELECT * FROM DrivingSessionSummary WHERE sessionId = :sessionId")
    suspend fun getSessionSummaryOnce(sessionId: Long): DrivingSessionSummary?

    /**
     * startTime이 startMillis와 endMillis 사이인 세션들의 DrivingSessionSummary들을 Flow로 가져오기
     */
    @Query("""
        SELECT summary.*
        FROM DrivingSessionSummary AS summary
        INNER JOIN DrivingSession AS session
          ON summary.sessionId = session.sessionId
        WHERE session.startTime BETWEEN :startMillis AND :endMillis
        ORDER BY session.startTime DESC
    """)
    fun getSummariesInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<DrivingSessionSummary>>

    // 월별 집계용 DTO
    data class MonthlyStats(
        val totalDistanceKm: Float,    // 누적 주행거리
        val averageKPL: Float,         // 평균 연비 (세션별 평균 연비의 단순 평균)
        val totalFuelCost: Int         // 누적 유류비
    )

    /**
     * startTime이 startMillis~endMillis 사이인 세션들의
     * 요약 데이터를 집계하여 반환
     */
    @Query("""
        SELECT
          IFNULL(SUM(s.totalDistanceKm), 0)   AS totalDistanceKm,
          IFNULL(AVG(s.averageKPL), 0)        AS averageKPL,
          IFNULL(SUM(s.fuelCost), 0)          AS totalFuelCost
        FROM DrivingSessionSummary AS s
        INNER JOIN DrivingSession   AS session
          ON s.sessionId = session.sessionId
        WHERE session.startTime BETWEEN :startMillis AND :endMillis
    """)
    fun getMonthlyStats(
        startMillis: Long,
        endMillis: Long
    ): Flow<MonthlyStats>

    /**
     * 특정 세션의 요약 정보를 Flow로 가져오기
     */
    @Query("SELECT * FROM DrivingSessionSummary WHERE sessionId = :sessionId")
    fun getSessionSummaryFlow(sessionId: Long): Flow<DrivingSessionSummary?>
}