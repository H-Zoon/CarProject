package com.devidea.chevy.k10s.obd.protocol.codec

object Msgs {
    fun sendLaneInfo(iArr: IntArray?): Pair<ByteArray, Int> {
        if (iArr == null) {
            val bArr = byteArrayOf(17, 0)
            return Pair(bArr, bArr.size)
        }
        val bArr2 = ByteArray(iArr.size + 2)
        bArr2[0] = 17
        bArr2[1] = iArr.size.toByte()
        for (i in 2 until bArr2.size) {
            bArr2[i] = iArr[i - 2].toByte()
        }
        return Pair(bArr2, bArr2.size)
    }

    fun sendCameraDistance(distance: Int, alert: Int, type: Int): Pair<ByteArray, Int> {
        return Pair(
            byteArrayOf(
                16,
                (distance and 255).toByte(),
                ((distance and 65280) shr 8).toByte(),
                alert.toByte(),
                type.toByte()
            ), 5
        )
    }

    fun sendLimitSpeed(distance: Int, limitedSpeed: Int): Pair<ByteArray, Int> {
        return Pair(
            byteArrayOf(
                1,
                (distance and 255).toByte(),
                ((distance and 65280) shr 8).toByte(),
                (limitedSpeed and 255).toByte(),
                ((limitedSpeed and 65280) shr 8).toByte()
            ), 5
        )
    }

    fun sendNextInfo(icon: Int, distance: Int): Pair<ByteArray, Int> {
        return Pair(
            byteArrayOf(
                0,
                icon.toByte(),
                (distance and 255).toByte(),
                ((distance and 65280) shr 8).toByte(),
                ((distance and 16711680) shr 16).toByte(),
                ((distance and -16777216) shr 24).toByte()
            ), 6
        )
    }

    fun sendInit(i: Int): Pair<ByteArray, Int> {
        return Pair(byteArrayOf(0, i.toByte()), 2)
    }

    fun sendAppPage(i: Int): Pair<ByteArray, Int> {
        return Pair(byteArrayOf(13, i.toByte()), 2)
    }

    fun sendConnectECU(i: Int): Pair<ByteArray, Int> {
        return Pair(byteArrayOf(2, i.toByte()), 2)
    }

    fun sendClearErr(): Pair<ByteArray, Int> {
        return Pair(byteArrayOf(6, 0), 2)
    }

    fun sendStartFastDetect(): Pair<ByteArray, Int> {
        return Pair(byteArrayOf(14, 0), 2)
    }

    fun sendHeartbeat(): Pair<ByteArray, Int> {
        return Pair(byteArrayOf(-86, 0), 2)
    }
}