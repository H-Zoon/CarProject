package com.devidea.aicar.storage.room.notification

import com.devidea.aicar.storage.room.drive.DrivingDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor( private val notificationDao: NotificationDao):
    NotificationRepository{

    override fun observeAllNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.observeAll()
    }

    override suspend fun insertNotification(notification: NotificationEntity) {
        notificationDao.insert(notification)
    }

    override suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    override suspend fun deleteById(id: Long) {
        notificationDao.deleteById(id)
    }

    override suspend fun clearAllNotifications() {
        notificationDao.clearAll()
    }
}