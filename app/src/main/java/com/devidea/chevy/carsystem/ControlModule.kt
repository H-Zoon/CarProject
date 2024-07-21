package com.devidea.chevy.carsystem

import android.util.Log
import com.devidea.chevy.bluetooth.BluetoothModel

class ControlModule {
    companion object {
        private const val CMD_LOCK = 3
        private const val CMD_POWER = 1
        private const val CMD_SKYLIGHT = 4
        private const val CMD_TRUNK = 5
        private const val CMD_WINDOWS = 2
        private const val OFF = 0
        private const val ON = 1
        private const val TAG = "ControlModule"
    }
    private var mControllingItem = ControlFuncs.CAR_TRUNK_OPEN
    private var mIsSettingPassword = false
    private var mIsVerifySucceed = false
    private var mPassword = ""
    private var mIsCarControlSupport = true
    private var mIncState = 1
    private var mDecState = 0
    private var mOkState = 0
    private var isShowCantSetPswNow = false

    fun isCarControlSupport(): Boolean {
        return mIsCarControlSupport
    }

    fun makeHUDSetMsgAndSend() {
        Log.d(TAG, "send button +:$mIncState -:$mDecState ok:$mOkState")
        val bArr = byteArrayOf(0, mIncState.toByte(), mDecState.toByte(), mOkState.toByte())
        packHudMsgAndSend(bArr, bArr.size)
    }

    fun onPasswordEnter(str: String = "123456") {
        Log.i(TAG, "onPasswordEnter $str setting psw? $mIsSettingPassword")
        if (mIsSettingPassword) {
            onSettingPasswordEnter(str)
            return
        }
        if (mControllingItem == ControlFuncs.CAR_INVALID_FUN) {
            return
        }
        val (cmd, state) = when (mControllingItem) {
            ControlFuncs.CAR_LOCK -> CMD_LOCK to OFF
            ControlFuncs.CAR_POWER_OFF -> CMD_POWER to OFF
            ControlFuncs.CAR_POWER_ON -> CMD_POWER to ON
            ControlFuncs.CAR_SKYLIGHT_OPEN -> CMD_SKYLIGHT to ON
            ControlFuncs.CAR_TRUNK_OPEN -> CMD_TRUNK to ON
            ControlFuncs.CAR_UNLOCK -> CMD_LOCK to ON
            ControlFuncs.CAR_WINDOW_CLOSE -> CMD_WINDOWS to OFF
            ControlFuncs.CAR_WINDOW_OPEN -> CMD_WINDOWS to ON
            else -> -1 to -1
        }
        if (cmd != -1 && state != -1) {
            Log.i(TAG, "send controlCmd: $cmd action: $state")
            sendControlCmd(cmd, state, str)
        }
        mControllingItem = ControlFuncs.CAR_INVALID_FUN
    }

    fun sendControlMessage(conMsg: ControlFuncs) {

        val (cmd, state) = when (conMsg) {
            ControlFuncs.CAR_LOCK -> CMD_LOCK to OFF
            ControlFuncs.CAR_POWER_OFF -> CMD_POWER to OFF
            ControlFuncs.CAR_POWER_ON -> CMD_POWER to ON
            ControlFuncs.CAR_SKYLIGHT_OPEN -> CMD_SKYLIGHT to ON
            ControlFuncs.CAR_TRUNK_OPEN -> CMD_TRUNK to ON
            ControlFuncs.CAR_UNLOCK -> CMD_LOCK to ON
            ControlFuncs.CAR_WINDOW_CLOSE -> CMD_WINDOWS to OFF
            ControlFuncs.CAR_WINDOW_OPEN -> CMD_WINDOWS to ON
            else -> -1 to -1
        }
        if (cmd != -1 && state != -1) {
            Log.i(TAG, "send controlCmd: $cmd action: $state")
            //sendControlCmd(cmd, state, str)

            val bArr = ByteArray(2)
            bArr[0] = cmd.toByte()
            bArr[1] = state.toByte()
            packAndSendMsg(bArr, bArr.size)
        }
        //mControllingItem = ControlFuncs.CAR_INVALID_FUN
    }


    private fun onSettingPasswordEnter(password: String) {
        mIsSettingPassword = false
        sendSetPassword(password)
    }

    fun onRecvMsg(bArr: ByteArray, length: Int) {
        //Log.i(tag, "onRecv ${String.format("%02X ", bArr[1])}")
        when (bArr[1].toInt()) {
            6 -> onRecvHUDSetMsg(bArr, length)
                    -96 -> onPswVerifyResult(bArr[2].toInt())
                    -95 -> onPswCanSet(bArr[2].toInt())
                    -94 -> {
                Log.i(TAG, "onSet psw result")
                onSetPswResult(bArr[2].toInt(), "123456")
            }
            -93 -> onNeedVerifyPsw()
                    -32 -> onCmdExcuteState(bArr[2].toInt(), bArr[3].toInt())
                    -31 -> if (bArr[2].toInt() == 0) onNewCmdAvalible()
                    -30 -> onExcuteCmdErr(bArr[2].toInt(), bArr[3].toInt())
        }
    }

    private fun sendMCUErrorAck() {
        val bArr = byteArrayOf(-84, 0)
        packAndSendMsgID06(bArr, bArr.size)
    }

    private fun onRecvHUDSetMsg(bArr: ByteArray, length: Int) {
        //뭔가를 서버로 보내고있음.
        /*if (length <= 1) return

                when (bArr[1].toInt() and 255) {
            0 -> {
                Log.d(tag, "onQuery button state")
                makeHUDSetMsgAndSend()
            }
            170 -> AppModel.getInstance().setAxelaHUDHardwareInfo(
            if (length > 2) bArr[2].toInt() and 255 else 0,
            if (length > 8) (bArr[8].toInt() and 255 shl 8) + (bArr[7].toInt() and 255) else 0,
            if (length > 12) (bArr[12].toInt() and 255 shl 8) + (bArr[11].toInt() and 255) else 0
            )
            171 -> AppModel.getInstance().setAxelaHUDHardwareInfo(
            if (length > 2) bArr[2].toInt() and 255 else 0,
            if (length > 4) (bArr[4].toInt() and 255 shl 8) + (bArr[3].toInt() and 255) else 0,
            if (length > 6) (bArr[6].toInt() and 255 shl 8) + (bArr[5].toInt() and 255) else 0
            )
            172 -> {
                val errorMsg = bArr.copyOfRange(2, length).toString(Charsets.UTF_8)
                sendMCUErrorAck()
                Log.e(tag, "ErrorDetected : $errorMsg")
            }
        }*/
    }

    private fun onNeedVerifyPsw() {
        Log.i(TAG, "onNeedVerifyPsw")
        mIsCarControlSupport = true
        sendVerifyPsw(mPassword)
    }

    private fun onSetPswResult(result: Int, password: String) {
        Log.i(TAG, "onSetPswResult: $result psw: $password")
    }

    private fun onPswCanSet(result: Int) {
        Log.i(TAG, "onCanSetPassword: $result")
        when {
            result == 0 -> {
                mIsSettingPassword = true
            }
            result == 1 && !isShowCantSetPswNow -> {
                isShowCantSetPswNow = false
                sendRequestSetPsw()
            }
        }
    }

    private fun onPswVerifyResult(result: Int) {
        Log.i(TAG, "onPswVerifyResult: $result")
        mIsVerifySucceed = result == 0
        if (mIsVerifySucceed) {
            mIsCarControlSupport = true
        }
    }

    private fun onExcuteCmdErr(i: Int, errorCode: Int) {
        val message = when (errorCode) {
            0 -> "명령을 실행하는 중입니다. 나중에 다시 시도해 주세요."
            1 -> "운전 중에는 트렁크를 열 수 없습니다."
            2 -> "원격 트렁크 열기를 지원하지 않습니다."
            3 -> "자동차의 시동이 걸렸으나 원격으로 시동을 걸 수 없습니다."
            4 -> "원격으로 두 번 시작되었으며 다시 점화해야 합니다."
            5 -> "현재로서는 원격으로 시동을 끌 수 없습니다."
            6 -> "운행하기 전에 엔진을 끄고 차에서 내리십시오."
            7 -> "제어 비밀번호 오류"
            8 -> "잘못된 비밀번호가 너무 많습니다. 한 시간 후에 다시 시도해 주세요."
            else -> ""
        }
        Log.d(TAG, "onExcuteCmdErr : $message")
    }

    private fun onNewCmdAvalible() {
    }

    private fun onCmdExcuteState(i: Int, state: Int) {
        val message = when (i) {
            CMD_POWER -> if (state == OFF) "시동을 끄는 중" else "시동을 거는 중"
            CMD_WINDOWS -> if (state == OFF) "차창을 닫는 중" else "차창을 여는 중"
            CMD_LOCK -> if (state == OFF) "차를 잠그는 중" else "잠금 해제 중"
            CMD_SKYLIGHT -> if (state == OFF) "선루프를 닫는 중" else "선루프를 여는 중"
            CMD_TRUNK -> if (state == ON) "트렁크를 여는 중" else "명령을 실행 중"
            else -> "명령을 실행 중"
        }
        Log.d(TAG, "onCmdExcuteState : $message")
    }

    private fun sendRequestSetPsw() {
        val bArr = byteArrayOf(-95, 0)
        packAndSendMsg(bArr, bArr.size)
    }

    private fun sendControlCmd(cmd: Int, state: Int, password: String) {
        val bArr = ByteArray(10)
        bArr[0] = cmd.toByte()
        bArr[1] = state.toByte()
        for (i in 0..5) {
            bArr[i + 2] = password[i].toByte()
        }
        packAndSendMsg(bArr, bArr.size)
    }

    private fun sendVerifyPsw(password: String) {
        if (password.length < 6) return
        val bArr = ByteArray(9)
        bArr[0] = -96
        password.toByteArray().copyInto(bArr, 1, 0, 6)
        packAndSendMsg(bArr, bArr.size)
    }

    private fun sendSetPassword(password: String) {
        if (password.length < 6) return
        val bArr = ByteArray(9)
        bArr[0] = -94
        password.toByteArray().copyInto(bArr, 1, 0, 6)
        packAndSendMsg(bArr, bArr.size)
    }

    fun sendSimulatorKey(key: Int) {
        val bArr = byteArrayOf(-95, key.toByte())
        packHudMsgAndSend(bArr, bArr.size)
    }

    private fun packAndSendMsg(bArr: ByteArray, length: Int) {
        val bArr2 = ByteArray(length + 5)
        bArr2[0] = -1
        bArr2[1] = 85
        bArr2[2] = (length + 1).toByte()
        bArr2[3] = 5
        System.arraycopy(bArr, 0, bArr2, 4, length)
        bArr2[length + 4] = bArr2.drop(2).take(length + 2).sumOf { it.toInt() and 255 }.toByte()
        BluetoothModel.sendMessage(bArr2)
    }

    private fun packAndSendMsgID06(bArr: ByteArray, length: Int) {
        val bArr2 = ByteArray(length + 5)
        bArr2[0] = -1
        bArr2[1] = 85
        bArr2[2] = (length + 1).toByte()
        bArr2[3] = 6
        System.arraycopy(bArr, 0, bArr2, 4, length)
        bArr2[length + 4] = bArr2.drop(2).take(length + 2).sumOf { it.toInt() and 255 }.toByte()
        BluetoothModel.sendMessage(bArr2)
    }

    private fun packHudMsgAndSend(bArr: ByteArray, i: Int) {
        val sb = StringBuilder()
        sb.append("send hud msg buf0:")
        var i2 = 0
        sb.append(bArr[0].toInt())
        sb.append(" buf1:")
        sb.append(bArr[1].toInt())
        val bArr2 = ByteArray(i + 5)
        bArr2[0] = -1
        bArr2[1] = 85
        var i3 = 2
        bArr2[2] = (i + 1).toByte()
        bArr2[3] = 6
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
}
