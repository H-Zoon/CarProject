package com.devidea.chevy.ui.main.compose

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devidea.chevy.ui.main.MainViewModel
import com.devidea.chevy.ui.map.MapActivity

@Composable
fun MAuthenticationScreen(viewModel: MainViewModel) {
    val authenticationSuccess by viewModel.authenticationSuccess.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()
    val context = LocalContext.current

    // 네비게이션 사이드 이펙트를 LaunchedEffect로 처리
    LaunchedEffect(authenticationSuccess) {
        if (authenticationSuccess) {
            val intent = Intent(context, MapActivity::class.java)
            context.startActivity(intent)
            // 네비게이션 후 추가 처리가 필요하다면 ViewModel에 이벤트 전달 (예: 네비게이션 완료 플래그 업데이트)
        }
    }

    // 인증이 완료되지 않은 경우의 UI
    if (!authenticationSuccess) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("B Activity 설명: 여기는 네비게이션이 설정된 B 액티비티입니다.")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.authenticateUser() },
                enabled = !isAuthenticating
            ) {
                if (isAuthenticating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text("인증 중...")
                    }
                } else {
                    Text("인증 후 B Activity로 이동")
                }
            }

            // 에러 메시지가 있을 경우 다이얼로그 표시
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
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
    }
}
