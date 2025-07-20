package com.devidea.aicar.storage.room.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    /** 전체 알림을 시간 내림차순으로 Flow 로 관찰 */
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    /** 새로운 알림 삽입 (충돌 시 대체) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    /** 특정 ID 알림을 읽음 처리 */
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    /** 전체 읽음 처리 */
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    /** 특정 ID 알림 삭제 */
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 전체 알림 삭제 */
    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
