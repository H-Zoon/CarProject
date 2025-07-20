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
    val isRead: Boolean = false,
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DrivingSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionOwnerId"],
            onDelete = CASCADE,
        ),
    ],
    indices = [Index("sessionOwnerId")],
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
    val instantKPL: Float,
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = DrivingSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class DrivingSessionSummary(
    @PrimaryKey val sessionId: Long, // DrivingSession.sessionId 와 동일
    val totalDistanceKm: Float, // 총 주행 거리
    val averageSpeedKmh: Float, // 평균 속도
    val averageKPL: Float, // 평균 연비
    val fuelCost: Int, // 유류비
    val accelEvent: Int, // 급가속 이벤트
    val brakeEvent: Int, // 급감속 이벤트
)
