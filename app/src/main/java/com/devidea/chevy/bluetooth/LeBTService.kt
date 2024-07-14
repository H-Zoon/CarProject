package com.devidea.chevy.bluetooth

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devidea.chevy.App
import com.devidea.chevy.MainActivity
import com.devidea.chevy.MainActivity.Companion.CHANNEL_ID
import com.devidea.chevy.R
import java.util.UUID

class LeBTService : Service() {

    var mBleServiceCallback: BleServiceCallback? = null
    val mBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LeBTService = this@LeBTService
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind close.")
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LeBluetoothService")
            .setContentText("Initializing Bluetooth LE Service")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    companion object {
        private const val TAG = "BluetoothLE"
        /*private val SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_NOTIFY_UUID =
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_WRITE_UUID =
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_RW_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
*/

        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
        private val DESCRIPTOR_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb")
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            try {
                val device = result.device
                val name = device.name
                val address = device.address
                Log.i(TAG, "device found name: $name")
                Log.i(TAG, "device found addr: $address")
                if ((name != null && name == BluetoothModel.BT_NAME1) || (name != null && name == BluetoothModel.BT_NAME2)) {
                    Log.i(TAG, "sinjet device found!")
                    bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                    bluetoothGatt =
                        device.connectGatt(App.ApplicationContext(), false, gattCallback)

                    mBleServiceCallback?.onBTStateChange(BTState.CONNECTED)
                    return
                } else {
                    Log.i(TAG, "sinjet device Not found!")
                    mBleServiceCallback?.onBTStateChange(BTState.NOT_FOUND)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server.")
                    bluetoothGatt?.discoverServices()
                    mBleServiceCallback?.onBTStateChange(BTState.CONNECTED)
                }

                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server.")
                    mBleServiceCallback?.onBTStateChange(BTState.DISCONNECTED)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "change MTU succeed = $mtu")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered.")
                /*al service = gatt.getService(SERVICE_UUID)
                val characteristicNotify = service.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
                gatt.setCharacteristicNotification(characteristicNotify, true)*/

                val characteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
                //val read = gatt.getService(SERVICE_UUID)?.getCharacteristic(READ)

                characteristic?.let {
                    gatt.setCharacteristicNotification(it, true)
                    val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    gatt.readCharacteristic(characteristic)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                Log.d(TAG, "Characteristic read: ${data.joinToString()}")
                mBleServiceCallback?.onReceived(data, data.size)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            Log.d(TAG, "Characteristic changed: ${data.joinToString()}")
            mBleServiceCallback?.onReceived(data, data.size)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(value: ByteArray) {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)
        characteristic?.value = value
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    interface BleServiceCallback {
        fun onBTStateChange(state: BTState)
        fun onReceived(data: ByteArray, length: Int)
    }
}