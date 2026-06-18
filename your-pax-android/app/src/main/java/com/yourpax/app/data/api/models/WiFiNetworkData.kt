package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class WiFiNetwork(
    @SerializedName("bssid") val bssid: String = "",
    @SerializedName("ssid") val ssid: String = "",
    @SerializedName("channel") val channel: String = "",
    @SerializedName("signal") val signal: String = "",
    @SerializedName("wpa") val wpa: String = "",
    @SerializedName("wps") val wps: String = ""
)

data class WiFiScanResponse(
    @SerializedName("networks") val networks: List<WiFiNetwork> = emptyList()
)

data class WiFiStatusResponse(
    @SerializedName("connected") val connected: Boolean = false,
    @SerializedName("ssid") val ssid: String = "",
    @SerializedName("signal") val signal: String = ""
)

data class AttackStatusResponse(
    @SerializedName("running") val running: Boolean = false,
    @SerializedName("output") val output: List<String> = emptyList()
)
