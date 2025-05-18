package com.devidea.aicar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.devidea.aicar.App.Companion.ACTION_AUTO_CONNECT
import com.devidea.aicar.storage.datastore.DataStoreRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutoConnectReceiver : BroadcastReceiver() {
    @Inject lateinit var sppClient: SppClient
    @Inject lateinit var dataStoreRepository: DataStoreRepository

    override fun onReceive(context: Context, intent: Intent) {
        // 1) goAsync() 로 PendingResult 얻기
        val pendingResult = goAsync()

        // 2) 별도 코루틴에서 실제 작업 수행
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Log.d("AutoConnectRCV", "action=${intent.action}")
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        // DataStore Flow.first() 는 suspend function 이므로 코루틴 내에서 안전
                        if (dataStoreRepository.isAutoConnectOnCharge.first()) {
                            sppClient.requestAutoConnect()
                        }
                    }
                    ACTION_AUTO_CONNECT -> {
                        sppClient.requestAutoConnect()
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoConnectRCV", "자동 연결 실패", e)
            } finally {
                // 3) 끝나면 반드시 finish() 호출
                pendingResult.finish()
            }
        }
    }
}