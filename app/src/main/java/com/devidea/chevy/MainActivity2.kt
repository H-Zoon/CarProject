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
import com.devidea.chevy.codec.ToDeviceCodec
import com.devidea.chevy.codec.ToureDevCodec
import com.devidea.chevy.datas.NaviData.AMapTrafficStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
            ToureDevCodec.sendInit(1)
        }

        findViewById<Button>(R.id.time).setOnClickListener {
            ToDeviceCodec.sendCurrentTime()
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
                while (true) {
                    ToureDevCodec.sendHeartbeat()
                    delay(2000L)
                }
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
            //CarModel.controlModule.sendControlMessage(conMsg = ControlFuncs.CAR_TRUNK_OPEN)
            CarModel.controlModule.makeHUDSetMsgAndSend()
        }

        fun createTrafficStatus(length: Int, index: Int, status: Int): AMapTrafficStatus {
            val trafficStatus = AMapTrafficStatus()
            trafficStatus.length = length
            trafficStatus.linkIndex = index
            trafficStatus.status = status
            return trafficStatus
        }

        fun handleTrafficStatusUpdate(trafficStatuses : MutableList<AMapTrafficStatus>) {
            Log.i("traffic", "trafficStatusList len:" + trafficStatuses.size)
            val bArr = ByteArray(trafficStatuses.size * 4)
            var i = 0
            var str = "status content:"
            for (aMapTrafficStatus in trafficStatuses) {
                str = str + (("""
 len:${aMapTrafficStatus.length}""".toString() + " index:" + aMapTrafficStatus.linkIndex).toString() + " status:" + aMapTrafficStatus.status)
                val i2 = i * 4
                bArr[i2] = aMapTrafficStatus.status.toByte()
                val length: Int = aMapTrafficStatus.length
                bArr[i2 + 1] = (length and 255).toByte()
                bArr[i2 + 2] = ((65280 and length) shr 8).toByte()
                bArr[i2 + 3] = ((length and 16711680) shr 16).toByte()
                i++
            }
            Log.i("traffic", str)
            //this.trafficData = bArr
            ToDeviceCodec.sendTrafficStatus(bArr)
        }

        findViewById<Button>(R.id.navi).setOnClickListener {
            ToDeviceCodec.sendnaviInfo(1,2)
            /*val trafficStatuses: MutableList<AMapTrafficStatus> = ArrayList()
            trafficStatuses.add(createTrafficStatus(30612, 0, 1))
            trafficStatuses.add(createTrafficStatus(247, 179, 2))
            trafficStatuses.add(createTrafficStatus(4386, 180, 1))
            trafficStatuses.add(createTrafficStatus(282, 214, 0))
            trafficStatuses.add(createTrafficStatus(24, 215, 2))
            trafficStatuses.add(createTrafficStatus(614743, 216, 1))
            trafficStatuses.add(createTrafficStatus(307, 1277, 2))
            trafficStatuses.add(createTrafficStatus(62125, 1279, 1))
            trafficStatuses.add(createTrafficStatus(482, 1406, 4))
            trafficStatuses.add(createTrafficStatus(966, 1407, 3))
            trafficStatuses.add(createTrafficStatus(20927, 1408, 1))
            trafficStatuses.add(createTrafficStatus(479, 1447, 3))
            trafficStatuses.add(createTrafficStatus(243683, 1448, 1))
            trafficStatuses.add(createTrafficStatus(2304, 1724, 2))
            trafficStatuses.add(createTrafficStatus(782862, 1726, 1))
            trafficStatuses.add(createTrafficStatus(389, 2517, 2))
            trafficStatuses.add(createTrafficStatus(5286, 2521, 1))
            trafficStatuses.add(createTrafficStatus(208, 2528, 2))
            trafficStatuses.add(createTrafficStatus(103, 2532, 3))
            trafficStatuses.add(createTrafficStatus(201, 2534, 2))
            trafficStatuses.add(createTrafficStatus(285, 2539, 3))
            trafficStatuses.add(createTrafficStatus(85, 2546, 2))
            trafficStatuses.add(createTrafficStatus(50433, 2548, 1))
            trafficStatuses.add(createTrafficStatus(56, 2758, 0))
            handleTrafficStatusUpdate(trafficStatuses)*/
        }

    }
}
