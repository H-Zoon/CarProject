package com.devidea.chevy.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class SppBluetoothChatService {

    companion object {
        private const val D = true
        private const val NAME_INSECURE = "BluetoothChatInsecure"
        private const val NAME_SECURE = "BluetoothChatSecure"
        private const val TAG = "SPP_SERVICE"
        private val MY_UUID_SECURE: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val MY_UUID_INSECURE: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    @Volatile
    private var mConnectState: Int = 0
    private var mOnChatServiceListener: OnChatServiceListener? = null
    private val mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    interface OnChatServiceListener {
        fun onReceived(data: ByteArray, length: Int)
        fun onStateChange(state: Int, subState: Int)
    }

    fun setOnChatServiceListener(onChatServiceListener: OnChatServiceListener) {
        this.mOnChatServiceListener = onChatServiceListener
    }

    @Synchronized
    fun onStateChange(state: Int, subState: Int) {
        Log.d(TAG, "onStateChange $mConnectState -> $state")
        mConnectState = state
        mOnChatServiceListener?.onStateChange(state, subState)
    }

    @Synchronized
    fun getState(): Int {
        return mConnectState
    }

    @Synchronized
    fun cancelConnectThread() {
        mConnectThread?.cancel()
        mConnectThread = null
        mConnectedThread?.cancel()
        mConnectedThread = null
    }

    @Synchronized
    fun connect(bluetoothDevice: BluetoothDevice, secure: Boolean, autoReconnect: Boolean) {
        if (mConnectState == 2) {
            mConnectThread?.cancel()
            mConnectThread = null
        }
        mConnectedThread?.cancel()
        mConnectedThread = null

        mConnectThread = ConnectThread(bluetoothDevice, secure, autoReconnect)
        mConnectThread?.start()
        onStateChange(2, 0)
    }

    @Synchronized
    fun connected(bluetoothSocket: BluetoothSocket, bluetoothDevice: BluetoothDevice, socketType: String) {
        Log.d(TAG, "connected, Socket Type: $socketType")
        mConnectThread?.cancel()
        mConnectThread = null
        mConnectedThread?.cancel()
        mConnectedThread = null

        mConnectedThread = ConnectedThread(bluetoothSocket, socketType)
        mConnectedThread?.start()
        onStateChange(1, 0)
    }

    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        mConnectThread?.cancel()
        mConnectThread = null
        mConnectedThread?.cancel()
        mConnectedThread = null
    }

    fun write(data: ByteArray) {
        synchronized(this) {
            if (mConnectState != 1) {
                return
            }
            mConnectedThread?.write(data)
        }
    }

    @SuppressLint("NewApi")
    private inner class AcceptThread(private val secure: Boolean) : Thread() {
        private val mSocketType: String = if (secure) "Secure" else "Insecure"
        private val mmServerSocket: BluetoothServerSocket?

        init {
            mmServerSocket = try {
                if (secure) {
                    mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
                } else {
                    mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: $mSocketType listen() failed", e)
                null
            }
        }

        override fun run() {
            Log.d(TAG, "Socket Type: $mSocketType BEGIN mAcceptThread $this")
            name = "AcceptThread$mSocketType"
            var socket: BluetoothSocket?

            while (mConnectState != 1) {
                socket = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket Type: $mSocketType accept() failed", e)
                    break
                }

                socket?.let {
                    synchronized(this@SppBluetoothChatService) {
                        when (mConnectState) {
                            0, 2 -> connected(it, it.remoteDevice, mSocketType)
                            else -> try {
                                it.close()
                            } catch (e: IOException) {
                                Log.e(TAG, "Could not close unwanted socket", e)
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: $mSocketType")
        }

        fun cancel() {
            Log.d(TAG, "Socket Type$mSocketType cancel $this")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type$mSocketType close() of server failed", e)
            }
        }
    }

    @SuppressLint("NewApi")
    inner class ConnectThread(private val device: BluetoothDevice, secure: Boolean, private val isSearchResult: Boolean) : Thread() {
        private val mSocketType: String = if (secure) "Secure" else "Insecure"
        private val mmSocket: BluetoothSocket? = try {
            if (secure) {
                device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
            } else {
                device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Socket Type: $mSocketType create() failed", e)
            null
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:$mSocketType threadId:$id")
            name = "ConnectThread$mSocketType"

            mAdapter.cancelDiscovery()

            try {
                mmSocket?.connect()
                synchronized(this@SppBluetoothChatService) {
                    mConnectThread = null
                }
                Log.d(TAG, "connect succeed!threadId:$id")
                mmSocket?.let { connected(it, device, mSocketType) }
            } catch (e: IOException) {
                Log.d(TAG, "connect failed! mIsSearchResult:$isSearchResult threadId:$id")
                cancel()
                if (isSearchResult) {
                    onStateChange(0, 2)
                } else {
                    onStateChange(0, 1)
                }
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }
        }
    }

    inner class ConnectedThread(private val mmSocket: BluetoothSocket, socketType: String) : Thread() {
        private val mmInStream: InputStream? = try {
            mmSocket.inputStream
        } catch (e: IOException) {
            Log.e(TAG, "temp sockets not created", e)
            null
        }

        private val mmOutStream: OutputStream? = try {
            mmSocket.outputStream
        } catch (e: IOException) {
            Log.e(TAG, "temp sockets not created", e)
            null
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = mmInStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        Log.d("BT", "recv len:$bytes")
                        mOnChatServiceListener?.onReceived(buffer, bytes)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "read exception make it disconnected", e)
                    onStateChange(0, 3)
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                if (mConnectState == 1) {
                    mmOutStream?.write(buffer)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }
}