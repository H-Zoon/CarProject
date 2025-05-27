package com.devidea.aicar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ScannedDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice? = null
)

sealed class ConnectionEvent {
    object Scanning : ConnectionEvent()
    object Connecting : ConnectionEvent()
    object Connected : ConnectionEvent()
    object Disconnected : ConnectionEvent()
    object Error : ConnectionEvent()
}

class SppService : Service() {
    companion object {
        private const val TAG = "SppService"
        private const val DISCOVERY_TIMEOUT_MS = 10_000L
        private val BT_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // --- Bluetooth adapter & streams ---
    private val bluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var socket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    // --- Scope & sync ---
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rawReaderJob: Job? = null
    private val writerMutex = Mutex()

    // --- Header cache ---
    private var currentHeader: String? = null

    // --- Receiver flag ---
    private var isReceiverRegistered = false

    // --- Flows ---
    private val _scannedList = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedList: StateFlow<List<ScannedDevice>> = _scannedList.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(replay = 1)
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    private val rawLines = MutableSharedFlow<String>(extraBufferCapacity = 256)
    private val frames = MutableSharedFlow<String>(extraBufferCapacity = 256)

    private val promptFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // 동시 요청에 대한 구조 변경
    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()

    // --- Discovery receiver ---
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    ?.let { bt ->
                        Log.d(TAG, "[Scan] Found device ${bt.name} @ ${bt.address}")
                        _scannedList.update { list ->
                            if (list.any { it.address == bt.address }) list else list + ScannedDevice(
                                bt.name.orEmpty(),
                                bt.address,
                                bt
                            )
                        }
                    }
            }
        }
    }

    // --- Binder ---
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SppService = this@SppService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "[Service] onBind")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "[Service] onCreate - Starting")
    }

    override fun onDestroy() {
        Log.d(TAG, "[Service] onDestroy - Cancelling scope")
        serviceScope.cancel()
        super.onDestroy()
    }

    // --- Scan / Stop ---
    fun requestScan() {
        Log.d(TAG, "[Scan] requestScan called")
        serviceScope.launch {
            _connectionEvents.emit(ConnectionEvent.Scanning)
            _scannedList.value = emptyList()
            registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            isReceiverRegistered = true
            bluetoothAdapter.startDiscovery()
        }
    }

    fun requestStop() {
        serviceScope.launch {
            _connectionEvents.emit(ConnectionEvent.Disconnected)
            _scannedList.value = emptyList()
            Log.d(TAG, "[Scan] requestStop called")
            if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
            if (isReceiverRegistered) {
                unregisterReceiver(discoveryReceiver)
                isReceiverRegistered = false
            }
        }
    }

    // --- Connect / Disconnect ---
    fun requestConnect(device: ScannedDevice) {
        Log.d(TAG, "[Connect] requestConnect to ${device.address}")
        serviceScope.launch {
            _connectionEvents.emit(ConnectionEvent.Connecting)

            try {
                withContext(Dispatchers.IO) {
                    // device.device가 null이라면 에러 처리
                    val btDevice = device.device ?: run {
                        Log.e(TAG, "[Connect] BluetoothDevice 인스턴스가 없습니다.")
                        _connectionEvents.emit(ConnectionEvent.Error)
                        return@withContext
                    }

                    // 실제 연결
                    socket = createSocket(btDevice).apply { connect() }
                    Log.d(TAG, "[Connect] Socket connected")
                    _connectionEvents.emit(ConnectionEvent.Connected)
                    Log.d(TAG, "[Connect] Connected event emitted")
                    setupStreams()
                    initializeElm327()
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Connect] Error: ${e.localizedMessage}", e)
                _connectionEvents.emit(ConnectionEvent.Error)
                // 연결에 실패했으면 깨끗이 정리
                requestDisconnect()
            }
        }
    }

    fun requestDisconnect() {
        Log.d(TAG, "[Disconnect] requestDisconnect called")
        serviceScope.launch {
            rawReaderJob?.cancel()
            rawReaderJob = null

            reader?.close()
            writer?.close()
            socket?.close()
            pending.clear()

            _connectionEvents.emit(ConnectionEvent.Disconnected)
        }
    }

    fun getCurrentConnectedDevice(): ScannedDevice? {
        val btDevice = socket?.remoteDevice ?: return null
        return ScannedDevice(
            name = btDevice.name.orEmpty(),
            address = btDevice.address,
            device = btDevice
        )
    }

    @Throws(IOException::class)
    private fun createSocket(device: BluetoothDevice): BluetoothSocket {
        bluetoothAdapter.cancelDiscovery()
        Log.d(TAG, "[Socket] Creating RFCOMM socket to ${device.address}")
        return device.createRfcommSocketToServiceRecord(BT_SPP_UUID)
    }

    // --- Streams setup ---
    @Throws(IOException::class)
    private fun setupStreams() {
        Log.d(TAG, "[Streams] setupStreams called")
        socket?.let { sock ->
            reader = BufferedReader(InputStreamReader(sock.inputStream))
            writer = BufferedWriter(OutputStreamWriter(sock.outputStream))
        }
        launchRawReader()
        launchAssembler()
        launchRouter()
    }

    // --- ELM327 init ---
    private suspend fun initializeElm327() {
        Log.d(TAG, "[Init] initializeElm327 starting")
        listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATCAF1", "ATSP0").forEach { cmd ->
            Log.d(TAG, "[Init] Sending $cmd")
            sendRawSync(cmd)
            promptFlow.first()
            Log.d(TAG, "[Init] Detected prompt after $cmd")
            delay(50)
        }
        Log.d(TAG, "[Init] initializeElm327 completed")
    }

    // --- sendRawSync ---
    internal suspend fun sendRawSync(cmd: String) = withContext(Dispatchers.IO) {
        val lastEvent = connectionEvents.replayCache.firstOrNull()
        if (lastEvent != ConnectionEvent.Connected) {
            Log.w(TAG, "[Writer] sendRawSync 호출 시 상태 불일치: lastEvent=$lastEvent, cmd=$cmd")
            throw IllegalStateException("Bluetooth not connected")
        }

        writerMutex.withLock {
            Log.d(TAG, "[Writer] sendRawSync writing $cmd")
            writer?.apply { write("$cmd\r"); flush() } ?: Log.e(TAG, "Writer unavailable")
        }
    }

    // --- Raw byte reader ---
    private fun launchRawReader(): Job {
        rawReaderJob = serviceScope.launch(Dispatchers.IO) {
            Log.d(TAG, "[Reader] launchRawReader started")
            val input = socket?.inputStream ?: return@launch
            val sbLine = StringBuilder()
            val buf = ByteArray(1)

            try {
                while (isActive) {
                    val n = input.read(buf)
                    if (n <= 0) break

                    val ch = buf[0].toInt().toChar()
                    when (ch) {
                        '>' -> {
                            Log.d(TAG, "[Reader] Detected '>' prompt")
                            promptFlow.tryEmit(Unit)
                            rawLines.emit(">")
                            if (sbLine.isNotEmpty()) {
                                rawLines.emit(sbLine.toString())
                                sbLine.setLength(0)
                            }
                        }

                        '\r', '\n' -> {
                            if (sbLine.isNotEmpty()) {
                                rawLines.emit(sbLine.toString())
                                sbLine.setLength(0)
                            }
                        }

                        else -> sbLine.append(ch)
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "[Reader] Stream closed, stopping reader: ${e.localizedMessage}")
            } finally {
                Log.d(TAG, "[Reader] launchRawReader ended")
            }
        }
        return rawReaderJob!!
    }


    // 수정된 Assembler 코드: 경계 처리 우선 및 불필요 라인 필터 순서 조정
    private fun launchAssembler() = serviceScope.launch {
        val sb = StringBuilder()
        rawLines.collect { raw ->
            val trimmed = raw.trim()
            // 1) 프레임 경계
            if (trimmed == ">") {
                val frame = sb.toString()
                    .replace("\\s".toRegex(), "")
                    .uppercase(Locale.US)
                // 최소 길이(4 hex chars) 이상일 때에만 emit
                if (frame.length >= 4) {
                    frames.emit(frame)
                }
                sb.clear()
                return@collect
            }
            // 2) 불필요 라인 필터
            if (trimmed.isBlank() || trimmed.startsWith("SEARCHING") || trimmed == "OK") return@collect
            // 3) PCI(header)만 버리기: 1~3 hex chars + optional ':'
            if (trimmed.matches(Regex("^[0-9A-Fa-f]{1,3}:?$$"))) {
                return@collect
            }
            // 4) 데이터 라인 처리: ':' 있으면 뒤, 없으면 전체
            val payloadHex = trimmed
                .substringAfter(':', trimmed)
                .replace("\\s+".toRegex(), "")
                .uppercase(Locale.US)
            sb.append(payloadHex)
        }
    }

    // --- Router ---
    private fun launchRouter() = serviceScope.launch {
        Log.d(TAG, "[Router] started")
        frames.collect { f ->
            if (f.length < 4) return@collect
            val key = extractCmdKey(f)
            Log.d(TAG, "[Router] frame=$f key=$key")
            pending.remove(key)?.complete(f)
        }
    }

    // --- Query API ---
    suspend fun query(header: String? = null, cmd: String, timeoutMs: Long = 1000): String =
        coroutineScope {
            val normalized = cmd.replace("\\s".toRegex(), "").lowercase()
            val key = normalized.substring(0, 4)
            //val key = cmd.lowercase()
            Log.d(TAG, "[Query] start ▶ key=$key, header=$header, cmd=$cmd, timeout=${timeoutMs}ms")
            val promise = CompletableDeferred<String>()
            pending[key] = promise
            if (header != null && header != currentHeader) {
                Log.d(TAG, "[Query] send new header ATSH$header")
                sendRawSync("ATSH$header")
                currentHeader = header
                delay(40)
            }
            Log.d(TAG, "[Query] sending cmd=$cmd")
            sendRawSync(cmd)
            try {
                withTimeout(timeoutMs) {
                    val resp = promise.await()
                    Log.d(TAG, "[Query] received response ▶ $resp")
                    resp
                }
            } finally {
                pending.remove(key)
            }
        }


    private fun extractCmdKey(raw: String): String {
        val f = raw.replace("\\s".toRegex(), "").lowercase()
        return when {
            f.startsWith("41") -> "01" + f.substring(2, 4)
            f.startsWith("62") -> "22" + f.substring(2, 4)
            f.startsWith("43") -> "03"
            else -> f.take(6)
        }
    }
}
