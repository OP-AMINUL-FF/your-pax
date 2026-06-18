package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class EvilApStatusResponse(
    @SerializedName("running") val running: Boolean = false,
    @SerializedName("mode") val mode: String = "basic"
)

data class EvilClientInfo(
    @SerializedName("ip") val ip: String = "",
    @SerializedName("mac") val mac: String = "",
    @SerializedName("hostname") val hostname: String = ""
)

data class EvilClientsResponse(
    @SerializedName("clients") val clients: List<EvilClientInfo> = emptyList()
)

data class EvilCredEntry(
    @SerializedName("time") val time: String = "",
    @SerializedName("password") val password: String = ""
)

data class LootMonitorData(
    @SerializedName("dns") val dns: List<MonitorDnsEntry> = emptyList(),
    @SerializedName("http") val http: List<MonitorHttpEntry> = emptyList(),
    @SerializedName("devices") val devices: List<MonitorDeviceEntry> = emptyList()
)

data class MonitorDnsEntry(
    @SerializedName("domain") val domain: String = "",
    @SerializedName("client") val client: String = ""
)

data class MonitorHttpEntry(
    @SerializedName("url") val url: String = "",
    @SerializedName("os") val os: String = ""
)

data class MonitorDeviceEntry(
    @SerializedName("hostname") val hostname: String = "",
    @SerializedName("mac") val mac: String = "",
    @SerializedName("ip") val ip: String = ""
)

data class PortalInfo(
    @SerializedName("name") val name: String = "",
    @SerializedName("source") val source: String = "builtin"
)

data class PortalListResponse(
    @SerializedName("portals") val portals: List<PortalInfo> = emptyList()
)

data class ConflictStatus(
    @SerializedName("evil_ap_running") val evilApRunning: Boolean = false,
    @SerializedName("monitor_mode") val monitorMode: Boolean = false,
    @SerializedName("handshake_running") val handshakeRunning: Boolean = false,
    @SerializedName("pmkid_running") val pmkidRunning: Boolean = false,
    @SerializedName("oneshot_running") val oneshotRunning: Boolean = false
)
