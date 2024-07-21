package com.devidea.chevy.codec

import com.devidea.chevy.bluetooth.BluetoothModel

object ToureDevCodec {
    private fun packAndSendMsg(bArr: ByteArray, i: Int) {
        val bArr2 = ByteArray(i + 5)
        var i2 = 0
        bArr2[0] = -1
        bArr2[1] = 85
        var i3 = 2
        bArr2[2] = (i + 1).toByte()
        bArr2[3] = 3
        System.arraycopy(bArr, 0, bArr2, 4, i)
        while (true) {
            val i4 = i + 4
            if (i3 < i4) {
                i2 += bArr2[i3].toInt() and 255
                i3++
            } else {
                bArr2[i4] = (i2 and 255).toByte()
                BluetoothModel.sendMessage(bArr2)
                return
            }
        }
    }

    fun sendInit(i: Int) {
        packAndSendMsg(byteArrayOf(0, i.toByte()), 2)
    }

    fun sendAppPage(i: Int) {
        packAndSendMsg(byteArrayOf(13, i.toByte()), 2)
    }

    fun sendConnectECU(i: Int) {
        packAndSendMsg(byteArrayOf(2, i.toByte()), 2)
    }

    fun sendClearErr() {
        packAndSendMsg(byteArrayOf(6, 0), 2)
    }

    fun sendStartFastDetect() {
        packAndSendMsg(byteArrayOf(14, 0), 2)
    }

    fun sendHeartbeat() {
        packAndSendMsg(byteArrayOf(-86, 0), 2)
    }
}