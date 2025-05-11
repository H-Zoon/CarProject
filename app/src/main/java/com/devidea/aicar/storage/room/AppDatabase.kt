package com.devidea.aicar.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devidea.aicar.storage.room.document.DocumentDao
import com.devidea.aicar.storage.room.document.DocumentEntity
import com.devidea.aicar.storage.room.drive.DrivingDao
import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.storage.room.drive.DrivingSession

@Database(
    entities = [DocumentEntity::class, DrivingSession::class, DrivingDataPoint::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun driveDAO(): DrivingDao
}
