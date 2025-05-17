package com.devidea.aicar.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn

interface SppClient {
    suspend fun requestStartScan()
    suspend fun requestStopScan()
    suspend fun requestConnect(device: ScannedDevice)
    suspend fun requestDisconnect()
    suspend fun query(cmd: String, header: String? = null, timeoutMs: Long = 1_000): String
    val deviceList: StateFlow<List<ScannedDevice>>
    val connectionEvents: SharedFlow<ConnectionEvent>
}

@Singleton
class SppClientImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SppClient {
    // — 바인딩 완료를 기다릴 Deferred
    private val serviceReady = CompletableDeferred<SppService>()

    // Flows exposed to callers
    private val _deviceList = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val deviceList: StateFlow<List<ScannedDevice>> = _deviceList.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(replay = 1)
    override val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ——— ServiceConnection callbacks ———
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as SppService.LocalBinder
            val svc = localBinder.getService()

            if (!serviceReady.isCompleted) {
                serviceReady.complete(svc)
            }

            // Forward flows
            svc.scannedList
                .onEach { _deviceList.value = it }
                .launchIn(clientScope)

            svc.connectionEvents
                .onEach { _connectionEvents.tryEmit(it) }
                .launchIn(clientScope)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            //service = null; bound = false
            if (!serviceReady.isCompleted) {
                serviceReady.completeExceptionally(
                    IllegalStateException("Service disconnected unexpectedly")
                )
            }
        }
    }

    fun serviceCreate() {
        val intent = Intent(context, SppService::class.java)
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // ——— Control wrappers ———
    override suspend fun requestStartScan() { ensureBoundService().requestScan() }
    override suspend fun requestStopScan() { ensureBoundService().requestStop()}

    override suspend fun requestConnect(device: ScannedDevice) { ensureBoundService().requestConnect(device) }
    override suspend fun requestDisconnect() { ensureBoundService().requestDisconnect() }

    override suspend fun query(cmd: String, header: String?, timeoutMs: Long): String {
        return ensureBoundService().query(header, cmd, timeoutMs)
    }

    suspend fun ensureBoundService(): SppService {
        if (!serviceReady.isCompleted) {
            val intent = Intent(context, SppService::class.java)
            ContextCompat.startForegroundService(context, intent)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        // 바인딩이 완료될 때까지 대기
        return serviceReady.await()
    }

    /** 앱 종료 시 바인딩 해제 */
    fun unbindService() {
        if (serviceReady.isCompleted) {
            try {
                context.unbindService(connection)
            } catch (_: IllegalArgumentException) {
                // 이미 언바인드 된 경우 무시
            }
        }
    }
}



