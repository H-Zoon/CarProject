package com.devidea.chevy.response

import com.google.gson.annotations.SerializedName

data class Meta (
    val total_count: Int,
    val pageable_count: Int,
    val is_end: Boolean,
    val same_name: SameName?
)