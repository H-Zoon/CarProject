package com.devidea.chevy.ui.components

import com.devidea.chevy.viewmodel.MainViewModel

data class CardItem(
    val title: String,
    val description: String,
    val date: MainViewModel.NavRoutes
)