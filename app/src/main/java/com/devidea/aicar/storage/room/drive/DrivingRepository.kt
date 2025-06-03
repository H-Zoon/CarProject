package com.devidea.aicar.storage.room.drive

import com.devidea.aicar.storage.room.drive.DrivingDao.MonthlyStats
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

    /**
     * Retrieves all driving sessions stored locally, ordered by start time.
     *
     * @return Flow<List<DrivingSession>> 구독 가능한 Flow 형태로 모든 세션 리스트를 반환.
     *                                데이터베이스가 변경되면 구독자에게 자동으로 업데이트 전송.
     */
    fun getAllSessions(): Flow<List<DrivingSession>>

    /**
     * Retrieves all data points (DrivingDataPoint) associated with a given session.
     *
     * @param sessionId 조회할 DrivingSession의 고유 ID
     * @return Flow<List<DrivingDataPoint>> 구독 가능한 Flow 형태로 해당 세션의 데이터 포인트 리스트를 반환.
     *                                      데이터가 추가/변경되면 구독자에게 자동으로 업데이트 전송.
     */
    fun getSessionData(sessionId: Long): Flow<List<DrivingDataPoint>>

    /**
     * 새로운 주행 세션을 시작하고, 방금 생성된 세션의 ID를 반환합니다.
     *
     * - 이 메서드를 호출하면 새로운 DrivingSession 엔티티가 생성되어 로컬 DB에 저장됩니다.
     * - 반환된 Long 값은 새로 생성된 세션의 sessionId이며, 이후 데이터 포인트 저장 등에 사용됩니다.
     *
     * @return Long : 생성된 DrivingSession의 sessionId
     */
    suspend fun startSession(): Long

    /**
     * 지정된 sessionId를 가진 주행 세션을 종료합니다.
     *
     * - endTime을 현재 시각으로 업데이트하여 세션을 종료 처리합니다.
     *
     * @param sessionId 종료할 DrivingSession의 고유 ID
     */
    suspend fun stopSession(sessionId: Long)

    /**
     * 주행 중에 발생한 데이터 포인트(DrivingDataPoint)를 로컬에 저장합니다.
     *
     * @param point 저장할 DrivingDataPoint 객체 (sessionOwnerId, timestamp, sensor 값 등 포함)
     */
    suspend fun saveDataPoint(point: DrivingDataPoint)

    /**
     * 특정 세션을 읽음 처리(isRead = true)하여, 이미 확인한 세션임을 표시합니다.
     *
     * @param id 읽음 처리할 DrivingSession의 고유 ID
     */
    suspend fun markAsRead(id: Long)

    /**
     * 완료된 주행 세션과 해당 세션의 데이터 포인트를 원격 서버에 업로드하여 동기화합니다.
     *
     * - 세션 메타데이터와 데이터 포인트를 분리하여 순차적으로 서버로 전송할 수 있습니다.
     * - 업로드가 완료되면 로컬 DB의 해당 세션에 대해 isSynced 플래그를 업데이트하는 로직을 포함할 수 있습니다.
     *
     * @param sessionId 동기화할 DrivingSession의 고유 ID
     */
    suspend fun syncSession(sessionId: Long)

    /**
     * 지정된 세션을 로컬 데이터베이스에서 삭제합니다.
     *
     * @param sessionId 삭제할 DrivingSession의 고유 ID
     */
    suspend fun deleteSession(sessionId: Long)

    /**
     * 로컬 데이터베이스에 저장된 모든 주행 세션을 삭제합니다.
     */
    suspend fun deleteAllSessions()

    /**
     * 삭제된 세션을 복원하기 위해 DrivingSession 객체를 삽입합니다.
     *
     * - 주로 휴지통 기능 등을 통해 사용되며, 기존 세션 객체를 전달하면 같은 ID로 복원할 수 있습니다.
     *
     * @param session 복원할 DrivingSession 객체
     * @return Long 삽입된(또는 덮어쓴) 세션의 sessionId
     */
    suspend fun insertSession(session: DrivingSession): Long

    /**
     * 현재 기록이 진행 중인 세션의 ID를 조회합니다.
     *
     * - endTime이 NULL 상태인 가장 최근 세션을 찾아서 해당 sessionId를 반환합니다.
     * - 진행 중인 세션이 없으면 null 반환.
     *
     * @return Long? 진행 중인 세션의 sessionId 또는 null
     */
    fun getOngoingSessionId(): Long?

    /**
     * 주어진 시간 범위(startMillis ~ endMillis, 밀리초 단위)에 시작된 세션 목록을 구독 가능한 Flow로 반환합니다.
     *
     * - startTime이 startMillis 이상, endMillis 이하인 DrivingSession들을 조회합니다.
     * - Flow로 반환하므로, 데이터 변경 시 자동 업데이트가 발생합니다.
     *
     * @param startMillis 범위 시작 타임스탬프 (밀리초)
     * @param endMillis 범위 끝 타임스탬프 (밀리초)
     * @return Flow<List<DrivingSession>> 해당 범위 내 세션 리스트를 포함하는 Flow
     */
    fun getSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<DrivingSession>>

    /**
     * 지정한 날짜(00:00:00 ~ 23:59:59)에 해당하는 세션 목록을 구독 가능한 Flow로 반환합니다.
     *
     * - 내부적으로 getSessionsInRange를 사용하여 날짜 기준으로 범위를 계산하고 조회합니다.
     *
     * @param date 조회할 날짜 (LocalDate)
     * @return Flow<List<DrivingSession>> 해당 날짜에 시작된 세션 리스트를 포함하는 Flow
     */
    fun getSessionsByDate(date: LocalDate): Flow<List<DrivingSession>> =
        getSessionsInRange(
            startMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            endMillis   = date.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli() - 1
        )

    /**
     * 세션 요약 정보(DrivingSessionSummary)를 삽입하거나 업데이트합니다.
     *
     * - onConflict=REPLACE 전략을 사용하므로, 동일 sessionId가 존재하면 덮어쓰기가 수행됩니다.
     *
     * @param summary 저장 또는 갱신할 DrivingSessionSummary 객체
     */
    suspend fun saveSessionSummary(summary: DrivingSessionSummary)

    /**
     * 특정 세션의 요약 정보를 일회성으로 조회합니다.
     *
     * - 주로 세부 화면 진입 시 단발성으로 요약 데이터를 가져올 때 사용합니다.
     *
     * @param sessionId 조회할 DrivingSessionSummary의 sessionId
     * @return DrivingSessionSummary? 해당 세션 요약 객체 또는 null
     */
    suspend fun getSessionSummaryOnce(sessionId: Long): DrivingSessionSummary?

    /**
     * 특정 세션의 요약 정보를 구독 가능한 Flow 형태로 조회합니다.
     *
     * - 요약 데이터가 변경되면 구독자에게 자동으로 업데이트를 전송합니다.
     * - 예: 세션이 진행 중일 때 주기적으로 요약을 계산하여 UI에 보여줄 때 사용.
     *
     * @param sessionId 조회할 DrivingSessionSummary의 sessionId
     * @return Flow<DrivingSessionSummary?> 해당 세션 요약 객체를 포함하는 Flow (없으면 null)
     */
    fun getSessionSummaryFlow(sessionId: Long): Flow<DrivingSessionSummary?>

    /**
     * 지정된 시간 범위(startMillis ~ endMillis)에 해당하는 세션 요약들의 월별 통계(집계) 정보를 구독 가능한 Flow로 반환합니다.
     *
     * - DrivingSessionSummary 테이블에서 totalDistanceKm, averageKPL, fuelCost 등을 집계하여 반환합니다.
     * - Flow로 반환하므로, 요약 데이터가 추가/변경될 때 자동 업데이트가 발생합니다.
     *
     * @param startMillis 범위 시작 타임스탬프 (밀리초)
     * @param endMillis 범위 끝 타임스탬프 (밀리초)
     * @return Flow<MonthlyStats> 해당 기간 내 집계된 통계 정보를 포함하는 Flow
     */
    fun getSessionSummariesInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<MonthlyStats>
}