package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class ConfigData(
    @SerializedName("manual_mode") val manualMode: Boolean = false,
    @SerializedName("websrv") val webServer: Boolean = true,
    @SerializedName("debug_mode") val debugMode: Boolean = true,
    @SerializedName("scan_interval") val scanInterval: Int = 180,
    @SerializedName("scan_vuln_interval") val scanVulnInterval: Int = 900,
    @SerializedName("wifi_interface") val wifiInterface: String = "wlan0",
    @SerializedName("monitor_interface") val monitorInterface: String = "wlan0mon",
    @SerializedName("evil_ap_ssid") val evilApSsid: String = "FreeWiFi",
    @SerializedName("evil_ap_channel") val evilApChannel: Int = 6,
    @SerializedName("portlist") val portList: List<Int> = emptyList(),
    @SerializedName("ip_scan_blacklist") val ipBlacklist: List<String> = emptyList(),
    @SerializedName("mac_scan_blacklist") val macBlacklist: List<String> = emptyList(),
    @SerializedName("startup_delay") val startupDelay: Int = 10,
    @SerializedName("nmap_scan_aggressivity") val nmapAggressivity: String = "-T2",
    @SerializedName("enable_monitor_mode") val enableMonitorMode: Boolean = false,
    @SerializedName("evil_ap_running") val evilApRunning: Boolean = false
)
