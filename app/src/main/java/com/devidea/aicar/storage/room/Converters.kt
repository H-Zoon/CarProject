package com.devidea.aicar.storage.room

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    // Long (epoch milli) → Instant 변환
    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
