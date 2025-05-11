package com.devidea.aicar.k10s

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import com.devidea.aicar.Logger
import com.devidea.aicar.service.ScannedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class BleManagerImpl(
    private val context: Context
) : BleManager {

    companion object {
        private const val TIMEOUT = 5000L

        private const val TAG = "BluetoothLE"
        private const val MAX_SERVICE_DISCOVERY_RETRIES = 3
        private const val MAX_MTU_REQUEST_RETRIES = 5
        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
        private val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val INITIAL_MTU = 185
        private const val MIN_MTU = 23
    }

    private var bleCallback: BleCallback? = null
    private var currentMTU = INITIAL_MTU
    private var serviceDiscoveryRetryCount = 0
    private var mtuRequestRetryCount = 0
    private var bluetoothGatt: BluetoothGatt? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleManagerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            //bleCallback?.onStateChanged(BTState.SCANNING)
            try {
                val device = result.device
                //bleCallback?.scannedDevice(name = device.name, address = device.address, device = device)
            } catch (e: Exception) {
                Logger.e { "Scan result processing error: ${e.localizedMessage}" }
                //bleCallback?.onStateChanged(BTState.NOT_FOUND)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Logger.e { "Scan failed with error code: $errorCode" }
            bleCallback?.onError("Scan failed with error code: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Logger.d { "Connected to GATT server." }
                    //bleCallback?.onStateChanged(BTState.CONNECTED)
                    discoverServicesWithRetry()
                }

                BluetoothGatt.STATE_DISCONNECTED -> {
                    Logger.d { "Disconnected from GATT server." }
                    //bleCallback?.onStateChanged(BTState.DISCONNECTED)
                }

                else -> {
                    Logger.d { "Connection state changed to $newState with status $status" }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d { "MTU successfully changed to $mtu" }
                currentMTU = mtu
                bleCallback?.onMtuChanged(mtu)
            } else {
                Logger.e { "MTU change failed with status $status" }
                handleMtuChangeFailure()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d { "Services discovered." }
                val service = gatt.getService(SERVICE_UUID)

                val notifyCharacteristic = service?.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
                val writeCharacteristic = service?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

                if (notifyCharacteristic != null && writeCharacteristic != null) {
                    setCharacteristicNotification(gatt, notifyCharacteristic)
                    requestMtuWithRetry(INITIAL_MTU)
                } else {
                    Logger.e { "Required characteristics not found." }
                    handleServiceDiscoveryFailure(status)
                }
            } else {
                Logger.e { "Service discovery failed with status $status." }
                handleServiceDiscoveryFailure(status)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d { "Characteristic read: ${value.joinToString()}" }
                bleCallback?.onCharacteristicRead(value, value.size)
            } else {
                Logger.e { "Characteristic read failed with status $status" }
                bleCallback?.onError("Characteristic read failed with status $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            bleCallback?.onCharacteristicRead(value, value.size)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d { "Characteristic written successfully" }
                bleCallback?.onSuccess()
            } else {
                Logger.e { "Characteristic write failed with status $status" }
                bleCallback?.onError("Characteristic write failed with status $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d { "Descriptor written successfully" }
            } else {
                Logger.e { "Descriptor write failed with status $status" }
                bleCallback?.onError("Descriptor write failed with status $status")
            }
        }
    }
    private fun setCharacteristicNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID)
        descriptor?.let {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            } else {
                gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            }
        } ?: Logger.e { "Descriptor not found for characteristic ${characteristic.uuid}" }
    }

    private fun handleServiceDiscoveryFailure(status: Int) {
        if (serviceDiscoveryRetryCount < MAX_SERVICE_DISCOVERY_RETRIES) {
            serviceDiscoveryRetryCount++
            Logger.d { "Retrying service discovery... Attempt $serviceDiscoveryRetryCount" }
            discoverServicesWithRetry()
        } else {
            Logger.e { "Service discovery failed after $MAX_SERVICE_DISCOVERY_RETRIES attempts." }
            bleCallback?.onError("Service discovery failed.")
            disconnect()
        }
    }

    private fun discoverServicesWithRetry() {
        bleManagerScope.launch {
            delay(500)
            bluetoothGatt?.discoverServices()
        }
    }

    private fun handleMtuChangeFailure() {
        if (mtuRequestRetryCount < MAX_MTU_REQUEST_RETRIES) {
            mtuRequestRetryCount++
            val decrementStep = 10
            currentMTU = maxOf(currentMTU - decrementStep, MIN_MTU)
            Logger.d { "Retrying MTU request with MTU = $currentMTU (Attempt $mtuRequestRetryCount)" }
            requestMtuWithRetry(currentMTU)
        } else {
            Logger.e { "MTU request failed after $MAX_MTU_REQUEST_RETRIES attempts." }
            bleCallback?.onError("MTU negotiation failed.")
            mtuRequestRetryCount = 0
            disconnect()
        }
    }

    @Synchronized
    private fun requestMtuWithRetry(desiredMtu: Int) {
        bleManagerScope.launch {
            delay((1000L * mtuRequestRetryCount)) // Exponential backoff
            if (bluetoothGatt?.requestMtu(desiredMtu) == false) {
                Logger.e { "MTU request not supported." }
                handleMtuChangeFailure()
            }
        }
    }

    override fun startScan() {
        scanJob?.cancel()
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        scanJob = bleManagerScope.launch {
            delay(TIMEOUT)
            stopScan()
        }
    }

    override fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        scanJob?.cancel()
        //bleCallback?.onStateChanged(BTState.SCANNED_LIST)
    }

    override fun requestConnect(device: ScannedDevice) {
        scanJob?.cancel()
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        //bluetoothGatt = scannedDevice.device.connectGatt(context, false, gattCallback)
        //bleCallback?.onStateChanged(BTState.CONNECTING)
    }


    override fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        //bleCallback?.onStateChanged(BTState.DISCONNECTED)
    }

    override fun sendMessage(value: String) {
        /*val byteValue = value.toByte()
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

        if (characteristic != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                characteristic.value = byteValue
                bluetoothGatt?.writeCharacteristic(characteristic)
            } else {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    byteValue,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            }
        } else {
            Logger.e { "Write characteristic not found." }
            bleCallback?.onError("Write characteristic not found.")
        }*/
    }

    override fun setBleCallback(callback: BleCallback?) {
        bleCallback = callback
    }
}