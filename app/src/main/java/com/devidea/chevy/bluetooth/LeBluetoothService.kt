/*
package com.devidea.chevy.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.content.Intent
import android.os.*
import android.util.Log
import kotlinx.coroutines.*
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.devidea.chevy.MainActivity
import com.devidea.chevy.MainActivity.Companion.CHANNEL_ID
import com.devidea.chevy.MainActivity2
import com.devidea.chevy.R
import com.devidea.chevy.carsystem.CarModel
import kotlinx.coroutines.channels.Channel
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

class LeBluetoothService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val messageChannel = Channel<ByteArray>(Channel.UNLIMITED)

    private val TAG = "LeBluetoothService"
    private val mBinder = LocalBinder()
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothManager: BluetoothManager? = null
    private var mHaveTlDevice = false
    private var mConnectionState = BTState.DISCONNECTED
    private var mIsConnectingPeriod = false
    private var mBleServiceCallback: BleServiceCallback? = null
    private var scanFilterList: List<ScanFilter>? = null
    private var scanSettingBuilder: ScanSettings.Builder? = null

    private fun hasPermissions(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val BT_CONNECT_TIMEOUT = 20000
        const val BT_SCAN_PERIOD = 15000
        const val CH_CHARACTERISTIC_NOTIFY = "0000ffe1-0000-1000-8000-00805f9b34fb"
        const val CH_CHARACTERISTIC_WRITE = "0000ffe2-0000-1000-8000-00805f9b34fb"
        const val HC08_SERVICE_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
        const val TOURE_CHARACTERISTIC_NOTIFY = "0000ff01-0000-1000-8000-00805f9b34fb"
        const val TOURE_CHARACTERISTIC_WRITE = "0000ff02-0000-1000-8000-00805f9b34fb"
        val UUID_HEART_RATE_MEASUREMENT: UUID =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)
        private var target_ch_write: BluetoothGattCharacteristic? = null
        private var target_ch_notify: BluetoothGattCharacteristic? = null
        private var target_toure_write: BluetoothGattCharacteristic? = null
        private var target_toure_notify: BluetoothGattCharacteristic? = null
        private var target_HC08: BluetoothGattCharacteristic? = null
        private var target_HEART_RATE: BluetoothGattCharacteristic? = null
    }

    init {
        scope.launch {
            for (message in messageChannel) {
                sendMessageInternal(message)
                delay(1000)
            }
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(
                TAG,
                "onConnectionStateChange: ${BTState.fromState(status)} -> ${
                    BTState.fromState(newState)
                }"
            )
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> handleGattConnected(gatt)
                BluetoothProfile.STATE_DISCONNECTED -> handleGattDisconnected(gatt)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "change MTU succeed = $mtu")
                BluetoothModel.mBleUsingMtu = mtu
                if (gatt.discoverServices()) {
                    Log.i(TAG, "Attempting to start service discovery")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "onServicesDiscovered: $status")
            if (status != BluetoothGatt.GATT_SUCCESS && mIsConnectingPeriod) {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        initialize()
                        connect(mBluetoothDevice)
                    }
                }, 2000)
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                findGattServicesTarget(getSupportedGattServices())
                onBTStateChange(BTState.CONNECTED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
                runBlocking {
                    delay(1000)
                    gatt.discoverServices()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.i(TAG, "onCharacteristicRead: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onRecvCharacteristic(characteristic)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onDescriptorWrite failed")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i(TAG, "onCharacteristicChanged from: ")
            onRecvCharacteristic(characteristic)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Log.d(TAG, "rssi = $rssi")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onCharacteristicWrite failed")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleGattConnected(gatt: BluetoothGatt) {
        Log.i(TAG, "Connected to GATT server.")
        if (hasPermissions(Manifest.permission.BLUETOOTH_CONNECT)) {
            val requestMtu = gatt.requestMtu(185)
            Log.d(TAG, "requestMtu to 185 result: $requestMtu")
            if (requestMtu || !gatt.discoverServices()) {
                Log.d(TAG, "Attempting to start service discovery")
            }
        }
    }

    private fun handleGattDisconnected(gatt: BluetoothGatt) {
        onBTStateChange(BTState.DISCONNECTED)
        if (mIsConnectingPeriod) {
            reconnect()
        }
    }

    private fun reconnect() {
        mBluetoothDevice?.let {
            scope.launch {
                delay(1000) // 재연결 시도 전 잠시 대기
                if (mConnectionState != BTState.CONNECTED) {
                    connect(it)
                }
            }
        } ?: run {
            Log.e(TAG, "Bluetooth device is null. Cannot reconnect.")
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): LeBluetoothService = this@LeBluetoothService
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind close.")
        scanLeDevice(false)
        disconnectBTGatt()
        closeBTGatt()
        return super.onUnbind(intent)
    }

    @SuppressLint("MissingPermission")
    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        }
        val bluetoothManager = mBluetoothManager ?: return false
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = bluetoothManager.adapter
        }
        return mBluetoothAdapter != null
    }

    @SuppressLint("MissingPermission")
    fun connect(bluetoothDevice: BluetoothDevice?): Boolean {
        bluetoothDevice ?: return false
        closeBTGatt()
        Log.d(TAG, "Trying a connection with BT addr: ${bluetoothDevice.address}")
        mBluetoothGatt = bluetoothDevice.connectGatt(this, false, mGattCallback)
        mBluetoothGatt ?: return false
        onBTStateChange(BTState.CONNECTED)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun disconnectBTGatt() {
        mBluetoothGatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    private fun closeBTGatt() {
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    fun setBleServiceCallback(callback: BleServiceCallback) {
        mBleServiceCallback = callback
    }

    fun isInConnecttingPeriod(): Boolean {
        return mIsConnectingPeriod
    }

    // 메시지를 큐에 추가하는 함수
    @SuppressLint("MissingPermission")
    fun sendMessage(data: ByteArray) {
        scope.launch {
            messageChannel.send(data)
        }
    }

    // 실제로 메시지를 전송하는 함수
    @SuppressLint("MissingPermission")
    private fun sendMessageInternal(data: ByteArray) {
        if (isBTConnected()) {
            if (mBluetoothGatt == null || target_ch_write == null) {
                Log.d(TAG, "send message but BluetoothGatt or target_chara == null")
                return
            } else {
                val byteArray = byteArrayOf(
                    -1, 85, 3, 3, 14, 0, 20
                )
                val newData = ByteArray(data.size)
                System.arraycopy(data, 0, newData, 0, data.size)
                if (newData.contentEquals(byteArray)) {
                    target_toure_notify?.value = newData
                    writeCharacteristic(target_toure_notify!!)
                } else {
                    target_ch_write?.value = newData
                    writeCharacteristic(target_ch_write!!)
                //}
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (!mBluetoothGatt!!.writeCharacteristic(characteristic)) {
            Log.e(TAG, "writeCharacteristic failed!!!")
        }
    }

    @SuppressLint("InlinedApi", "MissingPermission")
    fun onRecvCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = if (characteristic.properties and 1 != 0) 18 else 17
            val heartRate = characteristic.getIntValue(flag, 1)
            Log.d(TAG, "Received heart rate: $heartRate")
            return
        }
        val value = characteristic.value ?: return
        if (value.isNotEmpty()) {
            mBleServiceCallback?.onReceived(value, value.size)
        }
    }

    fun isBTConnected(): Boolean {
        return mConnectionState == BTState.CONNECTED
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        mBluetoothGatt?.readCharacteristic(characteristic)
    }

    @SuppressLint("InlinedApi", "MissingPermission")
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        mBluetoothGatt?.setCharacteristicNotification(characteristic, enabled)
        val descriptor =
            characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt?.writeDescriptor(descriptor)
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService>? {
        return mBluetoothGatt?.services
    }

    @SuppressLint("MissingPermission")
    fun getRssiVal(): Boolean {
        return mBluetoothGatt?.readRemoteRssi() ?: false
    }

    fun requestConnection() {
        Log.i(TAG, "requestConnection manually")
        if (mIsConnectingPeriod) {
            return
        }
        scanThenConnect()
    }

    private fun scanThenConnect() {
        Log.i(TAG, "scanThenConnect")
        if (initialize()) {
            scanLeDevice(false)
            scanLeDevice(true)
        }
    }

    fun disconnectDevice() {
        Log.i(TAG, "disconnectDevice")
        scanLeDevice(false)
        disconnectBTGatt()
    }

    private fun buildScanFilters(): List<ScanFilter>? {
        if (scanFilterList == null) {
            scanFilterList = ArrayList()
            val builder = ScanFilter.Builder()
            val fromString = ParcelUuid.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF")
            val uuids = listOf(
                "0000ffe1-0000-1000-8000-00805f9b34fb",
                TOURE_CHARACTERISTIC_NOTIFY,
                TOURE_CHARACTERISTIC_WRITE,
                CH_CHARACTERISTIC_WRITE
            )
            for (uuid in uuids) {
                builder.setServiceUuid(ParcelUuid.fromString(uuid), fromString)
                (scanFilterList as ArrayList<ScanFilter>).add(builder.build())
            }
        }
        return scanFilterList
    }

    private fun buildScanSettings(): ScanSettings? {
        scanSettingBuilder = ScanSettings.Builder()
        scanSettingBuilder?.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        scanSettingBuilder?.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        scanSettingBuilder?.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        return scanSettingBuilder?.build()
    }

    @SuppressLint("MissingPermission")
    fun scanLeDevice(enable: Boolean) {
        Log.i(TAG, "scanLeDevice = $enable")
        val bluetoothAdapter = mBluetoothAdapter ?: return
        if (enable) {
            //scope.coroutineContext.cancelChildren()
            startScanTimeout()
            mHaveTlDevice = false
            onBTStateChange(BTState.SCANNING)

            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null) {
                val filters = buildScanFilters()
                val settings = buildScanSettings()
                if (bluetoothAdapter.isEnabled) {
                    //scanner.startScan(filters, settings, mBLEScanCallback)
                    scanner.startScan(mBLEScanCallback)
                }
            }
        } else {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(mBLEScanCallback)
            //scope.coroutineContext.cancelChildren()  // Cancel scan timeout coroutine
        }
    }

    private fun startScanTimeout() {
        scope.launch {
            delay(BT_SCAN_PERIOD.toLong())
            endScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun endScan() {
        mBluetoothAdapter?.bluetoothLeScanner?.stopScan(mBLEScanCallback)
        if (mConnectionState == BTState.SCANNING && !mHaveTlDevice) {
            onBTStateChange(BTState.NOT_FOUND)
        }
    }

    fun onBTStateChange(state: BTState) {
        if (mConnectionState == state) {
            return
        }
        Log.d(TAG, "onBTStateChange: $state")
        mConnectionState = state
        mBleServiceCallback?.onBTStateChange(state)
        updateNotification(state)  // 알림 업데이트
        if (mConnectionState == BTState.CONNECTED) {
            mIsConnectingPeriod = false
            CarModel.devModule.onBTConnected()
        } else if (mConnectionState == BTState.DISCONNECTED) {
            mIsConnectingPeriod = true
        }
    }

    private fun updateNotification(state: BTState) {
        val notificationIntent = Intent(this, MainActivity2::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stateText = when (state) {
            BTState.CONNECTED -> "Connected"
            BTState.DISCONNECTED -> "Disconnected"
            BTState.SCANNING -> "Scanning"
            BTState.NOT_FOUND -> "Not Found"
            BTState.CONNECTING -> "CONNECTING"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LeBluetoothService")
            .setContentText("Bluetooth State: $stateText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onCreate() {
        super.onCreate()
        initialize()
        startForegroundService()
        //startConnectToDevice()
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
        scope.cancel()  // Cancel all coroutines
        closeBTGatt()
        super.onDestroy()
    }


    private fun startConnectTimeout() {
        scope.launch {
            delay(BT_CONNECT_TIMEOUT.toLong())
            onConnectTimeout()
        }
    }

    private fun onConnectTimeout() {
        mIsConnectingPeriod = false
        Log.d(TAG, "Connect BT timeout!!!")
        if (!isBTConnected()) {
            onBTStateChange(BTState.DISCONNECTED)
            BluetoothModel.clearBTMode()
        }
    }

    private val mBLEScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                scope.launch {
                    startConnectTimeout()
                    try {
                        val device = result.device
                        val name = device.name
                        val address = device.address
                        Log.i(TAG, "device found name: $name")
                        Log.i(TAG, "device found addr: $address")
                        if ((name != null && name == BluetoothModel.BT_NAME1) || (name != null && name == BluetoothModel.BT_NAME2)) {
                            Log.i(TAG, "sinjet device found!")
                            mHaveTlDevice = true
                            scanLeDevice(false)
                            if (address == null) {
                                onBTStateChange(BTState.DISCONNECTED)
                            } else {
                                mBluetoothDevice = device
                                connect(device)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error code $errorCode")
        }
    }

    fun findGattServicesTarget(gattServices: List<BluetoothGattService>?) {
        gattServices?.forEach { gattService ->
            gattService.characteristics.forEach { characteristic ->
                if (characteristic.uuid.toString() == CH_CHARACTERISTIC_WRITE) {
                    setCharacteristicNotification(characteristic, true)
                    readCharacteristic(characteristic)
                    target_ch_write = characteristic
                } else if (characteristic.uuid.toString() == CH_CHARACTERISTIC_NOTIFY) {
                    setCharacteristicNotification(characteristic, true)
                    readCharacteristic(characteristic)
                    target_ch_notify = characteristic
                } else if (characteristic.uuid.toString() == TOURE_CHARACTERISTIC_WRITE) {
                    setCharacteristicNotification(characteristic, true)
                    readCharacteristic(characteristic)
                    target_toure_write = characteristic
                } else if (characteristic.uuid.toString() == TOURE_CHARACTERISTIC_NOTIFY) {
                    setCharacteristicNotification(characteristic, true)
                    readCharacteristic(characteristic)
                    target_toure_notify = characteristic
                } else if (characteristic.uuid.toString() == HC08_SERVICE_UUID) {
                    setCharacteristicNotification(characteristic, true)
                    readCharacteristic(characteristic)
                    target_HC08 = characteristic
                } else if (characteristic.uuid == UUID_HEART_RATE_MEASUREMENT) {
                    setCharacteristicNotification(characteristic, true)
                    readCharacteristic(characteristic)
                    target_HEART_RATE = characteristic
                }
            }
        }
    }

    interface BleServiceCallback {
        fun onBTStateChange(state: BTState)
        fun onReceived(data: ByteArray, length: Int)
    }

    private class SampleGattAttributes {
        companion object {
            const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
            const val HEART_RATE_MEASUREMENT = "0000C004-0000-1000-8000-00805f9b34fb"
        }
    }
}
*/
