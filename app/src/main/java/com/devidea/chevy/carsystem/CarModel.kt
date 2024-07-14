package com.devidea.chevy.carsystem

import ModelMsgHandler
import android.os.Bundle

object CarModel {
    private const val TAG = "CarModel"
    var devModule: DeviceModule = DeviceModule()
    var tpmsModule: TpmsModule = TpmsModule()
    var controlModule: ControlModule = ControlModule()

    private var mMsgEndPos = 0
    private var mMsgLen = 0
    var mMsgBuf = ByteArray(1024)

    fun onRecvMsgFromDevice(bArr: ByteArray?, length: Int) {
        if (length == 0 || bArr == null) {
            return
        }
        for (i in 0 until length) {
            if (mMsgEndPos >= 1024) {
                mMsgEndPos = 0
            }
            mMsgBuf[mMsgEndPos] = bArr[i]
            mMsgEndPos++
            if (mMsgEndPos != 1) {
                if (mMsgEndPos == 2) {
                    if ((mMsgBuf[1].toInt() and 255) != 85) {
                        mMsgEndPos = 0
                    }
                } else if (mMsgEndPos == 3) {
                    mMsgLen = mMsgBuf[2].toInt() and 255
                    if (mMsgLen > 128) {
                        println("analyseCarInfo if (m_nDataPacketLen > 128)")
                    }
                } else if (mMsgEndPos == mMsgLen + 4) {
                    var checksum = 0
                    for (j in 2 until mMsgEndPos - 1) {
                        checksum += mMsgBuf[j].toInt() and 255
                    }
                    val calculatedChecksum = checksum and 255
                    if (calculatedChecksum == (mMsgBuf[mMsgEndPos - 1].toInt() and 255)) {
                        val msgData = ByteArray(mMsgLen)
                        System.arraycopy(mMsgBuf, 3, msgData, 0, mMsgLen)
                        getOnePackageMsg(msgData, mMsgLen)
                    }
                    mMsgLen = 0
                    mMsgEndPos = 0
                }
            } else if ((mMsgBuf[0].toInt() and 255) != 255) {
                mMsgEndPos = 0
            }
        }
    }

    private fun getOnePackageMsg(bArr: ByteArray, length: Int) {
        val modelMsgHandler = ModelMsgHandler(this)
        modelMsgHandler.handleMessage(bArr)
    }

    /*
    fun onAppMessage(bArr: ByteArray) {
        val obtainMessage = getMsgHandler().obtainMessage()
        obtainMessage.what = 1
        val bundle = Bundle()
        bundle.putByteArray(ModelMsgHandler.KEY_MSG_APP, bArr)
        obtainMessage.data = bundle
        getInstance().getMsgHandler().sendMessage(obtainMessage)
    }*/
}
