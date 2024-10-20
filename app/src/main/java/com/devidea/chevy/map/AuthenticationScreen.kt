package com.devidea.chevy.map

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devidea.chevy.viewmodel.MapViewModel

@Composable
fun AuthenticationScreen(
    isAuthenticating: Boolean,
    errorMessage: String?,
    onAuthenticate: () -> Unit,
    onErrorDismiss: () -> Unit
) {
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
            onClick = onAuthenticate,
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

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            AlertDialog(
                onDismissRequest = onErrorDismiss,
                title = { Text("에러 발생") },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(onClick = onErrorDismiss) {
                        Text("확인")
                    }
                }
            )
        }
    }
}