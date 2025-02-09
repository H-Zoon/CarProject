package com.devidea.chevy.ui.screen.map

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.devidea.chevy.ui.screen.navi.KNavi
import com.devidea.chevy.viewmodel.MainViewModel
import com.devidea.chevy.viewmodel.MapViewModel

@Composable
fun MapEnterScreen(
    viewModel: MapViewModel = hiltViewModel(),
    navHostController: NavHostController
) {
    val authenticationSuccess by viewModel.authenticationSuccess.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()

    if (authenticationSuccess) {
        MainScreen(viewModel= viewModel, navHostController = navHostController)
    } else {
        AuthenticationScreen(
            isAuthenticating = isAuthenticating,
            errorMessage = errorMessage,
            onAuthenticate = { viewModel.authenticateUser() },
            onErrorDismiss = { viewModel.clearError() }
        )
    }
}