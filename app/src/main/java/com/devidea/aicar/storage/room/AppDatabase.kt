package com.devidea.aicar.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devidea.aicar.storage.room.drive.DrivingDao
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingSession
import com.devidea.aicar.storage.room.notification.NotificationDao
import com.devidea.aicar.storage.room.notification.NotificationEntity

@Database(
    entities = [DrivingSession::class, DrivingDataPoint::class, NotificationEntity::class ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun driveDAO(): DrivingDao
    abstract fun notificationDao(): NotificationDao
}
