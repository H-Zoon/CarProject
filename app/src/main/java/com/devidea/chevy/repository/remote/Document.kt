package com.devidea.chevy.repository.remote

data class Document(
    val address_name: String,
    val address_type: String?,
    val x: Double,
    val y: Double,
    val address: Address?,
    val road_address: RoadAddress?,
    val place_name: String?
)

