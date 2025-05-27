package com.devidea.aicar.storage.room.drive

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity
data class DrivingSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val startTime: Instant,
    val endTime: Instant? = null,
    val isSynced: Boolean = false,
    val isRead: Boolean = false
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DrivingSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionOwnerId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("sessionOwnerId")]
)

data class DrivingDataPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionOwnerId: Long,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val rpm: Int,
    val speed: Int,
    val engineTemp: Int,
    val instantKPL: Float
)