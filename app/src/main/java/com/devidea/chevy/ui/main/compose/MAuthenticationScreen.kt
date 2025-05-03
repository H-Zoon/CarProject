package com.devidea.chevy.ui.main.compose

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.chevy.ui.main.MainViewModel
import com.devidea.chevy.ui.map.compose.MapScreenHost

@Composable
fun MAuthenticationScreen(modifier: Modifier = Modifier, viewModel: MainViewModel = hiltViewModel()) {
    val authenticationSuccess by viewModel.authenticationSuccess.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()

    if (!authenticationSuccess) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("B Activity 설명: 여기는 네비게이션이 설정된 B 액티비티입니다.")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = modifier,
                onClick = { viewModel.authenticateUser() },
                enabled = !isAuthenticating
            ) {
                if (isAuthenticating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = modifier
                        )
                        Text("인증 중...")
                    }
                } else {
                    Text("인증 후 B Activity로 이동")
                }
            }

            // 에러 메시지가 있을 경우 다이얼로그 표시
            if (errorMessage != null) {
                Spacer(modifier = modifier.height(16.dp))
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("에러 발생") },
                    text = { Text(errorMessage!!) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearError() }) {
                            Text("확인")
                        }
                    }
                )
            }
        }
    } else {
        MapScreenHost()
    }
}
