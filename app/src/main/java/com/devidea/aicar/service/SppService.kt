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
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devidea.aicar.ui.main.MainActivity
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

// ——————————————————————————————————————————————————————————————————————————
// Helper data classes & constants (placeholders — implement as needed)
// ——————————————————————————————————————————————————————————————————————————

data class ScannedDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice? = null
)

// ——————————————————————————————————————————————————————————————————————————
// Service
// ——————————————————————————————————————————————————————————————————————————

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

        // OBD 포그라운드 채널
        private const val NOTIFICATION_TITLE = "AICAR"
        private const val NOTIFICATION_BODY = "블루투스 연결이 시작되었습니다."
        private const val FOREGROUND_CHANNEL_ID = "AICAR"
        private const val FOREGROUND_CHANNEL_NAME = "AICAR_bluetooth"

        // 주행 기록 알림 채널
        private const val RECORD_CHANNEL_ID = "record_channel"
        private const val RECORD_CHANNEL_NAME = "주행 기록 알림"
        private const val RECORD_CHANNEL_DESC = "주행 기록 시작/종료 알림"
    }

    // --- Bluetooth adapter & streams ---
    private val bluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private var socket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    // --- Scope & sync ---
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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
    private val pending = ConcurrentHashMap<String, MutableList<CompletableDeferred<String>>>()

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
        Log.d(TAG, "[Service] onCreate - Starting foreground")
        startForegroundService()
    }

    override fun onDestroy() {
        Log.d(TAG, "[Service] onDestroy - Cancelling scope")
        serviceScope.cancel()
        super.onDestroy()
    }

    // --- Foreground notification ---
    private fun startForegroundService() {
        Log.d(TAG, "[Service] startForegroundService called")

        // 1) Oreo(API 26) 이상에서 채널 생성
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            FOREGROUND_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "백그라운드 OBD 블루투스 연결 서비스 알림 채널"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        // 2) 클릭 시 돌아갈 Activity 인텐트 (실제 Activity 클래스로 변경)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3) Notification 빌드 (smallIcon, title, text, ongoing 필수)
        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_BODY)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        // 4) 포그라운드 서비스 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ : 타입까지 붙인 오버로드 사용
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            // 이전 버전은 기존 방식
            startForeground(1, notification)
        }
    }

    fun sendNotification(notifId: Int, title: String, body: String) {
        val notif = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notifId, notif)
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
            requestStop() // 혹시 남아 있는 이전 연결을 종료

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
            _connectionEvents.emit(ConnectionEvent.Disconnected)
            reader?.close(); writer?.close(); socket?.close()
            pending.clear()
        }
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
        writerMutex.withLock {
            Log.d(TAG, "[Writer] sendRawSync writing $cmd")
            writer?.apply { write("$cmd\r"); flush() } ?: Log.e(TAG, "Writer unavailable")
        }
    }

    // --- Raw byte reader ---
    private fun launchRawReader() = serviceScope.launch(Dispatchers.IO) {
        Log.d(TAG, "[Reader] launchRawReader started")
        val input = socket!!.inputStream
        val sbLine = StringBuilder()
        val buf = ByteArray(1)
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
        Log.d(TAG, "[Reader] launchRawReader ended")
    }

    // 수정된 Assembler 코드: 경계 처리 우선 및 불필요 라인 필터 순서 조정
    private fun launchAssembler() = serviceScope.launch {
        Log.d(TAG, "[Assembler] started")
        val sb = StringBuilder()
        rawLines.collect { raw ->
            Log.d(TAG, "[Assembler] rawLine='$raw'")
            val trimmed = raw.trim()

            // 1) 경계(프레임 완성) 먼저 처리
            if (trimmed == ">") {
                val frame = sb.toString()
                    .replace("\\s".toRegex(), "")
                    .uppercase(Locale.US)
                Log.d(TAG, "[Assembler] Emitting frame='$frame'")
                frames.emit(frame)
                sb.clear()
                return@collect
            }

            // 2) 그 외 불필요한 라인 필터
            if (trimmed.isBlank() || trimmed.startsWith("SEARCHING") || trimmed == "OK") {
                return@collect
            }

            // 3) 데이터 누적
            sb.append(trimmed)
        }
    }

    // --- Router ---
    private fun launchRouter() = serviceScope.launch {
        Log.d(TAG, "[Router] started")
        frames.collect { f ->
            val key = extractCmdKey(f)
            pending.remove(key)?.forEach { it.complete(f) }
        }
    }

    // --- Query API ---
    suspend fun query(header: String? = null, cmd: String, timeoutMs: Long = 1000): String =
        coroutineScope {
            val key = cmd.lowercase()
            Log.d(TAG, "[Query] start ▶ key=$key, header=$header, cmd=$cmd, timeout=${timeoutMs}ms")
            val promise = CompletableDeferred<String>()
            pending.compute(key) { _, list ->
                (list ?: mutableListOf()).apply { add(promise) }
            }
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

    // --- Key extraction ---
    private fun extractCmdKey(raw: String): String {
        val f = raw.replace("\\s".toRegex(), "")
        return when {
            f.startsWith("41") -> "01" + f.substring(2, 4)
            f.startsWith("62") -> "22" + f.substring(2, 6)
            f.startsWith("43") -> "03"
            else -> f.take(6)
        }.lowercase()
    }
}
