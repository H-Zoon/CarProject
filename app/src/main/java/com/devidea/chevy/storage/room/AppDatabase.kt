package com.devidea.chevy.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devidea.chevy.storage.room.Converters
import com.devidea.chevy.storage.room.document.DocumentDao
import com.devidea.chevy.storage.room.document.DocumentEntity
import com.devidea.chevy.storage.room.drive.DrivingDao
import com.devidea.chevy.storage.room.drive.DrivingDataPoint
import com.devidea.chevy.storage.room.drive.DrivingSession

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
