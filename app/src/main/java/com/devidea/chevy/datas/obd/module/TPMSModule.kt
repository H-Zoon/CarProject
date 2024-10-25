package com.devidea.chevy.datas.obd.module;

import android.util.Log
import com.devidea.chevy.Logger
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.datas.obd.protocol.OBDProtocol
import com.devidea.chevy.datas.obd.TPMSData

// 자동차 TPMS 데이터를 관리하는 클래스
class TPMSModule {
    private var mIsPairing = false
    private val mTPMSData = Array(4) { TPMSData() }
    private var mIsRecvedTireData = false


    // TPMS 데이터를 수신하고 처리하는 메서드
    fun onRecvTPMS(bArr: ByteArray) {
        val b = bArr[1]
        when (b.toInt()) {
            0 -> onPairState(OBDProtocol.byteH4(bArr[2]), OBDProtocol.byteL4(bArr[2]))
            4 -> {
                val b2 = bArr[2]
                val f =
                    if (bArr[3].toInt() != -1) ((bArr[3].toInt() and 240) shr 4) + ((bArr[3].toInt() and 15) / 10.0f) else 255.0f
                val i = if (bArr[4].toInt() != -1) (bArr[4].toInt() and 255) - 40 else 255
                val b3 = bArr[5]
                Logger.d { "onTpms data:$i" }
                onTireData(b2.toInt(), f, i, b3)
            }
        }
    }

    // 페어링 상태를 처리하는 메서드
    private fun onPairState(i: Int, i2: Int) {
        Logger.i { "onPairState :$i ,$i2" }
        when (i2) {
            0 -> {
                mIsPairing = true
            }

            1 -> {
                mIsPairing = false
                refreshPairsucceed(i)
            }

            2 -> {
                mIsPairing = false
            }
        }
    }

    // 페어링 성공 시 UI 업데이트 메서드
    private fun refreshPairsucceed(i: Int) {
        mTPMSData.indices
            .filter { it == i }
            .forEach {
                Logger.d { "tirePair: ${mTPMSData[it].vIdPair}" }
                Logger.d { "tirePair:  $mTPMSData[it].vIdState}" }
                Logger.d { "tirePair: $mTPMSData[it].vIdTemp}" }
                Logger.d { "tirePair:  $mTPMSData[it].vIdValue}" }
            }
    }

    // 페어링 요청 메시지 전송 메서드
    fun sendPairTpms(i: Int) {
        val bArr = byteArrayOf(0, ((i and 15) shl 4).toByte())
        packAndSendTpmsMsg(bArr, bArr.size)
    }

    // TPMS 메시지를 패킹하고 전송하는 메서드
    private fun packAndSendTpmsMsg(bArr: ByteArray, i: Int) {
        val bArr2 = ByteArray(i + 5)
        var i2 = 0
        bArr2[0] = -1
        bArr2[1] = 85.toByte()
        bArr2[2] = (i + 1).toByte()
        bArr2[3] = 4.toByte()
        System.arraycopy(bArr, 0, bArr2, 4, i)
        for (i3 in 2 until i + 4) {
            i2 += bArr2[i3].toInt() and 255
        }
        bArr2[i + 4] = (i2 and 255).toByte()
        BluetoothModel.sendMessage(bArr2)
    }

    // 타이어 데이터를 처리하는 메서드
    private fun onTireData(i: Int, f: Float, i2: Int, b: Byte) {
        mIsRecvedTireData = true
        if (i in 0..3) {
            handleTireData(mTPMSData[i], f, i2, b.toInt())
        }
    }

    // 단일 타이어 데이터 처리 및 UI 업데이트 메서드
    private fun handleTireData(TPMSData: TPMSData?, f: Float, i: Int, i2: Int) {
        if (TPMSData == null) return

        val str2 = if (f != 255.0f) "$f BAR" else {
            TPMSData.tireState = -1
            "N/A BAR"
        }
        Logger.d { "${TPMSData.vIdValue}" }
        Logger.d { str2 }

        val str = if (i != 255) "$i℃" else ""
        Logger.d { "${TPMSData.vIdTemp}" }
        Logger.d { str }

        if (TPMSData.tireState != i2 || TPMSData.tireValue == 255.0f) {
            TPMSData.tireState = i2
            val str3 =
                if (TPMSData.tireValue == 255.0f || TPMSData.tireValue == -1.0f) "N/A" else "N/A"
            Logger.d { "${TPMSData.vIdState}" }
            Logger.d { "${str3}" }
        }
    }

    companion object {
        private const val tag = "tpms"
    }
}

