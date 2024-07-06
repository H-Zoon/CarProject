package com.devidea.chevy.bluetooth

import android.annotation.SuppressLint
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
import android.os.ParcelUuid
import com.devidea.chevy.carsystem.CarModel
import java.util.Timer
import java.util.TimerTask
import java.util.UUID


class LeBluetoothService : Service() {

    private val tag = "BLE"
    private val mBinder = LocalBinder()
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothManager: BluetoothManager? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private var mHaveTlDevice = false
    private var mConnectionState = BTState.DISCONNECTED
    private var mIsConnecttingPeriod = false
    private var mBleServiceCallback: BleServiceCallback? = null
    private var scanFilterList: List<ScanFilter>? = null
    private var scanSettingBuilder: ScanSettings.Builder? = null

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
        private var target_chara: BluetoothGattCharacteristic? = null
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(tag, "onConnectionStateChange: $status -> $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server.")
                val requestMtu = gatt.requestMtu(185)
                Log.d(tag, "requestMtu to 185 result: $requestMtu")
                if (requestMtu || !gatt.discoverServices()) {
                    Log.d(tag, "Attempting to start service discovery")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onBTStateChange(BTState.DISCONNECTED)
                if (mIsConnecttingPeriod) {
                    runBlocking {
                        delay(1000)
                        connect(mBluetoothDevice)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.d(tag, "onMtuChanged: $mtu")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "change MTU succeed = $mtu")
                BluetoothModel.onMtuChanged(mtu)
                if (gatt.discoverServices()) {
                    Log.i(tag, "Attempting to start service discovery")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(tag, "onServicesDiscovered: $status")
            if (status != BluetoothGatt.GATT_SUCCESS && mIsConnecttingPeriod) {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        initialize()
                        connect(mBluetoothDevice)
                    }
                }, 2000)
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                displayGattServices(getSupportedGattServices())
                onBTStateChange(BTState.CONNECTED)
                CarModel.devModule.onBTConnected()
            } else {
                Log.w(tag, "onServicesDiscovered received: $status")
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
            Log.i(tag, "onCharacteristicRead: $status")
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
                Log.e(tag, "onDescriptorWrite failed")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i(tag, "onCharacteristicChanged from: ")
            onRecvCharacteristic(characteristic)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Log.d(tag, "rssi = $rssi")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(tag, "onCharacteristicWrite failed")
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): LeBluetoothService = this@LeBluetoothService
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(tag, "onUnbind close.")
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
        val bluetoothAdapter = mBluetoothAdapter ?: return false
        bluetoothDevice ?: return false
        closeBTGatt()
        Log.d(tag, "Trying a connection with BT addr: ${bluetoothDevice.address}")
        mBluetoothGatt = bluetoothDevice.connectGatt(this, false, mGattCallback)
        mBluetoothGatt ?: return false
        onBTStateChange(BTState.CONNECTED)
        return true
    }

    private fun disconnectBTGatt() {
        mBluetoothGatt?.disconnect()
    }

    private fun closeBTGatt() {
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    fun setBleServiceCallback(callback: BleServiceCallback) {
        mBleServiceCallback = callback
    }

    fun isInConnecttingPeriod(): Boolean {
        return mIsConnecttingPeriod
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(data: ByteArray) {
        if (isBTConnected()) {
            if (mBluetoothGatt == null || target_chara == null) {
                Log.d(tag, "send message but BluetoothGatt or target_chara == null")
                return
            }
            val newData = ByteArray(data.size)
            System.arraycopy(data, 0, newData, 0, data.size)
            target_chara!!.value = newData
            writeCharacteristic(target_chara!!)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (!mBluetoothGatt!!.writeCharacteristic(characteristic)) {
            Log.e(tag, "writeCharacteristic failed!!!")
        }
    }

    @SuppressLint("InlinedApi", "MissingPermission")
    fun onRecvCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = if (characteristic.properties and 1 != 0) 18 else 17
            val heartRate = characteristic.getIntValue(flag, 1)
            Log.d(tag, "Received heart rate: $heartRate")
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
        Log.i(tag, "requestConnection manually")
        if (mIsConnecttingPeriod) {
            return
        }
        scanThenConnect()
    }

    private fun scanThenConnect() {
        Log.i(tag, "scanThenConnect")
        if (initialize()) {
            scanLeDevice(false)
            scanLeDevice(true)
        }
    }

    fun disconnectDevice() {
        Log.i(tag, "disconnectDevice")
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
        /*if (scanSettingBuilder != null) {
            return null
        }*/
        scanSettingBuilder = ScanSettings.Builder()
        scanSettingBuilder?.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        scanSettingBuilder?.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        scanSettingBuilder?.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        return scanSettingBuilder?.build()
    }

    private fun buildUUIDs(): Array<UUID> {
        return arrayOf(
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
            UUID.fromString(TOURE_CHARACTERISTIC_NOTIFY),
            UUID.fromString(TOURE_CHARACTERISTIC_WRITE),
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
            UUID.fromString(CH_CHARACTERISTIC_WRITE)
        )
    }

    @SuppressLint("MissingPermission")
    fun scanLeDevice(enable: Boolean) {
        Log.i(tag, "scanLeDevice = $enable")
        val bluetoothAdapter = mBluetoothAdapter ?: return
        if (enable) {
            mHandler.removeCallbacks(mEndScanRunnable)
            mHandler.postDelayed(mEndScanRunnable, BT_SCAN_PERIOD.toLong())
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
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null) {
                scanner.stopScan(mBLEScanCallback)
            }
            mHandler.removeCallbacks(mEndScanRunnable)
        }
    }

    override fun onDestroy() {
        Log.d(tag, "onDestroy")
        closeBTGatt()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    fun onBTStateChange(state: BTState) {
        if (mConnectionState == state) {
            return
        }
        Log.d(tag, "onBTStateChange: $state")
        mConnectionState = state
        mBleServiceCallback?.onBTStateChange(state)
        if (mConnectionState == BTState.CONNECTED) {
            mIsConnecttingPeriod = false
            mHandler.removeCallbacks(mConnectTimeoutRunnable)
        }
    }

    private val mConnectTimeoutRunnable = Runnable {
        mIsConnecttingPeriod = false
        Log.d(tag, "Connect BT timeout!!!")
        if (!isBTConnected()) {
            onBTStateChange(BTState.DISCONNECTED)
            BluetoothModel.clearBTMode()
        }
    }

    private val mEndScanRunnable = Runnable {
        mBluetoothAdapter?.bluetoothLeScanner?.stopScan(mBLEScanCallback)
        if (mConnectionState == BTState.SCANNING && !mHaveTlDevice) {
            onBTStateChange(BTState.NOT_FOUND)
        }
    }

    private val mBLEScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val device = result.device
                        val name = device.name
                        val address = device.address
                        Log.i(tag, "device found name: $name")
                        Log.i(tag, "device found addr: $address")
                        if ((name != null && name == BluetoothModel.BT_NAME1) || (name != null && name == BluetoothModel.BT_NAME2)) {
                            Log.i(tag, "sinjet device found!")
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
                        Log.e(tag, e.toString())
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "BLE scan failed with error code $errorCode")
        }
    }

    fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        /*gattServices ?: return
        for (gattService in gattServices) {
            val characteristics = gattService.characteristics
            if (lookforCh(characteristics)) return
            if (lookforToure(characteristics)) return
            if (lookforHc(characteristics)) return
        }*/

        for (gattService in gattServices!!) {
            val characteristics = gattService.characteristics
            a(characteristics)
        }
    }

    private fun a(list: List<BluetoothGattCharacteristic>): Boolean {
        var b = false
        for (characteristic in list) {
            val uuid = characteristic.uuid.toString()

            if (uuid == CH_CHARACTERISTIC_WRITE) {
                setCharacteristicNotification(characteristic, true)
                readCharacteristic(characteristic)
                target_chara = characteristic
                b = true
            }
        }
        return b
    }

    private fun lookforCh(list: List<BluetoothGattCharacteristic>): Boolean {
        var notifyFound = false
        var writeFound = false
        for (characteristic in list) {
            val uuid = characteristic.uuid.toString()
            if (uuid == "0000ffe1-0000-1000-8000-00805f9b34fb") {
                setCharacteristicNotification(characteristic, true)
                notifyFound = true
            }
            if (uuid == CH_CHARACTERISTIC_WRITE) {
                setCharacteristicNotification(characteristic, true)
                readCharacteristic(characteristic)
                target_chara = characteristic
                writeFound = true
            }
        }
        return notifyFound && writeFound
    }

    private fun lookforHc(list: List<BluetoothGattCharacteristic>): Boolean {
        for (characteristic in list) {
            if (characteristic.uuid.toString() == "0000ffe1-0000-1000-8000-00805f9b34fb") {
                setCharacteristicNotification(characteristic, true)
                readCharacteristic(characteristic)
                target_chara = characteristic
                return true
            }
        }
        return false
    }

    private fun lookforToure(list: List<BluetoothGattCharacteristic>): Boolean {
        var notifyFound = false
        var writeFound = false
        for (characteristic in list) {
            val uuid = characteristic.uuid.toString()
            if (uuid == TOURE_CHARACTERISTIC_NOTIFY) {
                setCharacteristicNotification(characteristic, true)
                notifyFound = true
            }
            if (uuid == TOURE_CHARACTERISTIC_WRITE) {
                setCharacteristicNotification(characteristic, true)
                readCharacteristic(characteristic)
                target_chara = characteristic
                writeFound = true
            }
        }
        return notifyFound && writeFound
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
