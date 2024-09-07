package com.devidea.chevy.response

import com.google.gson.annotations.SerializedName

data class Document(
    val address_name: String,
    val address_type: String,
    val x: String,
    val y: String,
    val address: Address?,
    val road_address: RoadAddress?,
    val place_name: String?
)

