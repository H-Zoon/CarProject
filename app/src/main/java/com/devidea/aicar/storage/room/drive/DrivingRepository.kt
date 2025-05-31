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

    /**주행기록 저장을 시작하기 위한 sessionId 반환*/
    suspend fun startSession(): Long

    /**sessionId의 저장 종료*/
    suspend fun stopSession(sessionId: Long)

    /**주행데이터 (DrivingDataPoint) 저징*/
    suspend fun saveDataPoint(point: DrivingDataPoint)

    /**세션 확인여부 저장*/
    suspend fun markAsRead(id: Long)

    /**
     * Uploads a completed session and its data to the remote server.
     */
    suspend fun syncSession(sessionId: Long)

    /** 개별 세션 삭제 */
    suspend fun deleteSession(sessionId: Long)

    /** 전체 세션 삭제 */
    suspend fun deleteAllSessions()

    /** 삭제된 세션을 복원하기 위한 함수*/
    suspend fun insertSession(session: DrivingSession): Long

    /**기록이 진행중인 세션 조회*/
    fun getOngoingSession(): DrivingSession?

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


    /** 세션 요약 정보 저장 또는 업데이트 */
    suspend fun saveSessionSummary(summary: DrivingSessionSummary)

    /** 특정 세션의 요약 정보를 일회성으로 조회 */
    suspend fun getSessionSummaryOnce(sessionId: Long): DrivingSessionSummary?

    /** 특정 세션의 요약 정보를 Flow로 구독 */
    fun getSessionSummaryFlow(sessionId: Long): Flow<DrivingSessionSummary?>

    /** startTime이 startMillis ~ endMillis 사이인 요약 리스트를 Flow로 구독 */
    fun getSessionSummariesInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<DrivingSessionSummary>>
}