package com.devidea.aicar.k10s

import android.bluetooth.BluetoothDevice

interface BleCallback {
    fun onStateChanged()
    fun scannedDevice(name: String?, address: String, device: BluetoothDevice)
    fun onCharacteristicRead(data: ByteArray, size : Int)
    fun onMtuChanged(mtu: Int)
    fun onSuccess()
    fun onError(message: String)
}