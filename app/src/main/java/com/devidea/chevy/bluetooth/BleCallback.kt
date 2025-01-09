package com.devidea.chevy.bluetooth

import androidx.core.app.NotificationCompat.MessagingStyle.Message

interface BleCallback {
    fun onStateChanged(state: BTState)
    fun onCharacteristicRead(data: ByteArray, size : Int)
    fun onMessageSent()
    fun onMtuChanged(mtu: Int)
    fun onError(message: String)
}