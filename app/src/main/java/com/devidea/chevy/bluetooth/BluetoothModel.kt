package com.devidea.chevy.bluetooth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.devidea.chevy.App
import com.devidea.chevy.carsystem.CarModel

object BluetoothModel {
    private const val TAG = "BluetoothModel"

    private const val BT_MSG_SEND_DELAY_TIME = 185
    private const val HIGH_SPEED_BT_MTU = 185
    const val BT_NAME1 = "Sinjet"
    const val BT_NAME2 = "ChanganMazda"

    private var btState = BTState.DISCONNECTED

    private var leBTService: LeBTService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LeBTService.LocalBinder
            leBTService = binder.getService()
            isBound = true
            leBTService?.mBleServiceCallback = mBleServiceCallback
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            leBTService = null
            isBound = false
        }
    }

    private val mBleServiceCallback = object : LeBTService.BleServiceCallback {
        override fun onBTStateChange(state: BTState) {
            Log.d(TAG, "onBTStateChange: $btState -> $state")
            if(state == BTState.CONNECTED && btState != state){
                //CarModel.devModule.handleAppInitOK()
                Log.d(TAG, "is connected")
            }
            btState = state
        }

        override fun onReceived(data: ByteArray, length: Int) {
            CarModel.onRecvMsgFromDevice(data, length)
        }
    }

    // 블루투스 모델을 초기화합니다.
    fun initBTModel(context: Context) {
        val intent = Intent(context, LeBTService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // 블루투스를 연결합니다.
    fun connectBT() {
        //btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.ConnectBT)
        if (btState == BTState.CONNECTED) {
            Log.d("BluetoothModel::", "already connect")
        } else {
            leBTService?.startScan()
        }
    }

    // 블루투스를 해제합니다.
    fun disconnectBT() {
        //btWorkingHandler.sendBTMessage(BTWorkingHandler.BTMessage.DisconnectBT)
    }


    // 블루투스로 메시지를 전송합니다.
    fun sendMessage(data: ByteArray) {
        var length = data.size
        var offset = 0
        while (length > 0) {
            val chunkSize = if (length > HIGH_SPEED_BT_MTU) HIGH_SPEED_BT_MTU else length
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            offset += chunkSize
            length -= chunkSize

            val byteArray = chunk.copyOf()
            leBTService?.sendMessage(byteArray)
        }
    }

    // 블루투스 모드를 초기화합니다.
    fun clearBTMode() {
    }

    // 블루투스가 연결되어 있는지 확인합니다.
    fun isConnected(): Boolean {
        return btState == BTState.CONNECTED
    }
}