package com.devidea.chevy.carsystem;

import com.devidea.chevy.viewmodel.CarViewModel
import android.util.Log
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.codec.ToDeviceCodec
import com.devidea.chevy.codec.ToureDevCodec
import com.devidea.chevy.datas.Data
import java.text.DecimalFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.experimental.and

class CarEventModule @Inject constructor(private val viewModel: CarViewModel) {

    companion object {
        const val MSG_NO_INFO_TIMEOUT = 100
        const val TEMPERATURE_RANGE_MAX = 112
        const val TEMPERATURE_RANGE_MIN = 0
        const val VOLTAGE_RANGE_MAX = 15.0f
        const val VOLTAGE_RANGE_MIN = 10.0f
        private const val TAG = "CarEventModule"
    }

    private var mObdData: OBDData = OBDData()
    private var isTimeSync = false

    private var mStrVinFirstHalf = ""
    private var mStrVinSecondHalf = ""

    sealed class DoorState {
        data object Closed : DoorState()
        data object Open : DoorState()
    }

    var SpeedAdjustArr = byteArrayOf(100, 102, 104, 106, 108, 110, 111, 112, 113, 114)

    private fun int2Byte(i: Int): Byte {
        val i2 = when {
            i in 0..9 -> i + 48
            i in 10..15 -> i + 65
            else -> return 0
        }
        return i2.toByte()
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
                    ToureDevCodec.sendConnectECU(0)
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
                    viewModel.updateCarVIN(mStrVinFirstHalf + mStrVinSecondHalf)
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
                        viewModel.updateObdData(mObdData)
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
                    Log.d(TAG, "mileage:$mileage")
                    handleMileage(mileage)
                    if (i > 12) {
                        val remainGas = Data.byte2int(bArr[12])
                        Log.d(TAG, "valid remainGas:$remainGas")
                        handleRemainGas(remainGas)
                    }
                }
            }
        }
    }


    private fun handleDoorState(
        leftFront: Int,
        rightFront: Int,
        leftRear: Int,
        rightRear: Int,
        trunk: Int
    ) {
        viewModel.updateLeftFront(if (leftFront == 1) DoorState.Open else DoorState.Closed)
        viewModel.updateRightFront(if (rightFront == 1) DoorState.Open else DoorState.Closed)
        viewModel.updateLeftRear(if (leftRear == 1) DoorState.Open else DoorState.Closed)
        viewModel.updateRightRear(if (rightRear == 1) DoorState.Open else DoorState.Closed)
        viewModel.updateTrunk(if (trunk == 1) DoorState.Open else DoorState.Closed)
    }

    private fun handleRemainGas(i: Int) {
        viewModel.updateRemainGas(if (i < 0 || i >= 255) "N/A" else "${(i * 100) / 255}%")
    }

    private fun handleMileage(i: Int) {
        viewModel.updateMileage(if (i >= 0) "$i Km" else "N/A")
    }

    private fun handCarInfo(parkingBrake: Int, seatbelt: Int, gear: Int, gearNum: Int) {
        viewModel.updateHandBrake(parkingBrake == 1)
        viewModel.updateSeatbelt(seatbelt == 1)
        viewModel.updateGear(makeGearText(gear, gearNum))
        viewModel.updateGearNum(gearNum)
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
        when (i) {
            2 -> {
                pidMapData.A = if (i2 == 13) {
                    (bArr[1].toInt() and 255) *
                            (SpeedAdjustArr[0] and 255.toByte()) / 100
                } else {
                    bArr[1].toInt() and 255
                }
            }
            3 -> {
                pidMapData.A = if (i2 == 13) {
                    (bArr[1].toInt() and 255) * (SpeedAdjustArr[0] and 255.toByte()) / 100
                } else {
                    bArr[1].toInt() and 255
                }
                pidMapData.B = bArr[2].toInt() and 255
            }
            4 -> {
                pidMapData.A = if (i2 == 13) {
                    (bArr[1].toInt() and 255) *
                            (SpeedAdjustArr[0] and 255.toByte()) / 100
                } else {
                    bArr[1].toInt() and 255
                }
                pidMapData.B = bArr[2].toInt() and 255
                pidMapData.C = bArr[3].toInt() and 255
            }
            5 -> {
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
        mObdData.mSupportPIDMap[i2] = pidMapData
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
        errCount: Int,
        voltage: Int,
        solarTermDoor: Int,
        waterTemperature: Int,
        bankOne1: Int,
        bankOne2: Int,
        bankTwo1: Int,
        bankTwo2: Int
    ) {
        viewModel.updateErrCount(errCount)
        viewModel.updateVoltage(String.format("%.1fV", voltage.toDouble() / 1000.0))
        viewModel.updateSolarTermDoor(String.format("%.1f%%", solarTermDoor * 100 / 255.0))
        viewModel.updateWaterTemperature(
            if (waterTemperature > 0 && waterTemperature != 255) ("${waterTemperature - 40}°C") else ("N/A")
        )

        if (mObdData.mSupportPIDMap[60]?.support == 1) {
            viewModel.updateThreeCatalystTemperatureBankOne1(String.format(Locale.getDefault(), "%d°C", bankOne1))
        }
        if (mObdData.mSupportPIDMap[61]?.support == 1) {
            viewModel.updateThreeCatalystTemperatureBankOne2(String.format(Locale.getDefault(), "%d°C", bankOne2))
        }
        if (mObdData.mSupportPIDMap[62]?.support == 1) {
            viewModel.updateThreeCatalystTemperatureBankTwo1(String.format(Locale.getDefault(), "%d°C", bankTwo1))
        }
        if (mObdData.mSupportPIDMap[63]?.support == 1) {
            viewModel.updateThreeCatalystTemperatureBankTwo2(String.format(Locale.getDefault(), "%d°C", bankTwo2))
        }
    }

    private fun onRecvHudMsg(bArr: ByteArray) {
        if (bArr[1] == 7.toByte()) {
            when (bArr[2]) {
                0.toByte() -> ToDeviceCodec.sendCurrentTime()
                1.toByte() -> isTimeSync = true
            }
        }
    }

    private fun handlePid(i: Int, b: Byte, b2: Byte, b3: Byte, b4: Byte) {
        mObdData.handlePid(i, b, b2, b3, b4)
    }

    private fun handleVoltage(i: Int) {
        val decimalFormat = DecimalFormat(".0")
        val f = i / 1000.0f
        if (f > 0.0f) {
            if (viewModel.mRotate.value > 350 && f < VOLTAGE_RANGE_MIN) {
                Log.d("onVoltageUnnormal", "배터리 전압 너무 높음")
            } else if (f > VOLTAGE_RANGE_MAX) {
                Log.d("onVoltageUnnormal", "배터리 전압 너무 낮음")
            }

            viewModel.updateMeterVoltage("${decimalFormat.format(f)}V")
        } else {
            viewModel.updateMeterVoltage("N/A")
        }
    }

    private fun handleRotate(rotate: Int) {
        viewModel.updateRotate(rotate)
    }

    private fun handleSpeed(speed: Int) {
        viewModel.updateSpeed(speed)
    }


    private fun handleTemp(temp: Int) {
        val adjustedTemp = temp - 40
        viewModel.updateTemp(adjustedTemp)
        if (temp >= 0 && temp != 255) {
            when {
                adjustedTemp < 0 -> Log.d("onVoltageUnnormal", "수온 낮음!")
                adjustedTemp > TEMPERATURE_RANGE_MAX -> Log.d("onVoltageUnnormal", "수온 높음!")
            }
        } else {
            Log.d("meter_temp_value", "N/A")
        }
    }

    private fun onCarLockFunctionState(i: Int, i2: Int) {
        Log.d(TAG, "onCarLockFunc:$i,$i2")
        if (viewModel.mGearFunOnoffVisible.value!! == 0) {
            Log.d("gear_d_lock", "변속기 단수 확인!")
            viewModel.updateGearFunOnoffVisible(1)
        }
        if (viewModel.mGearPUnlock.value != i) {
            viewModel.updateGearPUnlock(i)
            Log.d(
                "gear_d_lock", (if (i == 1) 1
                else 0).toString()
            )
        }
        if (viewModel.mGearDLock.value != i2) {
            viewModel.updateGearDLock(i2)
            Log.d(
                "gear_d_lock", (if (i2 == 1) 1
                else 0).toString()
            )
        }
    }

    fun onClkGearDLock() {
        val newGearDLock = if (viewModel.mGearDLock.value == 1) 0 else 1
        viewModel.updateGearDLock(newGearDLock)
        Log.d("gear_d_lock_onoff", "mGearDLock!")
        sendGearFunSet(newGearDLock, viewModel.mGearPUnlock.value!!)
    }

    fun onClkGearPUnlock() {
        val newGearPUnlock = if (viewModel.mGearPUnlock.value == 1) 0 else 1
        viewModel.updateGearPUnlock(newGearPUnlock)
        Log.d("gear_p_unlock_onoff", "mGearPUnlock!")
        sendGearFunSet(viewModel.mGearDLock.value!!, newGearPUnlock)
    }

    private fun sendGearFunSet(i: Int, i2: Int) {
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
