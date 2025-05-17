package com.devidea.aicar.storage.room.notification
import kotlinx.coroutines.flow.Flow

/**
 * 알림 관련 데이터를 제공하는 인터페이스
 */
interface NotificationRepository {

    /** 전체 알림을 시간 내림차순으로 Flow 로 관찰 */
    fun observeAllNotifications(): Flow<List<NotificationEntity>>

    /** 새로운 알림을 삽입 또는 업데이트 */
    suspend fun insertNotification(notification: NotificationEntity)

    /** 특정 알림을 읽음 처리 */
    suspend fun markAsRead(id: Long)

    /** 전체 알림을 읽음 처리 */
    suspend fun markAllAsRead()

    /** 특정 알림을 삭제 */
    suspend fun deleteById(id: Long)

    /** 모든 알림을 삭제 */
    suspend fun clearAllNotifications()
}