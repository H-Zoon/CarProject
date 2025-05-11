package com.devidea.aicar.k10s

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleServiceManager @Inject constructor(
    private val context: Context
) {

    private var isBound = false
    private var bleService: BleService? = null

    private val _serviceState = MutableStateFlow<BleService?>(null)
    val serviceState: StateFlow<BleService?> = _serviceState.asStateFlow()

    // 서비스 커넥션 구현
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            // LocalBinder로 캐스팅 후 서비스 인스턴스 획득
            (binder as? BleService.LocalBinder)?.let {
                bleService = it.getService()
                isBound = true
                _serviceState.value = bleService
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleService = null
            isBound = false
            _serviceState.value = null
        }
    }

    // 서비스 바인딩
    fun bindService() {
        val intent = Intent(context, BleService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // 서비스 연결 해제
    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            _serviceState.value = null
        }
    }

    // 외부에서 서비스 객체에 접근하기 위한 메서드
    fun getService(): BleService? {
        return bleService
    }
}