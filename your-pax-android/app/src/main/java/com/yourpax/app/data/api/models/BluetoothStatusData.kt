package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class BluetoothStatus(
    @SerializedName("active") val active: Boolean = false,
    @SerializedName("bridge_ip") val bridgeIp: String = "",
    @SerializedName("port") val port: Int = 0,
    @SerializedName("ssid") val ssid: String = "",
    @SerializedName("connected_clients") val connectedClients: Int = 0,
    @SerializedName("clients") val clients: List<String> = emptyList()
)

data class BluetoothDeviceInfo(
    @SerializedName("name") val name: String = "",
    @SerializedName("ip") val ip: String = "",
    @SerializedName("mac") val mac: String = ""
)

data class BluetoothDevicesResponse(
    @SerializedName("devices") val devices: List<BluetoothDeviceInfo> = emptyList()
)
