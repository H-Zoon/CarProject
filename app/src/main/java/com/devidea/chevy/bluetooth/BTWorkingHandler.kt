package com.devidea.chevy.bluetooth

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor

class BTWorkingHandler {

    var msgBuffCount: Int = 0

    private val handlerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    sealed class BTMessage {
        object InitBT : BTMessage()
        object ConnectBT : BTMessage()
        object DisconnectBT : BTMessage()
        object KeepConnectedBT : BTMessage()
        data class SendBT(val byteArray: ByteArray?) : BTMessage()
    }

    private val btActor = handlerScope.actor<BTMessage> {
        for (msg in channel) {
            when (msg) {
                is BTMessage.InitBT -> BluetoothModel.initBTInThread()
                is BTMessage.ConnectBT -> BluetoothModel.connectBTInThread()
                is BTMessage.DisconnectBT -> BluetoothModel.diconnectBTInThread()
                is BTMessage.KeepConnectedBT -> BluetoothModel.tryToKeepConnectedInThread()
                //TODO 이후 구현
                is BTMessage.SendBT -> msg.byteArray?.let { byteArray ->
                    BluetoothModel.sendMessageInThread(byteArray)
                    if (msgBuffCount > 0) {
                        msgBuffCount--
                    }
                }
            }
        }
    }

    fun sendBTMessage(message: BTMessage) {
        handlerScope.launch {
            btActor.send(message)
        }
    }

    fun cleanup() {
        handlerScope.cancel()
    }
}
