package com.devidea.aicar.storage.room.drive

import androidx.room.Dao
import androidx.room.Insert
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
    fun getOngoingSession(): Flow<DrivingSession?>

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
}