package com.devidea.chevy.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun requestScan()
    fun requestConnect(device: ScannedDevice)
    fun requestDisconnect()
    suspend fun query(cmd: String, header: String? = null, timeoutMs: Long = 1_000): String
    val deviceList: StateFlow<List<ScannedDevice>>
    val connectionEvents: SharedFlow<ConnectionEvent>
    val liveFrames: SharedFlow<String>
}

@Singleton
class SppClientImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SppClient {

    // Actual bound service instance
    private var service: SppService? = null
    private var bound: Boolean = false

    // Flows exposed to callers
    private val _deviceList = MutableStateFlow<List<ScannedDevice>>(emptyList())
    override val deviceList: StateFlow<List<ScannedDevice>> = _deviceList.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(replay = 1)
    override val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    private val _liveFrames = MutableSharedFlow<String>(extraBufferCapacity = 256)
    override val liveFrames: SharedFlow<String> = _liveFrames.asSharedFlow()

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ——— ServiceConnection callbacks ———
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as SppService.LocalBinder
            service = localBinder.getService()
            bound = true

            // Forward flows
            service!!.scannedList
                .onEach { _deviceList.value = it }
                .launchIn(clientScope)

            service!!.connectionEvents
                .onEach { _connectionEvents.tryEmit(it) }
                .launchIn(clientScope)

            service!!.liveFrames
                .onEach { _liveFrames.emit(it) }
                .launchIn(clientScope)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null; bound = false
        }
    }

    init {
        val intent = Intent(context, SppService::class.java)
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // ——— Control wrappers ———
    override fun requestScan() { ensureBound(); service!!.requestScan() }
    override fun requestConnect(device: ScannedDevice) { ensureBound(); service!!.requestConnect(device) }
    override fun requestDisconnect() { ensureBound(); service!!.requestDisconnect() }

    override suspend fun query(cmd: String, header: String?, timeoutMs: Long): String {
        ensureBound(); return service!!.query(header, cmd, timeoutMs)
    }

    private fun ensureBound() {
        check(bound && service != null) { "SppService is not bound yet" }
    }
}



