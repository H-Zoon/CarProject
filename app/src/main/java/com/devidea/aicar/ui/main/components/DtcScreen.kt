package com.devidea.aicar.ui.main.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devidea.aicar.service.ConnectionEvent
import com.devidea.aicar.ui.main.viewmodels.DtcViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtcScreen(viewModel: DtcViewModel = hiltViewModel(), modifier: Modifier) {
    val dtcList by viewModel.dtcList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedInfo by viewModel.selectedInfo.collectAsState()

    // Toast를 띄우기 위한 Context
    val context = LocalContext.current

    // 경고 다이얼로그 표시 여부
    var showPollingDialog by remember { mutableStateOf(false) }

    // 어떤 작업을 지연할 것인지 (FETCH = 조회, CLEAR = 삭제)
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    // 사용자가 고장코드를 탭했을 때 상세 조회
    val selectedCode = remember { mutableStateOf<String?>(null) }
    selectedCode.value?.let { code ->
        LaunchedEffect(code) {
            viewModel.fetchInfo(code)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Connection 상태 표시
        Text(
            text = when (connectionState) {
                ConnectionEvent.Connected -> "연결됨"
                ConnectionEvent.Connecting -> "연결 중..."
                ConnectionEvent.Scanning -> "스캔 중..."
                else -> "연결되지 않음"
            },
            color = if (connectionState == ConnectionEvent.Connected) Color.Green else Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 2. 에러 메시지
        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 3. 로딩 인디케이터 또는 DTC 리스트
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (dtcList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "고장코드 없음")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(dtcList.size) { idx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedCode.value = dtcList[idx]
                                }
                        ) {
                            Text(
                                text = dtcList[idx],
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {

            // 4. 고장코드가 있을 때만 맨 아래에 삭제 버튼 노출
            if (dtcList.isNotEmpty()) {
                Button(
                    onClick = {
                        // 1) 연결 상태 체크
                        if (connectionState != ConnectionEvent.Connected) {
                            Toast.makeText(context, "차량과 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // 2) Polling 중인지 체크
                        if (viewModel.isPolling) {
                            pendingAction = PendingAction.CLEAR
                            showPollingDialog = true
                        } else {
                            viewModel.clearDtcCodes()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = "고장코드 삭제")
                }
            }

            Button(
                modifier = Modifier
                    .padding(16.dp),
                onClick = {
                    // 1) 연결 상태 확인
                    if (connectionState != ConnectionEvent.Connected) {
                        Toast.makeText(context, "차량과 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // 2) Polling 중인지 확인
                    if (viewModel.isPolling) {
                        pendingAction = PendingAction.FETCH
                        showPollingDialog = true
                    } else {
                        viewModel.fetchDtcCodes()
                    }
                }
            ) {
                Text(text = "진단")
            }
        }
    }

    // 5. 고장코드 상세 정보 팝업 다이얼로그
    if (selectedCode.value != null) {
        AlertDialog(
            onDismissRequest = { selectedCode.value = null },
            title = { Text(text = selectedCode.value ?: "") },
            text = {
                selectedInfo?.let {
                    Column {
                        Text(text = it.title, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it.description)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCode.value = null }) {
                    Text("확인")
                }
            }
        )
    }

    // 6. Polling 중일 때 “현재 기록중입니다” 경고 다이얼로그
    if (showPollingDialog) {
        AlertDialog(
            onDismissRequest = {
                showPollingDialog = false
                pendingAction = null
            },
            title = { Text("경고") },
            text = { Text("현재 주향 기록중입니다. 주행기록이 일시 정지됩니다. \n 계속하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    // 1) Polling 일시정지
                    viewModel.pausePolling()

                    // 2) 보류된 액션 수행
                    when (pendingAction) {
                        PendingAction.FETCH -> viewModel.fetchDtcCodes()
                        PendingAction.CLEAR -> viewModel.clearDtcCodes()
                        null -> { /* do nothing */
                        }
                    }

                    // 3) Polling 재개
                    viewModel.resumePolling()

                    // 다이얼로그 닫기
                    showPollingDialog = false
                    pendingAction = null
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPollingDialog = false
                    pendingAction = null
                }) {
                    Text("취소")
                }
            }
        )
    }
}

private enum class PendingAction {
    FETCH,
    CLEAR
}