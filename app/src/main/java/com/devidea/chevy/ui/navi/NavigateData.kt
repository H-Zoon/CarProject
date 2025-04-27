package com.devidea.chevy.ui.navi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NavigateData(
    val addressName: String,
    val goalX: Double,
    val goalY: Double,
    val startX: Double,
    val startY: Double,
) : Parcelable
