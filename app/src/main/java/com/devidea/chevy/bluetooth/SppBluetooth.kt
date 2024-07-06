package com.devidea.chevy.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import com.devidea.chevy.App

class SppBluetooth : SppBluetoothChatService.OnChatServiceListener {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    var mChatService: SppBluetoothChatService? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mIsToureFound = false
    private var mConnectStatus = 0
    private var mIsConnecttingPeriod = false
    private var mHandler: Handler? = null
    private val m_appInitOkHandler = Handler()
    private var mSppBluetoothCallback: SppBluetoothCallback? = null

    // 블루투스 상태 변경 콜백 인터페이스
    interface SppBluetoothCallback {
        fun onBTStateChange(state: Int)
        fun onReceived(data: ByteArray, length: Int)
    }

    // 블루투스 브로드캐스트 리시버
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val bluetoothDevice: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            ?: return
                    Log.d(
                        tag,
                        "device found: name = ${bluetoothDevice.name} addr = ${bluetoothDevice.address}"
                    )
                    val name = bluetoothDevice.name
                    if (name == BluetoothModel.BT_NAME1 || name == BluetoothModel.BT_NAME2) {
                        mIsToureFound = true
                        devInSearchResult(bluetoothDevice)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(tag, "device discovery finished")
                    if (!mIsToureFound && mConnectStatus == 3) {
                        onBTStateChange(4)
                    }
                    if (!mIsToureFound && mIsConnecttingPeriod) {
                        searchBT()
                    }
                }
            }
        }
    }

    // 연결 타임아웃 처리 Runnable
    private val mConnectTimeoutRunnable = Runnable {
        mIsConnecttingPeriod = false
        if (isConnected()) {
            return@Runnable
        }
        BluetoothModel.clearBTMode()
    }

    // 블루투스 초기화
    @SuppressLint("NewApi")
    fun initSppBluetooth(): Boolean {
        registerReceiver()
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        val bluetoothAdapter = mBluetoothAdapter ?: return false
        if (!bluetoothAdapter.isEnabled) {
            mBluetoothAdapter?.enable()
        }
        if (mChatService == null) {
            setupChat()
            return true
        }
        return true
    }

    // 블루투스 브로드캐스트 리시버 등록
    private fun registerReceiver() {
        App.ApplicationContext().registerReceiver(mReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        App.ApplicationContext().registerReceiver(
            mReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        )
    }

    // 블루투스 콜백 설정
    fun setSppBluetoothCallback(sppBluetoothCallback: SppBluetoothCallback?) {
        mSppBluetoothCallback = sppBluetoothCallback
    }

    // 블루투스 상태 변경 처리
    fun onBTStateChange(state: Int) {
        Log.d(tag, "onBTStateChange: $mConnectStatus -> $state")
        if (mConnectStatus == state) {
            return
        }
        mSppBluetoothCallback?.onBTStateChange(state)
        mConnectStatus = state
        if (mConnectStatus == 1) {
            mIsConnecttingPeriod = false
            mHandler?.removeCallbacks(mConnectTimeoutRunnable)
        }
    }

    // 블루투스 초기화 해제
    fun deInitThreeBt() {
        mBluetoothAdapter?.cancelDiscovery()
        mChatService?.stop()
    }

    // 채팅 서비스 설정
    private fun setupChat() {
        Log.d(tag, "setupChat()")
        mChatService = SppBluetoothChatService().apply {
            setOnChatServiceListener(this@SppBluetooth)
        }
    }

    // 검색된 장치를 처리
    fun devInSearchResult(bluetoothDevice: BluetoothDevice) {
        mBluetoothDevice = bluetoothDevice
        connectDevice(bluetoothDevice, true, true)
    }

    // 장치 연결
    private fun connectDevice(
        bluetoothDevice: BluetoothDevice?,
        secure: Boolean,
        autoReconnect: Boolean
    ) {
        if (bluetoothDevice == null) {
            return
        }
        mIsToureFound = true
        mBluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            Log.d(tag, "Trying a connection with BTTTTTTTTTTTTTT addr: ${bluetoothDevice.address}")
            mChatService?.connect(bluetoothDevice, secure, autoReconnect)
        }
    }

    // 메시지 전송
    fun sendMessage(data: ByteArray) {
        val chatService = mChatService ?: return
       /* if (chatService.state != 1) {
            Log.d(tag, "send message but bt not connected")
        } else if (data.isNotEmpty()) {*/
            chatService.write(data)
        //}
    }

    // MCU로 데이터를 전송
    fun setToMcu(data: ByteArray, length: Int) {
        val packet = ByteArray(length + 5)
        var checksum = 0
        packet[0] = -1
        packet[1] = 85
        packet[2] = (length + 1).toByte()
        packet[3] = 3
        System.arraycopy(data, 0, packet, 4, length)
        for (i in 2 until length + 4) {
            checksum += packet[i].toInt() and 0xFF
        }
        packet[length + 4] = (checksum and 0xFF).toByte()
        sendMessage(packet)
    }

    // 블루투스 장치 검색
    fun searchBT() {
        val bluetoothAdapter = mBluetoothAdapter ?: return
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        mIsToureFound = false
        bluetoothAdapter.startDiscovery()
        onBTStateChange(3)
    }

    // 블루투스 연결 여부 확인
    fun isConnected(): Boolean {
        return mConnectStatus == 1
    }

    // 블루투스 연결 요청
    fun requestConnect() {
        if (mIsConnecttingPeriod) {
            return
        }
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        val bluetoothAdapter = mBluetoothAdapter ?: return
        if (!bluetoothAdapter.isEnabled) {
            mBluetoothAdapter?.enable()
        }
        if (mChatService == null) {
            setupChat()
        }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        startConnectTimeoutHandler()
        val bondedDevices = bluetoothAdapter.bondedDevices
        if (bondedDevices.isNullOrEmpty()) {
            Log.d(tag, "no paired devices")
            searchBT()
            return
        }
        for (device in bondedDevices) {
            Log.d(tag, "pairedDevice : name = ${device.name} addr = ${device.address}")
            if (device.name == BluetoothModel.BT_NAME1 || device.name == BluetoothModel.BT_NAME2) {
                mBluetoothDevice = device
                connectDevice(device, false, true)
                return
            }
        }
        Log.d(tag, "no mydevices paired")
        searchBT()
    }

    // 연결 타임아웃 핸들러 시작
    private fun startConnectTimeoutHandler() {
        mIsConnecttingPeriod = true
        if (mHandler == null) {
            mHandler = Handler()
        }
        mHandler?.postDelayed(mConnectTimeoutRunnable, 30000L)
    }

    // 블루투스 연결 요청 해제
    fun requestDisconnect() {
        disconnectBT()
    }

    // 블루투스 해제
    private fun disconnectBT() {
        try {
            mBluetoothAdapter?.cancelDiscovery()
            mChatService?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 블루투스 초기화
    fun clearBT() {
        mBluetoothAdapter?.cancelDiscovery()
        mChatService?.stop()
    }

    // 상태가 변경되었을 때 호출
    override fun onStateChange(state: Int, subState: Int) {
        onBTStateChange(state)
        if (state == 0 && mIsConnecttingPeriod) {
            try {
                Thread.sleep(1500L)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (subState == 1 && mIsConnecttingPeriod) {
                connectDevice(mBluetoothDevice, false, true)
            } else if (subState == 2 && mIsConnecttingPeriod) {
                connectDevice(mBluetoothDevice, true, true)
            }
        }
    }

    // 데이터를 수신했을 때 호출
    override fun onReceived(data: ByteArray, length: Int) {
        mSppBluetoothCallback?.onReceived(data, length)
    }

    // 블루투스 연결을 유지하려고 시도
    fun tryToKeepConnected() {
        if (isConnected() || mIsConnecttingPeriod) {
            return
        }
        requestConnect()
    }

    companion object {
        private const val BT_CONNECT_TIMEOUT = 30000
        private const val tag = "SPP"
    }
}