package com.devidea.chevy.ui.navi.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.devidea.chevy.ui.navi.NaviViewModel
import com.kakaomobility.knsdk.guidance.knguidance.KNGuideState

@Composable
fun Navigation(
    viewModel: NaviViewModel
) {
    AndroidView(
        factory = { viewModel.provideNavView(it) },
        modifier = Modifier.fillMaxSize()
    )

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (showExitDialog) {
            showExitDialog = false
        } else {
            showExitDialog = true
        }
    }

    // 다이얼로그 표시
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                // 사용자가 다이얼로그 외부를 터치하거나 뒤로가기로 다이얼로그를 닫을 경우
                showExitDialog = false
            },
            title = { Text("종료 확인") },
            text = { Text("뒤로가기를 진행하시겠습니까?") },
            confirmButton = {
                if (viewModel.lastGuideState == KNGuideState.KNGuideState_OnRouteGuide) {
                    Button(
                        onClick = {
                            viewModel._navView?.guideCancel()
                            showExitDialog = false
                        }
                    ) {
                        Text("안전운전 모드")
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel._navView?.guidance?.stop()
                        showExitDialog = false
                        viewModel.stopLocationCollection()
                    }
                ) {
                    Text("안내 종료하기")
                }
            }
        )
    }

}