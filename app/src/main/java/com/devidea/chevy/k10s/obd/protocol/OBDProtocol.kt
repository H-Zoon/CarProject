package com.devidea.chevy.k10s.obd.protocol

// 데이터 유틸리티 클래스
class OBDProtocol {
    companion object {
        fun bit0(b: Byte): Int = if ((b.toInt() and 1) == 1) 1 else 0
        fun bit1(b: Byte): Int = if ((b.toInt() and 2) == 2) 1 else 0
        fun bit2(b: Byte): Int = if ((b.toInt() and 4) == 4) 1 else 0
        fun bit3(b: Byte): Int = if ((b.toInt() and 8) == 8) 1 else 0
        fun bit4(b: Byte): Int = if ((b.toInt() and 16) == 16) 1 else 0
        fun bit5(b: Byte): Int = if ((b.toInt() and 32) == 32) 1 else 0
        fun bit6(b: Byte): Int = if ((b.toInt() and 64) == 64) 1 else 0
        fun bit7(b: Byte): Int = if ((b.toInt() and 128) == 128) 1 else 0

        fun byte2int(b: Byte): Int = b.toInt() and 255

        fun byteH4(b: Byte): Int = (b.toInt() and 240) shr 4

        fun byteL4(b: Byte): Int = b.toInt() and 15

        fun byte2int(b: Byte, b2: Byte): Int = (byte2int(b) or (byte2int(b2) shl 8)) and 0xFFFF

        fun byte2int(b: Byte, b2: Byte, b3: Byte, b4: Byte): Int =
            byte2int(b) + (byte2int(b2) shl 8) + (byte2int(b3) shl 16) + (byte2int(b4) shl 24)

        fun byte2Unicode(bArr: ByteArray, i: Int, i2: Int): String {
            val stringBuffer = StringBuffer("")
            var index = i
            while (index < i2) {
                val i4 = bArr[index].toInt().let { if (it < 0) it + 256 else it }
                val i6 = bArr[++index].toInt().let { if (it < 0) it + 256 else it }
                stringBuffer.append((i6 + (i4 shl 8)).toChar())
                index++
            }
            return stringBuffer.toString()
        }

        fun byte2Unicode(bArr: ByteArray, i: Int): String = byte2Unicode(bArr, 0, i)

        fun byte2Unicode(bArr: ByteArray): String = byte2Unicode(bArr, 0, bArr.size)

        fun unicode2Byte(str: String): ByteArray {
            val length = str.length
            val bArr = ByteArray(length shl 1)
            var i = 0
            for (i2 in 0 until length) {
                val charAt = str[i2].toInt()
                bArr[i++] = (charAt and 255).toByte()
                bArr[i++] = (charAt shr 8).toByte()
            }
            return bArr
        }

        fun isValidString(str: String?): Boolean = !str.isNullOrEmpty()
    }
}