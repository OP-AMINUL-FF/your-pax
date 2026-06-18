package com.yourpax.app.data.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.yourpax.app.util.Constants
import com.yourpax.app.util.NetworkUtils

class BluetoothConnector(private val context: Context) {

    private var isConnected = false

    suspend fun connectToPan(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
        try {
            // Use Bluetooth PAN profile via reflection (not in public SDK)
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                onResult(false)
                return
            }

            // Try to connect using the device's PAN UUID
            val uuid = java.util.UUID.fromString(Constants.BT_PAN_UUID)
            val socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
            socket.connect()
            socket.close()

            // Wait for IP assignment on bt/bnep interface
            kotlinx.coroutines.delay(3000)
            val ip = NetworkUtils.getBluetoothPanIp()
            isConnected = ip != null
            onResult(isConnected)
        } catch (e: Exception) {
            isConnected = false
            onResult(false)
        }
    }

    fun disconnect() {
        isConnected = false
    }

    fun isConnectedToPan(): Boolean = isConnected
}
