package com.yourpax.app.data.repository

import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class BluetoothRepository {
    private val api get() = RetrofitProvider.getApiService()

    suspend fun getBluetoothStatus(): Result<BluetoothStatus> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoBluetoothStatus
        val response = api.bluetoothStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get BT status: ${response.code()}")
    }

    suspend fun getBluetoothDevices(): Result<List<BluetoothDeviceInfo>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoBluetoothDevices
        val response = api.bluetoothDevices()
        if (response.isSuccessful) response.body()!!.devices
        else throw Exception("Failed to get BT devices: ${response.code()}")
    }

    suspend fun testConnection(): Result<Boolean> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching true
        val response = api.loadConfig()
        response.isSuccessful
    }
}
