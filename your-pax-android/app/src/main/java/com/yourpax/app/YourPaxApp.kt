package com.yourpax.app

import android.app.Application

class YourPaxApp : Application() {
    val bluetoothConnectionState = BluetoothConnectionState()
}

class BluetoothConnectionState(
    var isConnected: Boolean = false,
    var deviceIp: String = "192.168.4.1",
    var devicePort: Int = 8000,
    var deviceName: String = ""
)
