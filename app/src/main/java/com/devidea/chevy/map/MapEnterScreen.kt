package com.devidea.chevy.map

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.devidea.chevy.viewmodel.MapViewModel

@Composable
fun MapEnterScreen(
    viewModel: MapViewModel,
    activity: Activity
) {
    val authenticationSuccess by viewModel.authenticationSuccess.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()

    if (authenticationSuccess) {
        MainScreen(viewModel, activity)
    } else {
        AuthenticationScreen(
            isAuthenticating = isAuthenticating,
            errorMessage = errorMessage,
            onAuthenticate = { viewModel.authenticateUser() },
            onErrorDismiss = { viewModel.clearError() }
        )
    }
}