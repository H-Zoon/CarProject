package com.devidea.chevy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devidea.chevy.datas.obd.model.CarEventModel
import com.devidea.chevy.datas.obd.protocol.codec.ToDeviceCodec
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetoouth)
        val a = CarEventModel

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

        // 버튼에 각 함수 연결
        findViewById<Button>(R.id.buttonSendJumpPage).setOnClickListener {
            ToDeviceCodec.sendJumpPage(5)  // 예시로 인덱스 5 전달
        }

        findViewById<Button>(R.id.buttonSendChangeSetting).setOnClickListener {
            ToDeviceCodec.sendChangeSetting(3, 7)  // 예시로 인덱스 3, 7 전달
        }

        findViewById<Button>(R.id.buttonSendAdjustHeight).setOnClickListener {
            ToDeviceCodec.sendAdjustHeight(10)  // 예시로 인덱스 10 전달
        }

        findViewById<Button>(R.id.buttonSendTrafficStatus).setOnClickListener {
            ToDeviceCodec.sendTrafficStatus(byteArrayOf(1, 2, 3))  // 예시로 바이트 배열 전달
        }

        findViewById<Button>(R.id.buttonSendNotification).setOnClickListener {
            ToDeviceCodec.sendNotification(1, "Title", "Message")  // 예시로 인덱스 1, 타이틀, 메시지 전달
        }

        /**
         * 2 = 좌
         * 3 = 우
         * 4 = 11시
         * 5 = 1시
         * 6 = 8시
         * 7 = 5시
         * 8 = u턴
         * 9 = 직진
         * 10 = ?
         * 11 = 회전교차로 직진
         */
        findViewById<Button>(R.id.buttonSendNaviInfo).setOnClickListener {
            /* CoroutineScope(Dispatchers.Main).launch {
                while (true){
                    delay(3000L)*/
            ToDeviceCodec.sendNextInfo(2, 100)  // 예시로 인덱스 1, 거리 1000 전달
            //}
            //}

        }

        findViewById<Button>(R.id.buttonSendCurrentTime).setOnClickListener {
            ToDeviceCodec.sendCurrentTime()  // 현재 시간을 전송
        }

        findViewById<Button>(R.id.buttonSendLineInfo).setOnClickListener {
            //ToDeviceCodec.sendLineInfo(1, 0)  // 예시로 두 개의 인수 전달
        }

        findViewById<Button>(R.id.buttonNotifyIsNaviRunning).setOnClickListener {
            ToDeviceCodec.notifyIsNaviRunning(1)  // 예시로 1 (Byte) 전달
        }

        findViewById<Button>(R.id.buttonSendLimitSpeed).setOnClickListener {
            ToDeviceCodec.sendLimitSpeed(120, 10)  // 예시로 거리 120, 제한속도 80 전달
        }

        findViewById<Button>(R.id.buttonSendCameraDistance).setOnClickListener {
            ToDeviceCodec.sendCameraDistance(120, 10, 2)  // 예시로 거리 300, 속도 40, 60 전달
        }

        findViewById<Button>(R.id.buttonSendCameraDistanceEx).setOnClickListener {
            ToDeviceCodec.sendCameraDistanceEx(1, 1, 1)  // 예시로 거리 300, 속도 50, 70 전달
        }

        /**
         * 1 = 점멸
         * 0 = 점등
         */
        findViewById<Button>(R.id.buttonSendLaneInfo).setOnClickListener {
            ToDeviceCodec.sendLaneInfo(intArrayOf(0, 0, 1, 0))  // 예시로 배열 전달
        }

        findViewById<Button>(R.id.buttonSendLaneInfoEx).setOnClickListener {
            ToDeviceCodec.sendLaneInfoEx(intArrayOf(1, 0, 1, 1))  // 예시로 배열 전달
        }

        findViewById<Button>(R.id.buttonSendLaneInfoExV2).setOnClickListener {
            ToDeviceCodec.sendLaneInfoExV2(intArrayOf(1, 0, 1, 2))  // 예시로 배열 전달
        }

        findViewById<Button>(R.id.buttonSendNextRoadName).setOnClickListener {
            ToDeviceCodec.sendNextRoadName("Main Street")  // 예시로 도로명 전달
        }

        /*        fun hendle() {
            a.handleAllPid(byteArrayOf(1, 0, 7, -27, 0), 5)
            a.handleAllPid(byteArrayOf(3, 2, 0), 3)
            a.handleAllPid(byteArrayOf(4, 81), 2)
            a.handleAllPid(byteArrayOf(5, -128), 2)
            a.handleAllPid(byteArrayOf(6, -128), 2)
            a.handleAllPid(byteArrayOf(7, 123), 2)
            a.handleAllPid(byteArrayOf(10, -114), 2)
            a.handleAllPid(byteArrayOf(11, 39), 2)
            a.handleAllPid(byteArrayOf(12, 12, -122), 3)
            a.handleAllPid(byteArrayOf(13, 0), 2)
            a.handleAllPid(byteArrayOf(14, -117), 2)
            a.handleAllPid(byteArrayOf(15, 101), 2)
            a.handleAllPid(byteArrayOf(16, 1, -126), 3)
            a.handleAllPid(byteArrayOf(17, 54), 2)
            a.handleAllPid(byteArrayOf(19, 3), 2)
            a.handleAllPid(byteArrayOf(21, 126, -1), 3)
            a.handleAllPid(byteArrayOf(28, 30), 2)
            a.handleAllPid(byteArrayOf(31, 1, -60), 3)
            a.handleAllPid(byteArrayOf(33, 0, 0), 3)
            a.handleAllPid(byteArrayOf(35, 1, 23), 3)
            a.handleAllPid(byteArrayOf(46, 0), 2)
            a.handleAllPid(byteArrayOf(47, -4), 2)
            a.handleAllPid(byteArrayOf(48, -1), 2)
            a.handleAllPid(byteArrayOf(49, 47, -49), 3)
            a.handleAllPid(byteArrayOf(50, 0, 6), 3)
            a.handleAllPid(byteArrayOf(51, 100), 2)
            a.handleAllPid(byteArrayOf(52, -128, -117, 127, -12), 5)
            a.handleAllPid(byteArrayOf(60, 22, -18), 3)
            a.handleAllPid(byteArrayOf(65, 0, 5, -95, -27), 5)
            a.handleAllPid(byteArrayOf(66, 49, 66), 3)
            a.handleAllPid(byteArrayOf(67, 0, 61), 3)
            a.handleAllPid(byteArrayOf(68, -128, 0), 3)
            a.handleAllPid(byteArrayOf(69, 28), 2)
            a.handleAllPid(byteArrayOf(70, 76), 2)
            a.handleAllPid(byteArrayOf(71, 54), 2)
            a.handleAllPid(byteArrayOf(73, 51), 2)
            a.handleAllPid(byteArrayOf(74, 26), 2)
            a.handleAllPid(byteArrayOf(76, 38), 2)
            a.handleAllPid(byteArrayOf(79, 0, 0, 0, 0), 5)
            a.handleAllPid(byteArrayOf(81, 1), 2)

            a.handlePid(1, 155.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
            a.handlePid(33, 214.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
            a.handlePid(65, 241.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
            a.handlePid(97, 55.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
            a.handlePid(129, 180.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
            a.handlePid(161, 171.toByte(), 0.toByte(), 0.toByte(), 0.toByte())

            a.mObdData.mSupportPIDMap.forEach { (key, value) ->
                println("PID Index: $key, Data: $value")
            }
            a.mObdData.showPid()
            val b = a.mObdData.getPidDataList()
            for (item in b) {
                println("Name: ${item.strName}, Value: ${item.strValue}")
            }
        }
    */
    }
}