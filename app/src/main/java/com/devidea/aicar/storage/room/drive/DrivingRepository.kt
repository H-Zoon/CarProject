package com.devidea.aicar.storage.room.drive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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


    /** 개별 세션 삭제 */
    suspend fun deleteSession(sessionId: Long)

    /** 전체 세션 삭제 */
    suspend fun deleteAllSessions()

    // DrivingRepository.kt
    suspend fun insertSession(session: DrivingSession): Long

    fun getSessionsByDate(dateStr: String): Flow<List<DrivingSession>>

    /** 하루 단위 범위 조회 */
    fun getSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<DrivingSession>>

    /** 지정한 날짜(00:00~23:59:59)에 해당하는 세션 조회 */
    fun getSessionsByDate(date: LocalDate): Flow<List<DrivingSession>> =
        getSessionsInRange(
            startMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            endMillis   = date.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli() - 1
        )
}