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
import androidx.core.app.NotificationCompat
import com.devidea.chevy.App
import com.devidea.chevy.App.Companion.CHANNEL_ID
import com.devidea.chevy.Logger
import com.devidea.chevy.ui.activity.MainActivity
import com.devidea.chevy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID


class LeBTService : Service() {

    companion object {
        private const val BT_NAME1 = "Sinjet"
        private const val BT_NAME2 = "ChanganMazda"
        private const val TAG = "BluetoothLE"
        private const val MAX_SERVICE_DISCOVERY_RETRIES = 3
        private const val MAX_MTU_REQUEST_RETRIES = 3
        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
        private val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    var mBleServiceCallback: BleServiceCallback? = null
    val mBinder = LocalBinder()

    private var initMTU = 185
    private val scanTimeout = 10000L // 10 seconds timeout
    private var scanJob: Job? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // 재시도 카운터
    private var serviceDiscoveryRetryCount = 0
    private var mtuRequestRetryCount = 0

    inner class LocalBinder : Binder() {
        fun getService(): LeBTService = this@LeBTService
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.i { "onUnbind close." }
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onDestroy() {
        Logger.d { "onDestroy" }
        disconnect()
        stopScan()
        mBleServiceCallback = null
        serviceScope.cancel() // 이전에 제안된 코루틴 관리 개선사항 반영
        super.onDestroy()
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

    fun discoverServices() {
        bluetoothGatt?.discoverServices()
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

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    fun startScan() {
        mBleServiceCallback?.onBTStateChange(BTState.SCANNING)
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        scanJob = serviceScope.launch {
            delay(scanTimeout)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        serviceScope.cancel()
        mBleServiceCallback?.onBTStateChange(BTState.NOT_FOUND)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        mBleServiceCallback?.onBTStateChange(BTState.DISCONNECTED)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            try {
                val device = result.device
                val name = device.name
                val address = device.address
                Logger.i { "device found name: $name" }
                Logger.i { "device found addr: $address" }
                if ((name != null && name == BT_NAME1) || (name != null && name == BT_NAME2)) {
                    Logger.i { "sinjet device found!" }
                    serviceScope.cancel()
                    bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                    bluetoothGatt =
                        device.connectGatt(App.ApplicationContext(), false, gattCallback)

                    mBleServiceCallback?.onBTStateChange(BTState.CONNECTING)
                    return
                }
            } catch (e: Exception) {
                Logger.e { e.toString() }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Logger.d { "Connected to GATT server." }
                    //bluetoothGatt?.discoverServices()
                    mBleServiceCallback?.onBTStateChange(BTState.CONNECTED)
                }

                BluetoothGatt.STATE_DISCONNECTED -> {
                    Logger.d { "Disconnected from GATT server." }
                    mBleServiceCallback?.onBTStateChange(BTState.DISCONNECTED)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d { "MTU successfully changed to $mtu" }
                mBleServiceCallback?.onMtuChanged(mtu)
            } else {
                Logger.e { "MTU change failed with status $status" }
                requestMtuWithRetry(initMTU)
                // MTU 변경 실패 시 추가 처리 (예: 재시도)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d { "Services discovered." }
                val service = gatt.getService(SERVICE_UUID)

                val notifyCharacteristic = service?.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
                val writeCharacteristic = service?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

                if (notifyCharacteristic != null && writeCharacteristic != null) {
                    setCharacteristicNotification(gatt, notifyCharacteristic)
                    //setCharacteristicNotification(gatt, writeCharacteristic)

                    requestMtuWithRetry(initMTU)
                } else {
                    Logger.e { "onServicesDiscovered: Not Found characteristics" }
                    handleServiceDiscoveryFailure(status)
                }
            } else {
                Logger.e { "onServicesDiscovered: Failed with status $status" }
                handleServiceDiscoveryFailure(status)
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
            } ?: Logger.e {
                "setCharacteristicNotification: Descriptor not found for ${characteristic.uuid}"
            }
        }

        // 서비스 검색 실패 시 처리 메서드
        private fun handleServiceDiscoveryFailure(status: Int) {
            if (serviceDiscoveryRetryCount < MAX_SERVICE_DISCOVERY_RETRIES) {
                serviceDiscoveryRetryCount++
                Logger.d { "Retrying service discovery... Attempt $serviceDiscoveryRetryCount" }
                discoverServices()
            } else {
                Logger.e { "Service discovery failed after $MAX_SERVICE_DISCOVERY_RETRIES attempts." }
                disconnect()
            }
        }

        // MTU 협상 요청 및 재시도 로직
        private fun requestMtuWithRetry(desiredMtu: Int) {
            if (mtuRequestRetryCount < MAX_MTU_REQUEST_RETRIES) {
                mtuRequestRetryCount++
                val newMTU = desiredMtu - mtuRequestRetryCount * 2
                bluetoothGatt?.requestMtu(newMTU)
            } else {
                Logger.e { "MTU request failed after $MAX_MTU_REQUEST_RETRIES attempts." }
                mtuRequestRetryCount = 0
                disconnect()
                return
            }
        }

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
                Logger.d { "Characteristic read: ${value.joinToString()}" }
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
                mBleServiceCallback?.onMessageSent()
                Logger.d { "Characteristic written successfully" }
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
        fun onMtuChanged(mtu: Int)
        fun onMessageSent()
    }
}