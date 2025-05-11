package com.devidea.chevy.storage.room.drive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Repository interface for driving sessions and data points.
 * Handles both local database operations and background synchronization.
 */
interface DrivingRepository {
    fun getAllSessions(): Flow<List<DrivingSession>>
    fun getSessionData(sessionId: Long): Flow<List<DrivingDataPoint>>

    suspend fun startSession(): Long
    suspend fun stopSession(sessionId: Long)
    suspend fun saveDataPoint(point: DrivingDataPoint)

    /**
     * Uploads a completed session and its data to the remote server.
     */
    suspend fun syncSession(sessionId: Long)
}

/**
 * Default implementation of [DrivingRepository].
 * Uses Room for local storage and Retrofit for remote API.
 */
class DrivingRepositoryImpl @Inject constructor(
    private val dao: DrivingDao,
    //private val api: DrivingApi
) : DrivingRepository {

    override fun getAllSessions(): Flow<List<DrivingSession>> =
        dao.getAllSessions()

    override fun getSessionData(sessionId: Long): Flow<List<DrivingDataPoint>> =
        dao.getDataPoints(sessionId)

    override suspend fun startSession(): Long = withContext(Dispatchers.IO) {
        val session = DrivingSession(startTime = Instant.now())
        dao.insertSession(session)
    }

    override suspend fun stopSession(sessionId: Long) = withContext(Dispatchers.IO) {
        // Fetch, update endTime, and persist
        val session = dao.getSessionById(sessionId)
        val updated = session.copy(endTime = Instant.now())
        dao.updateSession(updated)
    }

    override suspend fun saveDataPoint(point: DrivingDataPoint) = withContext(Dispatchers.IO) {
        dao.insertDataPoint(point)
    }

    override suspend fun syncSession(sessionId: Long) = withContext(Dispatchers.IO) {
        /*// 1. Retrieve session and its points
        val session = dao.getSessionById(sessionId)
        val points = dao.getDataPointsOnce(sessionId)

        // 2. Upload session metadata
        api.uploadSession(session)

        // 3. Upload data points in batch
        api.uploadDataPoints(points)

        // 4. Optionally mark session as synced locally
        dao.markSessionSynced(sessionId)*/
    }
}