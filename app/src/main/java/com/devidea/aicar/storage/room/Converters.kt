package com.devidea.aicar.storage.room

import androidx.room.TypeConverter
import com.devidea.aicar.storage.room.document.DocumentTag
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTag(tag: DocumentTag?): String? = tag?.name

    @TypeConverter
    fun toTag(value: String?): DocumentTag? = value?.let { DocumentTag.valueOf(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? =
        value?.toEpochMilli()

    // Long (epoch milli) → Instant 변환
    @TypeConverter
    fun toInstant(value: Long?): Instant? =
        value?.let { Instant.ofEpochMilli(it) }
}