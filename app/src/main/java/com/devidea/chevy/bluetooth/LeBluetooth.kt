package com.devidea.chevy.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.IBinder
import android.util.Log
import com.devidea.chevy.App

class LeBluetooth {

    private var mLeBluetoothService: LeBluetoothService? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    var mLeBluetoothCallback: LeBluetoothCallback? = null

    interface LeBluetoothCallback {
        fun onBTStateChange(state: BTState)
        fun onReceived(data: ByteArray, length: Int)
    }

    private val mBleServiceCallback = object : LeBluetoothService.BleServiceCallback {
        override fun onBTStateChange(state: BTState) {
            mLeBluetoothCallback?.onBTStateChange(state)
        }

        override fun onReceived(data: ByteArray, length: Int) {
            mLeBluetoothCallback?.onReceived(data, length)
        }
    }

    private val mBleServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mLeBluetoothService = (service as LeBluetoothService.LocalBinder).getService()
            mLeBluetoothService?.setBleServiceCallback(mBleServiceCallback)
            BluetoothModel.connectBT()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mLeBluetoothService = null
        }
    }

    fun initLeBluetooth(): Boolean {
        mBluetoothManager = mBluetoothManager ?: App.instance.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothAdapter ?: mBluetoothManager?.adapter

        mBluetoothAdapter?.takeIf { !it.isEnabled }?.enable()

        mLeBluetoothService ?: Intent(App.instance, LeBluetoothService::class.java).also { intent ->
            App.instance.bindService(intent, mBleServiceConnection, Context.BIND_AUTO_CREATE)
        }

        return mBluetoothAdapter != null
    }

    fun setLeBluetoothCallback(callback: LeBluetoothCallback) {
        mLeBluetoothCallback = callback
    }

    fun sendMessage(data: ByteArray) {
        mLeBluetoothService?.sendMessage(data)
    }

    fun setBleServiceCallback(callback: LeBluetoothService.BleServiceCallback) {
        mLeBluetoothService?.setBleServiceCallback(callback)
    }

    fun requestConnect() {
        mLeBluetoothService?.requestConnection()
    }

    fun requestDisconnect() {
        mLeBluetoothService?.disconnectDevice()
    }

    fun clearBT() {
        mLeBluetoothService?.takeIf { it.isBTConnected() }?.disconnectDevice()
        mLeBluetoothService?.also {
            App.instance.unbindService(mBleServiceConnection)
            mLeBluetoothService = null
        }
    }

    fun tryToKeepConnected() {
        if (mLeBluetoothService == null) {
            initLeBluetooth()
        } else if (!mLeBluetoothService!!.isBTConnected() && !mLeBluetoothService!!.isInConnecttingPeriod()) {
            mLeBluetoothService!!.requestConnection()
        }
    }
}
