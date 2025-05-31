package com.devidea.aicar.storage.room.drive

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

    /**현제 기록중인 세션 조회*/
    override fun getOngoingSession(): DrivingSession? =
        dao.getOngoingSession()

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

    override suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
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

    override suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteSessionById(sessionId)
    }

    override suspend fun deleteAllSessions() = withContext(Dispatchers.IO) {
        dao.deleteAllSessions()
    }

    override suspend fun insertSession(session: DrivingSession) = withContext(Dispatchers.IO) {
        dao.insertSession(session)
    }

    override fun getSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<DrivingSession>> =
        dao.getSessionsInRange(startMillis, endMillis)

    override suspend fun saveSessionSummary(summary: DrivingSessionSummary) {
        dao.insertSessionSummary(summary)
    }

    override suspend fun getSessionSummaryOnce(sessionId: Long): DrivingSessionSummary? =
        dao.getSessionSummaryOnce(sessionId)


    override fun getSessionSummaryFlow(sessionId: Long): Flow<DrivingSessionSummary?> =
        dao.getSessionSummaryFlow(sessionId)


    override fun getSessionSummariesInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<DrivingSessionSummary>> =
        dao.getSummariesInRange(startMillis, endMillis)

    /**
     * 특정 달 (YearMonth) 기준으로 한 달치 통계를 Flow로 반환
     */
/*    fun getMonthlyStats(yearMonth: YearMonth): Flow<MonthlyStats> {
        val zone = ZoneId.systemDefault()
        val start = yearMonth.atDay(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = yearMonth.plusMonths(1).atDay(1)
            .atStartOfDay(zone).toInstant().toEpochMilli() - 1

        return dao.getMonthlyStats(start, end)
    }*/

}