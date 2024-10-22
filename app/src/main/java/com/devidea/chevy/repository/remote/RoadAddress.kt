package com.devidea.chevy.repository.remote

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoadAddress(
    val address_name: String,
    val region_1depth_name: String,
    val region_2depth_name: String,
    val region_3depth_name: String,
    val road_name: String,
    val underground_yn: String,
    val main_building_no: String,
    val sub_building_no: String,
    val building_name: String?,
    val zone_no: String,
    val x: String,
    val y: String
) : Parcelable