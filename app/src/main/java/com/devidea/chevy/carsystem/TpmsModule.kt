package com.devidea.chevy.carsystem;

import android.util.Log
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.datas.Data
import com.devidea.chevy.datas.TireData

// 자동차 TPMS 데이터를 관리하는 클래스
class TpmsModule {
    private var mIsPairing = false
    private val mTireData = Array(4) { TireData() }
    private var mIsRecvedTireData = false


    // TPMS 데이터를 수신하고 처리하는 메서드
    fun onRecvTPMS(bArr: ByteArray) {
        val b = bArr[1]
        when (b.toInt()) {
            0 -> onPairState(Data.byteH4(bArr[2]), Data.byteL4(bArr[2]))
            4 -> {
                val b2 = bArr[2]
                val f =
                    if (bArr[3].toInt() != -1) ((bArr[3].toInt() and 240) shr 4) + ((bArr[3].toInt() and 15) / 10.0f) else 255.0f
                val i = if (bArr[4].toInt() != -1) (bArr[4].toInt() and 255) - 40 else 255
                val b3 = bArr[5]
                Log.d(tag, "onTpms data:$i")
                onTireData(b2.toInt(), f, i, b3)
            }
        }
    }

    // 페어링 상태를 처리하는 메서드
    private fun onPairState(i: Int, i2: Int) {
        Log.i(tag, "onPairState :$i ,$i2")
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
        mTireData.indices
            .filter { it == i }
            .forEach {
                Log.d("tirePair", mTireData[it].vIdPair.toString())
                Log.d("tireState", mTireData[it].vIdState.toString())
                Log.d("tireTemp", mTireData[it].vIdTemp.toString())
                Log.d("tireData", mTireData[it].vIdValue.toString())
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
        Log.d(tag, "onTireData")
        mIsRecvedTireData = true
        if (i in 0..3) {
            handleTireData(mTireData[i], f, i2, b.toInt())
        }
    }

    // 단일 타이어 데이터 처리 및 UI 업데이트 메서드
    private fun handleTireData(tireData: TireData?, f: Float, i: Int, i2: Int) {
        if (tireData == null) return

        val str2 = if (f != 255.0f) "$f BAR" else {
            tireData.tireState = -1
            "N/A BAR"
        }
        Log.d("data","${tireData.vIdValue}")
        Log.d("data","${str2}")

        val str = if (i != 255) "$i℃" else ""
        Log.d("data","${tireData.vIdTemp}")
        Log.d("data","${str}")

        if (tireData.tireState != i2 || tireData.tireValue == 255.0f) {
            tireData.tireState = i2
            val str3 =
                if (tireData.tireValue == 255.0f || tireData.tireValue == -1.0f) "N/A" else "N/A"
            Log.d("data","${tireData.vIdState}")
            Log.d("data","${str3}")
        }
    }

    companion object {
        private const val tag = "tpms"
    }
}

