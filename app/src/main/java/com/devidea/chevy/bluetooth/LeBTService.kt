package com.devidea.chevy.bluetooth

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devidea.chevy.App
import com.devidea.chevy.App.Companion.CHANNEL_ID
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID


class LeBTService: Service() {

    var mBleServiceCallback: BleServiceCallback? = null
    val mBinder = LocalBinder()
    private val scanTimeout = 10000L // 10 seconds timeout
    private var scanJob: Job? = null

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

    internal fun updateNotification(state: BTState) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stateText = state.description

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LeBluetoothService")
            .setContentText("Bluetooth State: $stateText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
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
        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_NOTIFY_UUID =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_WRITE_UUID =
            UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
        private val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        mBleServiceCallback?.onBTStateChange(BTState.SCANNING)
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            delay(scanTimeout)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        scanJob?.cancel()
        mBleServiceCallback?.onBTStateChange(BTState.NOT_FOUND)
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
                    scanJob?.cancel()
                    bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                    bluetoothGatt =
                        device.connectGatt(App.ApplicationContext(), false, gattCallback)

                    mBleServiceCallback?.onBTStateChange(BTState.CONNECTING)
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        mBleServiceCallback?.onBTStateChange(BTState.DISCONNECTED)
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
                val service = gatt.getService(SERVICE_UUID)

                val notifyCharacteristic = service?.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
                val writeCharacteristic = service?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

                if (notifyCharacteristic != null && writeCharacteristic != null) {
                    setCharacteristicNotification(gatt, notifyCharacteristic)
                    setCharacteristicNotification(gatt, writeCharacteristic)
                } else {
                    Log.e(TAG, "onServicesDiscovered: Not Found characteristics")
                }
            } else {
                Log.e(TAG, "onServicesDiscovered: Failed with status $status")
            }
        }

        @SuppressLint("MissingPermission")
        @Suppress("DEPRECATION")
        private fun setCharacteristicNotification(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID)
            descriptor?.let {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                } else {
                    gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                }
                gatt.readCharacteristic(characteristic)
            } ?: Log.e(
                TAG,
                "setCharacteristicNotification: Descriptor not found for ${characteristic.uuid}"
            )
        }

        /**
         * DOC : https://stackoverflow.com/questions/73438580/new-oncharacteristicread-method-not-working
         */

        @Suppress("DEPRECATION")
        @Deprecated(
            "Used natively in Android 12 and lower",
            ReplaceWith("onCharacteristicRead(gatt, characteristic, characteristic.value, status)")
        )
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) = onCharacteristicRead(gatt, characteristic, characteristic.value, status)

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read: ${value.joinToString()}")
                mBleServiceCallback?.onReceived(value, value.size)
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated(
            "Used natively in Android 12 and lower",
            ReplaceWith("onCharacteristicChanged(gatt, characteristic, characteristic.value)")
        )
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) = onCharacteristicChanged(gatt, characteristic, characteristic.value)

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            //Log.d(TAG, "Characteristic changed: ${value.joinToString()}")
            mBleServiceCallback?.onReceived(value, value.size)
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
    @Suppress("DEPRECATION")
    @Deprecated(
        "Used natively in Android 12 and lower",
        ReplaceWith("bluetoothGatt.writeCharacteristic(characteristic, characteristic.value, characteristic.size)")
    )
    fun sendMessage(value: ByteArray) {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

        if (characteristic != null) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                characteristic.value = value
                bluetoothGatt?.writeCharacteristic(characteristic)
            } else {

                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    value,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            }
        }
    }

    interface BleServiceCallback {
        fun onBTStateChange(state: BTState)
        fun onReceived(data: ByteArray, length: Int)
    }
}