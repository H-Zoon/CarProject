package com.devidea.chevy.bluetooth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.devidea.chevy.Logger
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec
import com.devidea.chevy.datas.obd.protocol.codec.ToureDevCodec
import com.devidea.chevy.eventbus.Event
import com.devidea.chevy.eventbus.EventBus
import com.devidea.chevy.eventbus.UIEventBus
import com.devidea.chevy.eventbus.UIEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

object BluetoothModelV2 {
    private var currentMTU: Int = 185

    private var scanJob: Job? = null

    private var btState = BTState.DISCONNECTED

    private var leBTService: LeBTService? = null
    private var isBound = false

    private val sendChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val sendScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val busScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LeBTService.LocalBinder
            leBTService = binder.getService()
            isBound = true
            leBTService?.mBleServiceCallback = mBleServiceCallback
            startSending()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            leBTService = null
            isBound = false
        }
    }

    private val mBleServiceCallback = object : LeBTService.BleServiceCallback {
        override fun onBTStateChange(state: BTState) {
            if (btState != state) {
                CoroutineScope(Dispatchers.Main).launch {
                    UIEventBus.post(UIEvents.reuestBluetooth(state))
                }
                leBTService?.updateNotification(state)
                btState = state
                if (btState == BTState.CONNECTED) {
                    leBTService?.discoverServices()
                } else if (btState == BTState.DISCONNECTED) {
                    scanJob?.cancel()
                }
            }
        }

        override fun onReceived(data: ByteArray, length: Int) {
            revMsgBufferHandler(data, length)
        }

        override fun onMtuChanged(mtu: Int) {
            currentMTU = mtu
            ToureDevCodec.sendInit(1)
            ToDeviceCodec.sendCurrentTime()
        }

        override fun onMessageSent() {
            sendContinuation?.invoke()
            sendContinuation = null
        }
    }

    fun initBTModel(context: Context) {
        val intent = Intent(context, LeBTService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun connectBT() {
        if (btState == BTState.CONNECTED) {
            return
        } else {
            leBTService?.startScan()
        }
    }

    fun disconnectBT() {
        leBTService?.disconnect()
        sendScope.cancel()
    }

    private val sendMutex = Mutex()

    fun sendMessage(data: ByteArray) {
        sendScope.launch {
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

    private fun startSending() {
        sendScope.launch {
            for (chunk in sendChannel) {
                Log.e("startSending", "chunk : $chunk")
                sendMutex.withLock {
                    try {
                        sendChunk(chunk)
                    } catch (e: Exception) {
                        Logger.e { "전송 중 에러 발생: ${e.message}" }
                        // 필요 시 에러 처리 로직 추가
                    }
                }
            }
        }
    }

    private suspend fun sendChunk(chunk: ByteArray) {
        leBTService?.sendMessage(chunk)
        waitForMessageSent()
    }

    private var sendContinuation: (() -> Unit)? = null
    private suspend fun waitForMessageSent() = suspendCancellableCoroutine<Unit> { cont ->
        sendContinuation = {
            cont.resume(Unit)
        }
        cont.invokeOnCancellation {
            sendContinuation = null
        }
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

    private fun msgBranchingHandler(bArr: ByteArray) {
        val header = bArr[0].toInt()
        Log.e("TAG", "header : $header, massage : ${bArr.joinToString()}")
        when (header) {
            1, 3 -> a(bArr)
            55 -> ToureDevCodec.sendHeartbeat()
        }
    }

    fun a(bArr: ByteArray){
        busScope.launch {
            EventBus.post(Event.carStateEvent(bArr))
        }
    }
}