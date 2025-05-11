package com.devidea.chevy.storage.room.drive

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

    @Query("SELECT * FROM DrivingSession ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<DrivingSession>>

    @Query("SELECT * FROM DrivingDataPoint WHERE sessionOwnerId = :sessionId ORDER BY timestamp")
    fun getDataPoints(sessionId: Long): Flow<List<DrivingDataPoint>>

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

    /**
     * Mark a session as synced with the remote server.
     * Requires DrivingSession.isSynced column in the entity.
     */
    @Query("UPDATE DrivingSession SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionSynced(sessionId: Long)
}