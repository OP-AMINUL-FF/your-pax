package com.yourpax.app.data.bluetooth

sealed class BluetoothConnectionState {
    data object Idle : BluetoothConnectionState()
    data object CheckingBluetooth : BluetoothConnectionState()
    data object PermissionGranted : BluetoothConnectionState()
    data object BluetoothOff : BluetoothConnectionState()
    data object Scanning : BluetoothConnectionState()
    data object Connecting : BluetoothConnectionState()
    data object TestingApi : BluetoothConnectionState()
    data class Connected(val ip: String, val port: Int = 8000) : BluetoothConnectionState()
    data class Error(val message: String) : BluetoothConnectionState()
}
