package com.devidea.chevy.storage

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTag(tag: DocumentTag?): String? = tag?.name

    @TypeConverter
    fun toTag(value: String?): DocumentTag? = value?.let { DocumentTag.valueOf(it) }
}