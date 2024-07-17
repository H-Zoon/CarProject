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
import com.devidea.chevy.carsystem.CarModel
import com.devidea.chevy.carsystem.ControlFuncs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity2 : AppCompatActivity() {

    fun packAndMsg(bArr: ByteArray, i: Int) {
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetoouth)

        val serviceChannel = NotificationChannel(
            MainActivity.CHANNEL_ID,
            "LeBluetoothService Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

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

        BluetoothModel.initBTModel(this)

        // 연결 버튼
        findViewById<Button>(R.id.connectButton).setOnClickListener {
            BluetoothModel.connectBT()
        }

        // 해제 버튼
        findViewById<Button>(R.id.disconnectButton).setOnClickListener {
            BluetoothModel.disconnectBT()
        }

        findViewById<Button>(R.id.init).setOnClickListener {
            CarModel.carEventModule.handleAppInitOK()
        }

        findViewById<Button>(R.id.time).setOnClickListener {
            CarModel.carEventModule.syncTime()
        }

        findViewById<Button>(R.id.sub).setOnClickListener {
            val bArr = byteArrayOf(0x10, 0x00)
            val i = 2 // bArr의 길이
            packAndMsg(bArr, i)
        }

        findViewById<Button>(R.id.sub2).setOnClickListener {
            val bArr = byteArrayOf(0x12)
            val i = 1 // bArr의 길이
            packAndMsg(bArr, i)
        }

        findViewById<Button>(R.id.heart).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                CarModel.carEventModule.sendHeartbeat()
            }
        }


        findViewById<Button>(R.id.t0).setOnClickListener {
            CarModel.tpmsModule.sendPairTpms(0)
        }

        findViewById<Button>(R.id.t1).setOnClickListener {
            CarModel.tpmsModule.sendPairTpms(1)
        }

        findViewById<Button>(R.id.t2).setOnClickListener {
            CarModel.tpmsModule.sendPairTpms(2)
        }

        findViewById<Button>(R.id.t3).setOnClickListener {
            CarModel.tpmsModule.sendPairTpms(3)
        }
        findViewById<Button>(R.id.t_open).setOnClickListener {
            CarModel.controlModule.sendControlMessage(conMsg = ControlFuncs.CAR_TRUNK_OPEN)
        }



    }
}
