package com.devidea.chevy.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Document(
    val address_name: String,
    val address_type: String?,
    val x: String,
    val y: String,
    val address: Address?,
    val road_address: RoadAddress?,
    val place_name: String?
) : Parcelable

