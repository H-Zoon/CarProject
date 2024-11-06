package com.devidea.chevy.ui.components

import com.devidea.chevy.viewmodel.MainViewModel.NavRoutes

data class CardItem(
    val title: String,
    val description: String,
    val route: NavRoutes
)