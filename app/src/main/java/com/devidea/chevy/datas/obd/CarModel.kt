package com.devidea.chevy.datas.obd

import com.devidea.chevy.Logger
import com.devidea.chevy.datas.obd.protocol.codec.ToureDevCodec
import com.devidea.chevy.eventbus.Event
import com.devidea.chevy.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CarModel {
    private var mMsgEndPos = 0
    private var mMsgLen = 0
    var mMsgBuf = ByteArray(1024)

    fun revMsgBufferHandler(bArr: ByteArray?, length: Int) {
        Logger.d{ bArr.contentToString()}
        // 수신한 데이터의 길이가 0이거나 데이터 배열이 null인 경우 함수 종료
        if (length == 0 || bArr == null) return


        // 수신한 데이터를 버퍼에 저장
        for (i in 0 until length) {
            // 버퍼의 크기가 1024를 넘으면 처음부터 다시 저장
            if (mMsgEndPos >= 1024) mMsgEndPos = 0

            // 버퍼에 수신한 바이트를 저장하고 위치 증가
            mMsgBuf[mMsgEndPos] = bArr[i]
            mMsgEndPos++

            when (mMsgEndPos) {
                // 첫 번째 바이트 처리
                1 -> {
                    // 첫 번째 바이트가 255가 아니면 버퍼 초기화
                    if ((mMsgBuf[0].toInt() and 255) != 255) {
                        mMsgEndPos = 0
                    }
                }
                // 두 번째 바이트 처리
                2 -> {
                    // 두 번째 바이트가 85가 아니면 버퍼 초기화
                    if ((mMsgBuf[1].toInt() and 255) != 85) {
                        mMsgEndPos = 0
                    }
                }
                // 세 번째 바이트 처리
                3 -> {
                    // 세 번째 바이트를 메시지 길이로 설정
                    mMsgLen = mMsgBuf[2].toInt() and 255
                    // 메시지 길이가 128을 초과하면 경고 메시지 출력
                    if (mMsgLen > 128) {
                        Logger.d{"analyseCarInfo if (m_nDataPacketLen > 128)"}
                    }
                }
                // 메시지 끝에 도달한 경우
                else -> {
                    if (mMsgEndPos == mMsgLen + 4) {
                        // 체크섬 계산
                        val checksum = (2 until mMsgEndPos - 1).sumBy { mMsgBuf[it].toInt() and 255 }
                        val calculatedChecksum = checksum and 255

                        // 계산된 체크섬과 수신한 체크섬 비교
                        if (calculatedChecksum == (mMsgBuf[mMsgEndPos - 1].toInt() and 255)) {
                            // 메시지 데이터를 추출
                            val msgData = ByteArray(mMsgLen)
                            System.arraycopy(mMsgBuf, 3, msgData, 0, mMsgLen)
                            // 추출한 데이터를 처리하는 함수 호출
                            msgBranchingHandler(msgData)
                        }

                        // 버퍼와 메시지 길이 초기화
                        resetBuffer()
                    }
                }
            }
        }
    }

    // 버퍼와 메시지 길이를 초기화하는 함수
    private fun resetBuffer() {
        mMsgLen = 0
        mMsgEndPos = 0
    }

    private fun msgBranchingHandler(bArr: ByteArray) {
        val header = bArr[0].toInt()
        //Log.e(TAG, "header : $header, massage : ${bArr.joinToString()}")
        CoroutineScope(Dispatchers.Main).launch {
            when (header) {
                1 ->  EventBus.post(Event.carStateEvent(bArr))
                //8 -> AppModel.getInstance().onRecvMsg(bArr2, bArr2.size)
                63 -> {
                    if (bArr.size > 2 && bArr[1] == 16.toByte()) {
                        val cArr = CharArray(bArr.size - 2) { bArr[it + 2].toChar() }
                        //AppModel.getInstance().setAgencyCode(String(cArr))
                    }
                }

                3 -> EventBus.post(Event.carStateEvent(bArr))
                //4 -> tpmsModule.onRecvTPMS(bArr) 추후 고도화
                //5, 6 -> controlModule.onRecvMsg(bArr, bArr.size) 추후 고도화
                /*52 -> {
                ImageSender.getInstance().onRecvMessage(bArr2, i)
                if (bArr2[1] == 49.toByte()) {
                    NaviModel.getInstance().onRequestTrafficData()
                }
            }*/
                //53 -> PhoneModel.getIns(SinjetApplication.getInstance()).onRecvMsg(bArr2, bArr2.size)
                //54 -> DynamicFontModel.getInstance().onRecvDynamicFontMsg(bArr2)
                55 -> ToureDevCodec.sendHeartbeat()
                /*55, 56, 57 -> {
                CarModel.getInstance().devModule.setMCUUpgradeMethod(1)
                McuUpgradeModel.getInstance().onRecvUpgradeMsg(bArr2, i)
            }
            58 -> {
                CarModel.getInstance().devModule.setMCUUpgradeMethod(2)
                McuUpgradeModelSimple.getInstance().onRecvUpgradeMsg(bArr2, i)
                if (i >= 14 && bArr2[1] == 17.toByte()) {
                    BluetoothModel.getInstance().onTransportSpeed(
                        Data.byte2int(bArr2[10], bArr2[11], bArr2[12], bArr2[13])
                    )
                }
            }*/
            }
        }
    }
}
