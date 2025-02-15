package com.devidea.chevy.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devidea.chevy.App.Companion.CHANNEL_ID
import com.devidea.chevy.Logger
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.R
import com.devidea.chevy.bluetooth.BTState
import com.devidea.chevy.bluetooth.BleCallback
import com.devidea.chevy.bluetooth.BleManager
import com.devidea.chevy.bluetooth.BleManagerImpl
import com.devidea.chevy.datas.obd.protocol.codec.Msgs
import com.devidea.chevy.eventbus.Event
import com.devidea.chevy.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class BleService : Service() {

    private val sendChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val sendScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val busScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentMTU = 185

    private val _btState = MutableStateFlow(BTState.DISCONNECTED)
    val btState: StateFlow<BTState> = _btState.asStateFlow()

    private val mBinder = LocalBinder()
    private lateinit var bleManager: BleManager

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.i { "onUnbind close." }
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        bleManager = BleManagerImpl(applicationContext)

        bleManager.setBleCallback(object : BleCallback {
            override fun onStateChanged(state: BTState) {
                Logger.i { "BLE State Changed: $state" }
                updateState(state)
                updateNotification(state)
            }

            override fun onCharacteristicRead(data: ByteArray, size: Int) {
                Logger.d { "onCharacteristicRead: ${data.joinToString()}" }
                revMsgBufferHandler(data, size)
            }

            override fun onMessageSent() {
                Logger.d { "onMessageSent()" }
            }

            override fun onMtuChanged(mtu: Int) {
                Logger.d { "onMtuChanged: $mtu" }
                currentMTU = mtu
                //ToureDevCodec.sendInit(1)
                //ToDeviceCodec.sendCurrentTime()
            }

            override fun onError(message: String) {
                Logger.d { "onError: $message" }
            }
        })
        startForegroundService()
        startSending()
    }

    override fun onDestroy() {
        Logger.d { "onDestroy" }
        sendScope.cancel() // 코루틴 취소
        sendChannel.close()
        super.onDestroy()
    }


    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LeBluetoothService")
            .setContentText("Initializing Bluetooth LE Service")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun updateNotification(state: BTState) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stateText = state.description

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LeBluetoothService")
            .setContentText("Bluetooth State: $stateText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private val mMsgBuf = ByteArray(1024)

    private fun revMsgBufferHandler(bArr: ByteArray?, length: Int) {
        var mMsgEndPos = 0
        var mMsgLen = 0

        if (length == 0 || bArr == null) return

        for (i in 0 until length) {
            if (mMsgEndPos >= 1024) mMsgEndPos = 0
            mMsgBuf[mMsgEndPos] = bArr[i]
            mMsgEndPos++

            when (mMsgEndPos) {
                1 -> {
                    if ((mMsgBuf[0].toInt() and 255) != 255) {
                        mMsgEndPos = 0
                    }
                }

                2 -> {
                    if ((mMsgBuf[1].toInt() and 255) != 85) {
                        mMsgEndPos = 0
                    }
                }

                3 -> {
                    mMsgLen = mMsgBuf[2].toInt() and 255
                    if (mMsgLen > 128) {
                        Logger.d { "analyseCarInfo if (m_nDataPacketLen > 128)" }
                    }
                }

                else -> {
                    if (mMsgEndPos == mMsgLen + 4) {
                        val checksum =
                            (2 until mMsgEndPos - 1).sumBy { mMsgBuf[it].toInt() and 255 }
                        val calculatedChecksum = checksum and 255
                        if (calculatedChecksum == (mMsgBuf[mMsgEndPos - 1].toInt() and 255)) {
                            val msgData = ByteArray(mMsgLen)
                            System.arraycopy(mMsgBuf, 3, msgData, 0, mMsgLen)
                            msgBranchingHandler(msgData)
                        }
                        mMsgLen = 0
                        mMsgEndPos = 0
                    }
                }
            }
        }
    }

    private fun msgBranchingHandler(msgData: ByteArray) {
        if (msgData.isEmpty()) return

        val header = msgData[0].toInt()
        Log.e("TAG", "Header: $header, Message: ${msgData.joinToString()}")

        when (header) {
            1, 3 -> handleCarStateEvent(msgData)
            55 -> sendHandleMsg(Msgs.sendHeartbeat())
            else -> Logger.d { "Unknown header: $header" }
        }
    }

    private fun handleCarStateEvent(msgData: ByteArray) {
        busScope.launch {
            EventBus.post(Event.carStateEvent(msgData))
        }
    }

    private val sendMutex = Mutex()

    fun sendMessage(data: ByteArray) {
        sendScope.launch {
            sendMutex.withLock {
                if (currentMTU == 0) {
                    Log.e("TAG", "error")
                    return@launch
                } else {
                    var length = data.size
                    var offset = 0
                    while (length > 0) {
                        val chunkSize = if (length > currentMTU) currentMTU else length
                        val chunk = data.copyOfRange(offset, offset + chunkSize)
                        offset += chunkSize
                        length -= chunkSize
                        sendChannel.send(chunk)
                    }
                }
            }
        }
    }

    private fun startSending() {
        sendScope.launch {
            for (chunk in sendChannel) {
                try {
                    bleManager.sendMessage(chunk)
                } catch (e: Exception) {
                    Logger.e { "전송 중 에러 발생: ${e.message}" }
                }
            }
        }
    }

    private fun updateState(newState: BTState) {
        _btState.value = newState
    }

    fun requestScan() {
        bleManager.startScan()
    }

    fun requestDisconnect() {
        bleManager.disconnect()
    }

    fun sendHandleMsg(pair: Pair<ByteArray, Int>) {
        val bArr = pair.first
        val i = pair.second

        val bArr2 = ByteArray(i + 5)
        var i2 = 0
        bArr2[0] = -1
        bArr2[1] = 85
        var i3 = 2
        bArr2[2] = (i + 1).toByte()
        bArr2[3] = 3
        System.arraycopy(bArr, 0, bArr2, 4, i)
        while (true) {
            val i4 = i + 4
            if (i3 < i4) {
                i2 += bArr2[i3].toInt() and 255
                i3++
            } else {
                bArr2[i4] = (i2 and 255).toByte()
                sendMessage(bArr2)
                return
            }
        }
    }

    private val sendNaviMsgMutex = Mutex()

    suspend fun sendNaviMsg(pair: Pair<ByteArray, Int>) {
        sendNaviMsgMutex.withLock {
            suspendCoroutine<Unit> { cont ->
                val bArr = pair.first
                val i = pair.second

                val bArr2 = ByteArray(i + 5)
                var i2 = 0
                bArr2[0] = -1
                bArr2[1] = 85
                var i3 = 2
                bArr2[2] = (i + 1).toByte()
                bArr2[3] = 1
                System.arraycopy(bArr, 0, bArr2, 4, i)
                while (true) {
                    val i4 = i + 4
                    if (i3 < i4) {
                        i2 += bArr2[i3].toInt() and 255
                        i3++
                    } else {
                        bArr2[i4] = (i2 and 255).toByte()
                        sendMessage(bArr2)
                        break
                    }
                }
                cont.resume(Unit)
            }
        }
    }
}