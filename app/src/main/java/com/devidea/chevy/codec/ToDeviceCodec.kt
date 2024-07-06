package com.devidea.chevy.codec

import android.util.Log
import com.devidea.chevy.bluetooth.BluetoothModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class ToDeviceCodec {
    companion object {
        const val HUD_PAGE_MAIN = 1
        const val HUD_PAGE_SETTING = 4
        const val HUD_PAGE_TPMS = 2
        const val HUD_PAGE_VOLTAGE_TEMP = 3

        private fun packAndSendMsg(bArr: ByteArray, i: Int) {
            val bArr2 = ByteArray(i + 5)
            var i2 = 0
            bArr2[0] = -1
            bArr2[1] = 85
            var i3 = 2
            bArr2[2] = (i + 1).toByte()
            bArr2[3] = 1
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

        @JvmStatic
        fun sendJumpPage(i: Int) {
            val bArr = byteArrayOf(0, i.toByte())
            packAndSendMsg(8, bArr, bArr.size)
        }

        @JvmStatic
        fun sendChangeSetting(i: Int, i2: Int) {
            val bArr = byteArrayOf(1, i.toByte(), i2.toByte())
            packAndSendMsg(8, bArr, bArr.size)
        }

        @JvmStatic
        fun sendAdjustHeight(i: Int) {
            val bArr = byteArrayOf(2, i.toByte())
            packAndSendMsg(8, bArr, bArr.size)
        }

        @JvmStatic
        fun sendTrafficStatus(bArr: ByteArray) {
            val bArr2 = ByteArray(bArr.size + 1)
            bArr2[0] = 49
            System.arraycopy(bArr, 0, bArr2, 1, bArr.size)
            packAndSendMsg(52, bArr2, bArr2.size)
        }

        @JvmStatic
        fun sendNotification(i: Int, str: String?, str2: String?) {
            var i2 = str?.toByteArray()?.size ?: 0
            if (i2 > 255) {
                i2 = 255
            }
            var i3 = str2?.toByteArray()?.size ?: 0
            if (i3 > 255) {
                i3 = 255
            }
            val i4 = i2 + 4
            val bArr = ByteArray(i4 + i3)
            bArr[0] = 52
            bArr[1] = i.toByte()
            bArr[2] = i2.toByte()
            str?.toByteArray()?.let {
                System.arraycopy(it, 0, bArr, 3, i2)
            }
            bArr[i2 + 3] = i3.toByte()
            str2?.toByteArray()?.let {
                System.arraycopy(it, 0, bArr, i4, i3)
            }
            packAndSendMsg(52, bArr, bArr.size)
        }

        private fun packAndSendMsg(i: Int, bArr: ByteArray, i2: Int) {
            val bArr2 = ByteArray(i2 + 5)
            var i3 = 0
            bArr2[0] = -1
            bArr2[1] = 85
            var i4 = 2
            bArr2[2] = (i2 + 1).toByte()
            bArr2[3] = i.toByte()
            System.arraycopy(bArr, 0, bArr2, 4, i2)
            while (true) {
                val i5 = i2 + 4
                if (i4 < i5) {
                    i3 += bArr2[i4].toInt() and 255
                    i4++
                } else {
                    bArr2[i5] = (i3 and 255).toByte()
                    BluetoothModel.sendMessage(bArr2)
                    return
                }
            }
        }
    }

    private fun packAndSendMsg(bArr: ByteArray, i: Int) {
        val bArr2 = ByteArray(i + 5)
        var i2 = 0
        bArr2[0] = -1
        bArr2[1] = 85
        var i3 = 2
        bArr2[2] = (i + 1).toByte()
        bArr2[3] = 1
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

    fun sendnaviInfo(i: Int, i2: Int) {
        packAndSendMsg(
            byteArrayOf(
                0,
                i.toByte(),
                (i2 and 255).toByte(),
                ((i2 and 65280) shr 8).toByte(),
                ((i2 and 16711680) shr 16).toByte(),
                ((i2 and -16777216) shr 24).toByte()
            ), 6
        )
    }

    fun sendLineInfo(i: Int, i2: Int) {
        val bArr = byteArrayOf(
            2,
            (i and 255).toByte(),
            ((i and 65280) shr 8).toByte(),
            ((i and 16711680) shr 16).toByte(),
            ((i and 0xff000000.toInt()) shr 24).toByte(),
            (i2 and 255).toByte(),
            ((i2 and 65280) shr 8).toByte(),
            ((i2 and 16711680) shr 16).toByte(),
            ((i2 and 0xff000000.toInt()) shr 24).toByte()
        )
        packAndSendMsg(bArr, bArr.size)
    }

    fun notifyIsNaviRunning(b: Byte) {
        Log.i("navi", "notifyIsNaviRunning:${b.toInt()}")
        packAndSendMsg(byteArrayOf(-32, b), 2)
    }

    fun sendLimitSpeed(i: Int, i2: Int) {
        packAndSendMsg(
            byteArrayOf(
                1,
                (i and 255).toByte(),
                ((i and 65280) shr 8).toByte(),
                (i2 and 255).toByte(),
                ((i2 and 65280) shr 8).toByte()
            ), 5
        )
    }

    fun sendCameraDistance(i: Int, i2: Int, i3: Int) {
        packAndSendMsg(
            byteArrayOf(
                16,
                (i and 255).toByte(),
                ((i and 65280) shr 8).toByte(),
                i2.toByte(),
                i3.toByte()
            ), 5
        )
    }

    fun sendCameraDistanceEx(i: Int, i2: Int, i3: Int) {
        packAndSendMsg(
            byteArrayOf(
                19,
                (i and 255).toByte(),
                ((i and 65280) shr 8).toByte(),
                i2.toByte(),
                i3.toByte()
            ), 5
        )
    }

    fun sendLaneInfo(iArr: IntArray?) {
        if (iArr == null) {
            val bArr = byteArrayOf(17, 0)
            packAndSendMsg(bArr, bArr.size)
            return
        }
        val bArr2 = ByteArray(iArr.size + 2)
        bArr2[0] = 17
        bArr2[1] = iArr.size.toByte()
        for (i in 2 until bArr2.size) {
            bArr2[i] = iArr[i - 2].toByte()
        }
        packAndSendMsg(bArr2, bArr2.size)
    }

    fun sendLaneInfoEx(iArr: IntArray?) {
        if (iArr == null) {
            val bArr = byteArrayOf(18)
            packAndSendMsg(bArr, bArr.size)
            return
        }
        val bArr2 = ByteArray(iArr.size + 1)
        bArr2[0] = 18
        for (i in 1 until bArr2.size) {
            bArr2[i] = iArr[i - 1].toByte()
        }
        packAndSendMsg(bArr2, bArr2.size)
    }

    fun sendLaneInfoExV2(iArr: IntArray?) {
        if (iArr == null) {
            val bArr = byteArrayOf(21)
            packAndSendMsg(bArr, bArr.size)
            return
        }
        val bArr2 = ByteArray(iArr.size + 1)
        bArr2[0] = 21
        for (i in 1 until bArr2.size) {
            bArr2[i] = iArr[i - 1].toByte()
        }
        packAndSendMsg(bArr2, bArr2.size)
    }

    fun sendNextRoadName(str: String?) {
        if (str == null) {
            return
        }
        val bArr = ByteArray(str.toByteArray().size + 1)
        bArr[0] = 5
        System.arraycopy(str.toByteArray(), 0, bArr, 1, str.toByteArray().size)
        packAndSendMsg(bArr, bArr.size)
    }

    fun sendCurrentTime() {
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis
        val i = calendar.get(Calendar.HOUR_OF_DAY)
        val i2 = calendar.get(Calendar.MINUTE)
        val i3 = calendar.get(Calendar.SECOND)
        val i4 = calendar.get(Calendar.YEAR)
        val i5 = calendar.get(Calendar.MONTH)
        val i6 = calendar.get(Calendar.DAY_OF_MONTH)
        val bArr = byteArrayOf(
            7,
            i.toByte(),
            i2.toByte(),
            i3.toByte(),
            (i4 and 255).toByte(),
            ((i4 and 65280) shr 8).toByte(),
            (i5 + 1).toByte(),
            i6.toByte()
        )
        Log.i("ing", "sendCurrentTime$i,$i2,$i3,$i4,$i5,$i6")
        packAndSendMsg(bArr, bArr.size)


            val bArr2 = byteArrayOf(0xFF.toByte(), 0x55, 6, 0x0A, 0x01, 0x02, 0x03, 0x04, 0x05, 0x1F.toByte())
            packAndSendMsg(bArr2, bArr2.size)

    }
}