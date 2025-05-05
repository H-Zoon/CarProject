package com.devidea.chevy.k10s

import com.devidea.chevy.service.ScannedDevice

interface BleManager {
    fun startScan()
    fun stopScan()
    fun requestConnect(device: ScannedDevice)
    fun disconnect()
    fun sendMessage(value: String)
    fun setBleCallback(callback: BleCallback?)
}