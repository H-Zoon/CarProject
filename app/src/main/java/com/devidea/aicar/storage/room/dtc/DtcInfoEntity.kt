package com.devidea.aicar.storage.room.dtc

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dtc_info")
data class DtcInfoEntity(
    @PrimaryKey val code: String,       // 예: "P0301"
    val title: String,                  // 예: "Cylinder 1 Misfire Detected"
    val description: String             // 예: "This code indicates a misfire in cylinder 1..."
)