package com.devidea.chevy.k10s.navi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NavigateDocument (
    val address_name: String,
    val goalX: Double,
    val goalY: Double,
    val startX: Double,
    val startY: Double,
) : Parcelable