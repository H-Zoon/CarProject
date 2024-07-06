package com.devidea.chevy.bluetooth

import android.bluetooth.BluetoothAdapter
import android.util.Log
import com.devidea.chevy.carsystem.CarModel

object BluetoothModel {
    private const val BT_MSG_SEND_DELAY_TIME = 16
    private const val BT_MSG_SEND_UPGRADE_FILE_DELAY_TIME = 25
    const val BT_NAME1 = "Sinjet"
    const val BT_NAME2 = "ChanganMazda"
    private const val HIGH_SPEED_BT_MTU = 182

    private var mMsgSendDelayTime = BT_MSG_SEND_DELAY_TIME
    private var mMsgSendUpgradeDelayTime = BT_MSG_SEND_UPGRADE_FILE_DELAY_TIME
    private const val tag = "BluetoothModel"

    private fun setMsgSendDelayTime(delayTime: Int) {
        mMsgSendDelayTime = delayTime
        mMsgSendUpgradeDelayTime = delayTime
    }

    fun getMsgSendUpgradeDelayTime(): Int {
        return mMsgSendUpgradeDelayTime
    }

    fun getMsgSendDelayTime(): Int {
        return mMsgSendDelayTime
    }

    private var btWorkingHandler: BTWorkingHandler = BTWorkingHandler()
    private var leBluetooth: LeBluetooth = LeBluetooth()

    private var btState = BTState.DISCONNECTED
    private var mIsInit = false
    private var mBleUsingMtu = HIGH_SPEED_BT_MTU
    private var mBleChangedMtu = HIGH_SPEED_BT_MTU
    private var mBluetoothAdapter: BluetoothAdapter? = null

    // MTU가 변경되었을 때 호출됩니다.
    fun onMtuChanged(mtu: Int) {
        if (mtu == 100) {
            setMsgSendDelayTime(10)
        } else {
            mBleChangedMtu = mtu - 3
        }
    }

    fun usingBleMtu(): Int {
        return this.mBleUsingMtu
    }

    // 전송 속도에 따른 설정을 처리합니다.
    fun onTransportSpeed(speed: Int) {
        Log.i("kevin", "on transport speed: $speed")
        if (speed >= 1000 && speed >= 5000) {
            useChangedMtu()
            mMsgSendDelayTime = 25
            mMsgSendUpgradeDelayTime = 25
        }
    }

    // 변경된 MTU를 적용합니다.
    private fun useChangedMtu() {
        mBleUsingMtu = mBleChangedMtu
    }

    // 블루투스 모델을 초기화합니다.
    fun initBTModel() {
        btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.InitBT)
    }

    // 블루투스를 연결합니다.
    fun connectBT() {
        btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.ConnectBT)
    }

    // 블루투스를 해제합니다.
    fun disconnectBT() {
        btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.DisconnectBT)
    }

    // 스레드 내에서 블루투스를 초기화합니다.
    fun initBTInThread() {
        Log.d(tag, "init BTModel")
        if (leBluetooth.initLeBluetooth()) {
            leBluetooth.setLeBluetoothCallback(mLeBluetoothCallback)
            connectBTInThread()
            mIsInit = true
            return
        }
        Log.d(tag, "your phone does not support")
    }


    // 스레드 내에서 블루투스를 연결합니다.
    fun connectBTInThread() {
        if (btState == BTState.CONNECTED) return
        leBluetooth.requestConnect()
    }

    // 스레드 내에서 블루투스를 해제합니다.
    fun diconnectBTInThread() {
        leBluetooth.requestDisconnect()
    }

    // 블루투스로 메시지를 전송합니다.
    @Synchronized
    fun sendMessage(data: ByteArray) {
        if (true) {
            var length = data.size
            var offset = 0
            while (length > 0) {
                val chunkSize = if (length > mBleUsingMtu) mBleUsingMtu else length
                val chunk = data.copyOfRange(offset, offset + chunkSize)
                offset += chunkSize
                length -= chunkSize

                // Correctly initialize byteArray with the contents of chunk
                val byteArray = chunk.copyOf()

                btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.KeepConnectedBT)
                btWorkingHandler.msgBuffCount++
                btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.SendBT(byteArray))
            }
        }
    }

    // 스레드 내에서 블루투스로 메시지를 전송합니다.
    fun sendMessageInThread(data: ByteArray) {
        leBluetooth.sendMessage(data)
    }

    // 블루투스 상태가 변경되었을 때 호출됩니다.
    private fun onBTStateChange(state: BTState) {
        Log.d(tag, "onBTStateChange: $btState -> $state")
        btState = state
        if (state == BTState.DISCONNECTED) {
            onBTDisconnected()
        }
    }

    // 블루투스가 해제되었을 때 호출됩니다.
    private fun onBTDisconnected() {
        mBleUsingMtu = HIGH_SPEED_BT_MTU
        mMsgSendDelayTime = BT_MSG_SEND_DELAY_TIME
        mMsgSendUpgradeDelayTime = BT_MSG_SEND_UPGRADE_FILE_DELAY_TIME
    }

    // 블루투스로부터 데이터를 수신했을 때 호출됩니다.
    private fun onReceived(data: ByteArray, length: Int) {
        CarModel.onRecvMsgFromDevice(data, length)
    }

    // 블루투스 모드를 초기화합니다.
    fun clearBTMode() {
        leBluetooth.clearBT()
        mIsInit = false
    }

    // 블루투스가 연결되어 있는지 확인합니다.
    fun isConnected(): Boolean {
        return btState == BTState.CONNECTED
    }

    fun tryToKeepConnected() {
        Log.i(tag, "BluetoothModel tryToKeepConnected")
        btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.KeepConnectedBT)
    }

    fun tryToKeepConnectedInThread() {
        if (isBTPowerOn()) {
            Log.d(tag, "tryToKeepConnected BT")
            if (mIsInit) {

                leBluetooth.tryToKeepConnected()

            } else {
                initBTModel()
            }
        }
    }

    private fun isBTPowerOn(): Boolean {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        return mBluetoothAdapter?.isEnabled == true
    }

    private val mLeBluetoothCallback = object : LeBluetooth.LeBluetoothCallback {
        override fun onBTStateChange(state: BTState) {
            this@BluetoothModel.onBTStateChange(state)
        }

        override fun onReceived(data: ByteArray, length: Int) {
            Log.d("BT", "Le BT onRecv")
            this@BluetoothModel.onReceived(data, length)
        }
    }
}