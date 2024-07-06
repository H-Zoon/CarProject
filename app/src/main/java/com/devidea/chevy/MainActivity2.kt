package com.devidea.chevy

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.devidea.chevy.bluetooth.BluetoothModel
import com.devidea.chevy.carsystem.CarModel

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetoouth)

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
        // 블루투스 초기화 (선택적)
        BluetoothModel.initBTModel()

        // 연결 버튼
        findViewById<Button>(R.id.connectButton).setOnClickListener {
            connectBluetooth()
        }

        // 해제 버튼
        findViewById<Button>(R.id.disconnectButton).setOnClickListener {
            disconnectBluetooth()
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
    }

    private fun connectBluetooth() {
        BluetoothModel.connectBT() // 블루투스 연결
    }

    private fun disconnectBluetooth() {
        BluetoothModel.disconnectBT() // 블루투스 해제
    }
}
