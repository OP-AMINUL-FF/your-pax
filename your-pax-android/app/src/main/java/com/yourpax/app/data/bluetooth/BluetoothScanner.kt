package com.yourpax.app.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.yourpax.app.util.Constants

class BluetoothScanner(private val bluetoothAdapter: BluetoothAdapter?) {

    private var isScanning = false
    private var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    private var onScanFinished: (() -> Unit)? = null

    fun startScan(
        onFound: (BluetoothDevice) -> Unit,
        onFinished: () -> Unit
    ) {
        if (bluetoothAdapter == null || isScanning) return
        onDeviceFound = onFound
        onScanFinished = onFinished
        isScanning = true

        bluetoothAdapter.startDiscovery()
        bluetoothAdapter.cancelDiscovery()
        bluetoothAdapter.startDiscovery()

        object : Thread() {
            override fun run() {
                try {
                    sleep(Constants.BT_SCAN_TIMEOUT)
                } catch (_: InterruptedException) {}
                isScanning = false
                bluetoothAdapter.cancelDiscovery()
                onScanFinished?.invoke()
            }
        }.start()
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun findYourPaxDevice(): BluetoothDevice? {
        return getBondedDevices().find {
            it.name?.contains(Constants.YOUR_PAX_BT_NAME, ignoreCase = true) == true
        }
    }

    fun stopScan() {
        isScanning = false
        bluetoothAdapter?.cancelDiscovery()
    }
}
