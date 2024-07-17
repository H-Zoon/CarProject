package com.devidea.chevy.carsystem;

import android.util.Log
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.codec.ToDeviceCodec
import com.devidea.chevy.codec.ToureDevCodec
import com.devidea.chevy.datas.Data
import java.text.DecimalFormat
import java.util.Locale
import kotlin.experimental.and

class CarEventModule {

    companion object {
        const val MSG_NO_INFO_TIMEOUT = 100
        const val TEMPERATURE_RANGE_MAX = 112
        const val TEMPERATURE_RANGE_MIN = 0
        const val VOLTAGE_RANGE_MAX = 15.0f
        const val VOLTAGE_RANGE_MIN = 10.0f
        private const val TAG = "CarEventModule"
    }

    private var mObdData: OBDData = OBDData()

    val mNaviCodec = ToDeviceCodec()
    private var isTimeSync = false
    var mToureCodec = ToureDevCodec()

    private var mStrVinFirstHalf = ""
    private var mStrVinSecondHalf = ""
    var mRotate = 0
    var mLeftFront = 0
    var mRightFront = 0
    var mLeftRear = 0
    var mRightRear = 0
    var mTrunk = 0
    var mRemainGas = -1
    var mMileage = -1
    var mHandBrake = -1
    var mSeatbelt = -1
    var mGear = -1
    var mGearNum = -1
    var SpeedAdjustArr = byteArrayOf(100, 102, 104, 106, 108, 110, 111, 112, 113, 114)
    var mErrCount = -1
    var mVoltage = -1
    var mSolarTermDoor = -1
    var mWaterTemperature = -1
    var mThreeCatalystTemperatureBankOne1 = -1
    var mThreeCatalystTemperatureBankOne2 = -1
    var mThreeCatalystTemperatureBankTwo1 = -1
    var mThreeCatalystTemperatureBankTwo2 = -1
    var mMemission = ""
    var mMeterVoltage = -1
    var mMeterRotate = -1
    var mSpeed = -1
    var mTemp = -1
    var mTimeOfLastVoltageWarning = 0L
    var mVoltageWarningTimes = 0
    var mTimeOfLastWarnHandbrake = 0L
    var mTimeOfLastWarnSeatbelt = 0L
    var mTimeOfLastWarnTemperature = 0L
    var mTemperatureWarnTimes = 0
    var mGearFunOnoffVisible = 0
    var mGearDLock = 0
    var mGearPUnlock = 0
    var strMCUUUID: String? = null
    var moduleUpgradeMethod = 0
    var carVIN = ""

    private fun int2Byte(i: Int):

            Byte {
        val i2 = when {
            i in 0..9 -> i + 48
            i in 10..15 -> i + 65
            else -> return 0
        }
        return i2.toByte()
    }

    fun sendHeartbeat() {
        mToureCodec.sendHeartbeat()
    }

    fun handleAppInitOK() {
        mToureCodec.sendInit(1)
    }

    fun onBTDisconnected() {
        isTimeSync = false
    }

    fun syncTime() {
        mNaviCodec.sendCurrentTime()
    }


    fun onRecvMsg(bArr: ByteArray, i: Int) {
        when (bArr[0]) {
            1.toByte() -> onRecvHudMsg(bArr)
            3.toByte() -> onRecvCommonMsg(bArr, i)
        }
    }

    private fun onRecvCommonMsg(bArr: ByteArray, i: Int) {
        Log.d(TAG, "recv msg ${bArr.joinToString()}")
        when (bArr[1]) {
            0.toByte() -> {
                if (bArr[2] == 1.toByte()) {
                    mToureCodec.sendConnectECU(0)
                }
            }

            1.toByte() -> getDevType(bArr[2].toInt() and 255)
            2.toByte() -> getOBDProtocolType(bArr[2].toInt() and 255)
            3.toByte() -> when (bArr[2]) {
                0.toByte() -> {
                    mStrVinFirstHalf = ""
                    val bArr2 = ByteArray(9)
                    System.arraycopy(bArr, 3, bArr2, 0, 9)
                    val str = String(bArr2)
                    mStrVinFirstHalf = str
                }

                1.toByte() -> {
                    mStrVinSecondHalf = ""
                    val bArr3 = ByteArray(8)
                    System.arraycopy(bArr, 3, bArr3, 0, 8)
                    val str2 = String(bArr3)
                    mStrVinSecondHalf = str2
                    carVIN = mStrVinFirstHalf + mStrVinSecondHalf
                }
            }

            6.toByte() -> {
                val bit0 = Data.bit0(bArr[2])
                val bit1 = Data.bit1(bArr[2])
                val bit2 = Data.bit2(bArr[2])
                val bit3 = Data.bit3(bArr[2])
                val bit4 = Data.bit4(bArr[2])
                val bit5 = Data.bit5(bArr[2])
                val bit7 = Data.bit7(bArr[2])
                val byteH4 = Data.byteH4(bArr[4])
                val byteL4 = Data.byteL4(bArr[4])
                handleDoorState(bit0, bit1, bit2, bit3, bit5)
                handCarInfo(bit4, bit7, byteH4, byteL4)
            }

            18.toByte() -> {
                val bArr4 = ByteArray(bArr.size - 2)
                System.arraycopy(bArr, 2, bArr4, 0, bArr.size - 2)
                getMonitorErrInfo(bArr4, bArr.size - 2)
            }

            26.toByte() -> getCarConditionData(
                (bArr[2].toInt() and 255 shl 8) + (bArr[3].toInt() and 255),
                (bArr[4].toInt() and 255 shl 8) + (bArr[5].toInt() and 255),
                bArr[6].toInt() and 255,
                bArr[7].toInt() and 255,
                ((bArr[8].toInt() and 255 shl 8) + (bArr[9].toInt() and 255)) / 10 - 40,
                ((bArr[10].toInt() and 255 shl 8) + (bArr[11].toInt() and 255)) / 10 - 40,
                ((bArr[12].toInt() and 255 shl 8) + (bArr[13].toInt() and 255)) / 10 - 40,
                ((bArr[14].toInt() and 255 shl 8) + (bArr[15].toInt() and 255)) / 10 - 40
            )

            33.toByte() -> onCarLockFunctionState(Data.byteH4(bArr[2]), Data.byteL4(bArr[2]))
            else -> when (bArr[1]) {
                8.toByte() -> {
                    val bArr5 = ByteArray(bArr.size - 2)
                    System.arraycopy(bArr, 2, bArr5, 0, bArr.size - 2)
                    handleAllPid(bArr5, bArr.size - 2)
                }

                9.toByte() -> if (i > 5) handlePid(1, bArr[2], bArr[3], bArr[4], bArr[5])
                10.toByte() -> if (i > 5) handlePid(33, bArr[2], bArr[3], bArr[4], bArr[5])
                11.toByte() -> if (i > 5) handlePid(65, bArr[2], bArr[3], bArr[4], bArr[5])
                12.toByte() -> if (i > 5) handlePid(97, bArr[2], bArr[3], bArr[4], bArr[5])
                13.toByte() -> if (i > 5) handlePid(129, bArr[2], bArr[3], bArr[4], bArr[5])
                14.toByte() -> if (i > 5) handlePid(161, bArr[2], bArr[3], bArr[4], bArr[5])
                15.toByte() -> when (bArr[2]) {
                    0.toByte() -> {
                        Log.e("BT:", "!!!!!!!on start fast detect!!!!")
                    }

                    1.toByte() -> {
                        Log.e(" BT ", "!!!!!!!on end fast detect!!!!")
                    }
                }

                16.toByte() -> {
                    if (i < 10) return
                    var mileage = -1
                    if (bArr[2].toInt() != -1 && bArr[3].toInt() != -1) {
                        handleVoltage((bArr[2].toInt() and 255 shl 8) + (bArr[3].toInt() and 255))
                    }
                    handleRotate((bArr[4].toInt() and 255 shl 8) + (bArr[5].toInt() and 255) / 4)
                    handleSpeed(bArr[6].toInt() and 255)
                    handleTemp(bArr[7].toInt() and 255)
                    if (bArr[9].toInt() != -1 || bArr[8].toInt() != -1) {
                        mileage = Data.byte2int(bArr[9], bArr[8])
                    }
                    Log.d("car ", "mileage:$mileage")
                    handleMileage(mileage)
                    if (i > 12) {
                        val remainGas = Data.byte2int(bArr[12])
                        Log.d("car ", "valid remainGas:$remainGas")
                        handleRemainGas(remainGas)
                    }
                }
            }
        }
    }

    fun getDoorState(): IntArray {
        return intArrayOf(mLeftFront, mRightFront, mLeftRear, mRightRear, mTrunk)
    }

    private fun handleDoorState(i: Int, i2: Int, i3: Int, i4: Int, i5: Int) {
        if (mLeftFront == i && mRightFront == i2 && mLeftRear == i3 && mRightRear == i4 && mTrunk == i5) {
            return
        }
        mLeftFront = i
        mRightFront = i2
        mLeftRear = i3
        mRightRear = i4
        mTrunk = i5
        Log.d(TAG, "onDoorState:$i,$i2,$i3,$i4,$i5")
        var str = ""
        var i6 = 0
        if (i == 1) {
            str = "왼쪽 앞문 열림"
            i6 = 1
        }
        if (i2 == 1) {
            i6++
            str = "오른쪽 앞문 열림"
        }
        if (i3 == 1) {
            i6++
            str = "왼쪽 뒷문 열림"
        }
        if (i4 == 1) {
            i6++
            str = "오른쪽 뒷문 열림"
        }
        if (i5 == 1) {
            i6++
            str = "트렁크 열림"
        }
        if (i6 > 0) {
            if (i6 == 1) {
                Log.d("CarEvent", str)
            }
        } else {
            Log.d("CarEvent", "DoorOpen")
        }
    }

    private fun handleRemainGas(i: Int) {
        if (mRemainGas != i) {
            mRemainGas = i
            val str = if (i < 0 || i >= 255) {
                "N/A"
            } else {
                "${(i * 100) / 255}%"
            }
            Log.d("CarEvent", "GasLow")
        }
    }

    private fun handleMileage(i: Int) {
        if (mMileage != i) {
            mMileage = i
            val str = if (i >= 0) {
                "$i Km"
            } else {
                "N/A"
            }
            Log.d("handleMileage", str)
        }
    }

    private fun handCarInfo(i: Int, i2: Int, i3: Int, i4: Int) {
        if (mHandBrake != i) {
            mHandBrake = i
            Log.d(
                "handCarInfo", (if (i == 1) 1
                else 0).toString()
            )
        }
        if (mSeatbelt != i2) {
            mSeatbelt = i2
            Log.d(
                "mSeatbelt", (if (i2 == 0) 1
                else 0).toString()
            )
        }
        if (mGear == i3 && mGearNum == i4) {
            return
        }
        mGear = i3
        mGearNum = i4
        Log.d("meter_gear", makeGearText(i3, i4))
    }

    private fun makeGearText(i: Int, i2: Int): String {
        return when (i) {
            1 -> "P"
            2 -> "R"
            3 -> "N"
            4 -> "D$i2"
            5 -> "S"
            6 -> "M"
            9 -> "L$i2"
            else -> ""
        }
    }

    private fun handleAllPid(bArr: ByteArray, i: Int) {
        val i2 = bArr[0].toInt() and 255
        val pidMapData = mObdData.mSupportPIDMap[i2] ?: return
        if (i == 2) {
            pidMapData.A = if (i2 == 13) {
                (bArr[1].toInt() and 255) *
                        (SpeedAdjustArr[0] and 255.toByte()) / 100
            } else {
                bArr[1].toInt() and 255
            }
        } else if (i == 3) {
            pidMapData.A = if (i2 == 13) {
                (bArr[1].toInt() and 255) * (SpeedAdjustArr[0] and 255.toByte()) / 100
            } else {
                bArr[1].toInt() and 255
            }
            pidMapData.B = bArr[2].toInt() and 255
        } else if (i == 4) {
            pidMapData.A = if (i2 == 13) {
                (bArr[1].toInt() and 255) *
                        (SpeedAdjustArr[0] and 255.toByte()) / 100
            } else {
                bArr[1].toInt() and 255
            }
            pidMapData.B = bArr[2].toInt() and 255
            pidMapData.C = bArr[3].toInt() and 255
        } else if (i == 5) {
            pidMapData.A = if (i2 == 13) {
                (bArr[1].toInt() and 255) *
                        (SpeedAdjustArr[0] and 255.toByte()) / 100
            } else {
                bArr[1].toInt() and 255
            }
            pidMapData.B = bArr[2].toInt() and 255
            pidMapData.C = bArr[3].toInt() and 255
            pidMapData.D = bArr[4].toInt() and 255
        }
    }

    private fun getDevType(i: Int) {
        Log.d("getDevType", "call")
    }

    private fun getMonitorErrInfo(bArr: ByteArray, i: Int) {
        val i2 = bArr[0].toInt() and 255
        if (i2 == 255) {
            Log.d("ing", "clear old err")
            mObdData.getDectectedErrObdList().clear()
        } else if (i2 == 0) {
            Log.d("obd_search_listview_obd", "refresh")
        } else {
            val bArr2 = ByteArray(2)
            for (i3 in 1 until i step 2) {
                val bArr3 = ByteArray(5)
                bArr2[0] = bArr[i3]
                bArr2[1] = bArr[i3 + 1]
                val b = bArr2[0]
                val b2 = bArr2[1]
                val i4 = b.toInt() and 192
                bArr3[0] = when (i4) {
                    0 -> 80
                    64 -> 67
                    128 -> 66
                    192 -> 85
                    else -> 0
                }
                val i5 = b.toInt() and 48
                bArr3[1] = when (i5) {
                    0 -> 48
                    16 -> 49
                    32 -> 50
                    48 -> 51
                    else -> 0
                }
                bArr3[2] = int2Byte(b.toInt() and 15)
                bArr3[3] = int2Byte((b2.toInt() and 240) shr 4)
                bArr3[4] = int2Byte(b2.toInt() and 15)
                val str = String(bArr3)
                if (!mObdData.getDectectedErrObdList().contains(str)) {
                    mObdData.getDectectedErrObdList().add(str)
                }
            }
        }
    }

    private fun getOBDProtocolType(i: Int) {
        val str = when (i) {
            0 -> "NO_TYPE"
            1 -> "J1850_PWM"
            2 -> "J1850_VPW"
            3 -> "ISO9141_2"
            4 -> "ISO14230_5BAUD"
            5 -> "ISO14230_FAST"
            6 -> "ISO15765_500K_11BIT"
            7 -> "ISO15765_500K_29BIT"
            8 -> "ISO15765_250K_11BIT"
            9 -> "ISO15765_250K_29BIT"
            else -> "unknown"
        }
        Log.d("car_obd_type_value", str)
    }

    private fun getCarConditionData(
        i: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int, i8: Int
    ) {
        mErrCount = i
        mVoltage = i2
        mSolarTermDoor = i3
        mWaterTemperature = i4
        mThreeCatalystTemperatureBankOne1 = i5
        mThreeCatalystTemperatureBankOne2 = i6
        mThreeCatalystTemperatureBankTwo1 = i7
        mThreeCatalystTemperatureBankTwo2 = i8
        val d = i2.toDouble()
        Log.d("Voltage", String.format("%.1fV", d / 1000.0))

        Log.d("system_air_intake_value", String.format("%.1f%%", i3 * 100 / 255.0))

        if (i4 > 0 && i4 != 255) {
            Log.d("system_cooling_value", "${i4 - 40}°C")
        } else {
            Log.d("system_cooling_value", "N/A")
        }
        val iArr = intArrayOf(-1, -1, -1, -1)
        if (mObdData.mSupportPIDMap[60]?.support == 1) {
            iArr[0] = i5
        }
        if (mObdData.mSupportPIDMap[61]?.support == 1) {
            iArr[1] = i6
        }
        if (mObdData.mSupportPIDMap[62]?.support == 1) {
            iArr[2] = i7
        }
        if (mObdData.mSupportPIDMap[63]?.support == 1) {
            iArr[3] = i8
        }
        var str = ""
        for (i9 in iArr.indices) {
            if (iArr[i9] != -1) {
                str = String.format(Locale.getDefault(), "%d°C", iArr[i9])
                break
            }
        }
        Log.d("system_memissions_value", str)
        mMemission = str
        if (mErrCount <= 0) {
            Log.d("car_condition_img_result", mErrCount.toString())

        }
    }

    private fun onRecvHudMsg(bArr: ByteArray) {
        if (bArr[1] == 7.toByte()) {
            when (bArr[2]) {
                0.toByte() -> syncTime()
                1.toByte() -> isTimeSync = true
            }
        }
    }

    private fun handlePid(i: Int, b: Byte, b2: Byte, b3: Byte, b4: Byte) {
        mObdData.handlePid(i, b, b2, b3, b4)
    }

    fun handleVoltage(i: Int) {
        if (mMeterVoltage == i) {
            return
        }
        mMeterVoltage = i
        val decimalFormat = DecimalFormat(".0")
        val f = i / 1000.0f
        if (f > 0.0f) {
            if (mRotate > 350 && f < VOLTAGE_RANGE_MIN) {
                onVoltageUnnormal(-1)
            } else if (f > VOLTAGE_RANGE_MAX) {
                onVoltageUnnormal(1)
            } else if (mVoltageWarningTimes > 0) {
                mVoltageWarningTimes = 0
            }
            Log.d("Voltage", "${decimalFormat.format(f)}V")
        } else {
            Log.d("Voltage", "N/A")
        }
    }

    fun onVoltageUnnormal(i: Int) {
        val currentTimeMillis = System.currentTimeMillis()
        val j = mTimeOfLastVoltageWarning
        if (j == 0L || currentTimeMillis - j > 180000) {
            mTimeOfLastVoltageWarning = currentTimeMillis
            mVoltageWarningTimes++
            when (i) {
                1 -> Log.d("onVoltageUnnormal", "배터리 전압 너무 높음")

                -1 -> Log.d("onVoltageUnnormal", "배터리 전압 너무 낮음")
            }
        }
    }

    fun handleRotate(i: Int) {
        if (mMeterRotate == i) {
            return
        }
        mMeterRotate = i
        if (i < 0 || i > 8000) {
            return
        }
        mRotate = i
        Log.d("system_idling_value", "$i r/min")
        Log.d("meter_rotate_value", mRotate.toString())
    }

    fun handleSpeed(i: Int) {
        if (mSpeed == i) {
            return
        }
        mSpeed = i
        if (i < 0 || i > 240) {
            return
        }
        Log.d("meter_speed_meter", "$i")
        if (i >= 1) {
            onCarMoving()
        }
    }

    fun onCarMoving() {
        if (mHandBrake == 1 && (mTimeOfLastWarnHandbrake == 0L || System.currentTimeMillis() - mTimeOfLastWarnHandbrake > 15000)) {
            mTimeOfLastWarnHandbrake = System.currentTimeMillis()
            Log.d("onVoltageUnnormal", "사이드 브레이크 확인!")
        }
        if (mSeatbelt == 0) {
            if (mTimeOfLastWarnSeatbelt == 0L || System.currentTimeMillis() - mTimeOfLastWarnSeatbelt > 30000) {
                mTimeOfLastWarnSeatbelt = System.currentTimeMillis()
                Log.d("onVoltageUnnormal", "안전밸트 확인!")
            }
        }
    }

    fun handleTemp(i: Int) {
        if (mTemp == i) {
            return
        }
        mTemp = i
        if (i >= 0 && i != 255) {
            val i2 = i - 40
            Log.d("meter_temp_value", "$i2℃")
            when {
                i2 < 0 -> onWaterTemperatureUnnormal(-1)
                i2 > TEMPERATURE_RANGE_MAX -> onWaterTemperatureUnnormal(1)
                mTemperatureWarnTimes > 0 -> mTemperatureWarnTimes = 0
            }
        } else {
            Log.d("meter_temp_value", "N/A")
        }
    }

    fun onWaterTemperatureUnnormal(i: Int) {
        val currentTimeMillis = System.currentTimeMillis()
        val j = mTimeOfLastWarnTemperature
        if (j == 0L || currentTimeMillis - j > 180000) {
            mTimeOfLastWarnTemperature = currentTimeMillis

            when (i) {
                -1 -> Log.d("onVoltageUnnormal", "수온 낮음!")
                1 -> Log.d("onVoltageUnnormal", "수온 높음!")
            }
        }
    }

    private fun onCarLockFunctionState(i: Int, i2: Int) {
        Log.d(TAG, "onCarLockFunc:$i,$i2")
        if (mGearFunOnoffVisible == 0) {
            Log.d("gear_d_lock", "변속기 단수 확인!")
            mGearFunOnoffVisible = 1
        }
        if (mGearPUnlock != i) {
            mGearPUnlock = i
            Log.d(
                "gear_d_lock", (if (i == 1) 1
                else 0).toString()
            )

        }
        if (mGearDLock != i2) {
            mGearDLock = i2
            Log.d(
                "gear_d_lock", (if (i2 == 1) 1
                else 0).toString()
            )
        }
    }

    fun onClkGearDLock() {
        mGearDLock = if (mGearDLock == 1) 0
        else 1
        Log.d("gear_d_lock_onoff", "mGearDLock!")
        sendGearFunSet(mGearDLock, mGearPUnlock)
    }

    fun onClkGearPUnlock() {
        mGearPUnlock = if (mGearPUnlock == 1) 0
        else 1
        Log.d("gear_p_unlock_onoff", "mGearPUnlock!")
        sendGearFunSet(mGearDLock, mGearPUnlock)
    }

    fun sendGearFunSet(i: Int, i2: Int) {
        val bArr = byteArrayOf(33, (i and 15 or (i2 and 15 shl 4)).toByte())
        packAndSendMsg(bArr, bArr.size)
    }

    private fun packAndSendMsg(bArr: ByteArray, i: Int) {
        val bArr2 = ByteArray(i + 5)
        var i2 = 0
        bArr2[0] = -1
        bArr2[1] = 85
        var i3 = 2
        bArr2[2] = (i + 1).toByte()
        bArr2[3] = 3
        System.arraycopy(bArr, 0, bArr2, 4, i)
        while (i3 < i + 4) {
            i2 += bArr2[i3].toInt() and 255
            i3++
        }
        bArr2[i + 4] = (i2 and 255).toByte()
        BluetoothModel.sendMessage(bArr2)
    }
}
