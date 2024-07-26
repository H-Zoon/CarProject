package com.devidea.chevy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.carsystem.CarModel
import com.devidea.chevy.carsystem.pid.OBDData
import com.devidea.chevy.codec.ToDeviceCodec
import com.devidea.chevy.codec.ToureDevCodec
import com.devidea.chevy.datas.NaviData.AMapTrafficStatus
import com.devidea.chevy.viewmodel.CarViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity2 : AppCompatActivity() {
    @Inject
    lateinit var viewModel: CarViewModel
    /* fun packAndMsg(bArr: ByteArray, i: Int) {
        var i2: Int
        val bArr2 = ByteArray(i + 5)
        bArr2[0] = -1
        bArr2[1] = 85
        var i3 = 2
        bArr2[2] = (i + 1).toByte()
        bArr2[3] = 55
        System.arraycopy(bArr, 0, bArr2, 4, i)
        var i4 = 0
        while (true) {
            i2 = i + 4
            if (i3 >= i2) {
                break
            }
            i4 += bArr2[i3].toInt() and 255
            i3++
        }
        bArr2[i2] = (i4 and 255).toByte()
        BluetoothModel.sendMessage(bArr2)
        val sb = StringBuilder(i)
        val length = bArr.size
        for (i5 in 0 until length) {
            sb.append(String.format("%02X ", bArr[i5]))
        }
        Log.d("mcu_upgrade", "onSend: $sb")
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetoouth)
        val a = CarEventModule(viewModel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("블루투스에 대한 액세스가 필요합니다")
                builder.setMessage("어플리케이션이 블루투스를 감지 할 수 있도록 위치 정보 액세스 권한을 부여하십시오.")
                builder.setPositiveButton(android.R.string.ok, null)

                builder.setOnDismissListener {
                    requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), 2)
                }
                builder.show()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("블루투스에 대한 액세스가 필요합니다")
                builder.setMessage("어플리케이션이 블루투스를 연결 할 수 있도록 위치 정보 액세스 권한을 부여하십시오.")
                builder.setPositiveButton(android.R.string.ok, null)

                builder.setOnDismissListener {
                    requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 3)
                }
                builder.show()
            }
        }

        // 연결 버튼
        findViewById<Button>(R.id.connectButton).setOnClickListener {
            a.handleAllPid(byteArrayOf(1, 0, 7, -27, 0), 5)
            a.handlePid(5, 1, 0, 7, -27)

            a.handleAllPid(byteArrayOf(3, 2, 0), 3)
            a.handlePid(3, 3, 2, 0, 0)

            a.handleAllPid(byteArrayOf(4, 81), 2)
            a.handlePid(2, 4, 81, 0, 0)

            a.handleAllPid(byteArrayOf(5, -128), 2)
            a.handlePid(2, 5, -128, 0, 0)

            a.handleAllPid(byteArrayOf(6, -128), 2)
            a.handlePid(2, 6, -128, 0, 0)

            a.handleAllPid(byteArrayOf(7, 123), 2)
            a.handlePid(2, 7, 123, 0, 0)

            a.handleAllPid(byteArrayOf(10, -114), 2)
            a.handlePid(2, 10, -114, 0, 0)

            a.handleAllPid(byteArrayOf(11, 39), 2)
            a.handlePid(2, 11, 39, 0, 0)

            a.handleAllPid(byteArrayOf(12, 12, -122), 3)
            a.handlePid(3, 12, 12, -122, 0)

            a.handleAllPid(byteArrayOf(13, 0), 2)
            a.handlePid(2, 13, 0, 0, 0)

            a.handleAllPid(byteArrayOf(14, -117), 2)
            a.handlePid(2, 14, -117, 0, 0)

            a.handleAllPid(byteArrayOf(15, 101), 2)
            a.handlePid(2, 15, 101, 0, 0)

            a.handleAllPid(byteArrayOf(16, 1, -126), 3)
            a.handlePid(3, 16, 1, -126, 0)

            a.handleAllPid(byteArrayOf(17, 54), 2)
            a.handlePid(2, 17, 54, 0, 0)

            a.handleAllPid(byteArrayOf(19, 3), 2)
            a.handlePid(2, 19, 3, 0, 0)

            a.handleAllPid(byteArrayOf(21, 126, -1), 3)
            a.handlePid(3, 21, 126, -1, 0)

            a.handleAllPid(byteArrayOf(28, 30), 2)
            a.handlePid(2, 28, 30, 0, 0)

            a.handleAllPid(byteArrayOf(31, 1, -60), 3)
            a.handlePid(3, 31, 1, -60, 0)

            a.handleAllPid(byteArrayOf(33, 0, 0), 3)
            a.handlePid(3, 33, 0, 0, 0)

            a.handleAllPid(byteArrayOf(35, 1, 23), 3)
            a.handlePid(3, 35, 1, 23, 0)

            a.handleAllPid(byteArrayOf(46, 0), 2)
            a.handlePid(2, 46, 0, 0, 0)

            a.handleAllPid(byteArrayOf(47, -4), 2)
            a.handlePid(2, 47, -4, 0, 0)

            a.handleAllPid(byteArrayOf(48, -1), 2)
            a.handlePid(2, 48, -1, 0, 0)

            a.handleAllPid(byteArrayOf(49, 47, -49), 3)
            a.handlePid(3, 49, 47, -49, 0)

            a.handleAllPid(byteArrayOf(50, 0, 6), 3)
            a.handlePid(3, 50, 0, 6, 0)

            a.handleAllPid(byteArrayOf(51, 100), 2)
            a.handlePid(2, 51, 100, 0, 0)

            a.handleAllPid(byteArrayOf(52, -128, -117, 127, -12), 5)
            a.handlePid(5, 52, -128, -117, 127)

            a.handleAllPid(byteArrayOf(60, 22, -18), 3)
            a.handlePid(3, 60, 22, -18, 0)

            a.handleAllPid(byteArrayOf(65, 0, 5, -95, -27), 5)
            a.handlePid(5, 65, 0, 5, -95)

            a.handleAllPid(byteArrayOf(66, 49, 66), 3)
            a.handlePid(3, 66, 49, 66, 0)

            a.handleAllPid(byteArrayOf(67, 0, 61), 3)
            a.handlePid(3, 67, 0, 61, 0)

            a.handleAllPid(byteArrayOf(68, -128, 0), 3)
            a.handlePid(3, 68, -128, 0, 0)

            a.handleAllPid(byteArrayOf(69, 28), 2)
            a.handlePid(2, 69, 28, 0, 0)

            a.handleAllPid(byteArrayOf(70, 76), 2)
            a.handlePid(2, 70, 76, 0, 0)

            a.handleAllPid(byteArrayOf(71, 54), 2)
            a.handlePid(2, 71, 54, 0, 0)

            a.handleAllPid(byteArrayOf(73, 51), 2)
            a.handlePid(2, 73, 51, 0, 0)

            a.handleAllPid(byteArrayOf(74, 26), 2)
            a.handlePid(2, 74, 26, 0, 0)

            a.handleAllPid(byteArrayOf(76, 38), 2)
            a.handlePid(2, 76, 38, 0, 0)

            a.handleAllPid(byteArrayOf(79, 0, 0, 0, 0), 5)
            a.handlePid(5, 79, 0, 0, 0)

            a.handleAllPid(byteArrayOf(81, 1), 2)
            a.handlePid(2, 81, 1, 0, 0)


            a.mObdData.mSupportPIDMap.forEach { (key, value) ->
                println("PID Index: $key, Data: $value")
            }
            a.mObdData.showPid()
            val b = a.mObdData.getPidDataList()
            for (item in b) {
                println("Name: ${item.strName}, Value: ${item.strValue}")
            }
        }
    }
}
