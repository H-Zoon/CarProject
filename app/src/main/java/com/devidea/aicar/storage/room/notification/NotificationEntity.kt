package com.devidea.aicar.storage.room.notification

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // 알림 고유 ID
    val title: String, // 알림 제목
    val body: String, // 알림 내용
    val timestamp: Instant, // 발행 시각 (Converters 로 Long ↔ Instant 변환)
    val isRead: Boolean = false, // 읽음 여부
)
