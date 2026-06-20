package com.yourpax.app.data.repository

import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class BluetoothRepository(private val comm: CommunicationManager = CommHolder.comm) {

    suspend fun getBluetoothStatus(): Result<BluetoothStatus> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoBluetoothStatus
        comm.request("bluetooth_status", emptyMap(), BluetoothStatus::class.java).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getBluetoothDevices(): Result<List<BluetoothDeviceInfo>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoBluetoothDevices
        val response = comm.request("bluetooth_devices", emptyMap(), BluetoothDevicesResponse::class.java).getOrThrow()
        response.devices
    }

    suspend fun testConnection(): Result<Boolean> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching true
        comm.request("load_config", emptyMap(), ConfigData::class.java).isSuccess
    }
}
