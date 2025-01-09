package com.devidea.chevy.bluetooth

interface BleManager {
    fun startScan()
    fun stopScan()
    fun disconnect()
    fun sendMessage(value: ByteArray)
    fun setBleCallback(callback: BleCallback?)
}
