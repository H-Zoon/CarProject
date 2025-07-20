package com.devidea.aicar.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.RequiresPermission
import com.devidea.aicar.storage.datastore.DataStoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

interface SppClient {
    suspend fun requestStartScan()

    suspend fun requestStopScan()

    suspend fun requestConnect(device: ScannedDevice)

    suspend fun requestDisconnect()

    suspend fun requestAutoConnect()

    suspend fun getCurrentConnectedDevice(): ScannedDevice?

    suspend fun query(
        cmd: String,
        header: String? = null,
        timeoutMs: Long = 1_000,
    ): String

    val deviceList: StateFlow<List<ScannedDevice>>
    val connectionEvents: StateFlow<ConnectionEvent>
}

@Singleton
class SppClientImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val bluetoothAdapter: BluetoothAdapter,
        private val repository: DataStoreRepository,
    ) : SppClient {
        // — 바인딩 완료를 기다릴 Deferred
        private val serviceReady = CompletableDeferred<SppService>()

        // Flows exposed to callers
        private val _deviceList = MutableStateFlow<List<ScannedDevice>>(emptyList())
        override val deviceList: StateFlow<List<ScannedDevice>> = _deviceList.asStateFlow()

        private val _connectionEvents = MutableStateFlow<ConnectionEvent>(ConnectionEvent.Idle)
        override val connectionEvents: StateFlow<ConnectionEvent> = _connectionEvents.asStateFlow()

        private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // ——— ServiceConnection callbacks ———
        private val connection =
            object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName,
                    binder: IBinder,
                ) {
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
                    // service = null; bound = false
                    if (!serviceReady.isCompleted) {
                        serviceReady.completeExceptionally(
                            IllegalStateException("Service disconnected unexpectedly"),
                        )
                    }
                }
            }

        fun serviceCreate() {
            val intent = Intent(context, SppService::class.java)
            // ContextCompat.startForegroundService(context, intent)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        // ——— Control wrappers ———
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override suspend fun requestStartScan() {
            ensureBoundService().requestScan()
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override suspend fun requestStopScan() {
            ensureBoundService().requestStop()
        }

        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override suspend fun requestConnect(device: ScannedDevice) {
            ensureBoundService().requestConnect(device)
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        override suspend fun requestDisconnect() {
            ensureBoundService().requestDisconnect()
        }
        // override suspend fun requestUpdateNotification(message: String) { ensureBoundService().updateNotification(message) }

        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override suspend fun requestAutoConnect() {
            repository.getDevice.firstOrNull()?.let {
                val btDevice = bluetoothAdapter.getRemoteDevice(it.address)
                ensureBoundService().requestConnect(ScannedDevice(it.name, it.address, btDevice))
            } ?: run {
                // requestUpdateNotification("저장된 블루투스 기기가 없습니다.")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override suspend fun getCurrentConnectedDevice(): ScannedDevice? = ensureBoundService().getCurrentConnectedDevice()

        override suspend fun query(
            cmd: String,
            header: String?,
            timeoutMs: Long,
        ): String = ensureBoundService().query(header, cmd, timeoutMs)

        suspend fun ensureBoundService(): SppService {
            if (!serviceReady.isCompleted) {
                val intent = Intent(context, SppService::class.java)
                // ContextCompat.startForegroundService(context, intent)
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
