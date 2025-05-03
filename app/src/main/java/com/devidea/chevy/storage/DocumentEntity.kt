package com.devidea.chevy.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    indices = [Index(value = ["tag"], unique = true)] // 하나의 Home, Office 태그만 허용
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val place_name: String,
    val category_name: String,
    val category_group_code: String,
    val category_group_name: String,
    val phone: String,
    val address_name: String,
    val road_address_name: String,
    val x: Double,
    val y: Double,
    val place_url: String,
    val distance: String?,
    val tag: DocumentTag? = null,
    val isFavorite: Boolean = false
)