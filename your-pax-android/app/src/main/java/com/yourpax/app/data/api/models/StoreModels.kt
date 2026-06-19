package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class StoreFileItem(
    @SerializedName("name") val name: String = "",
    @SerializedName("path") val path: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("modified") val modified: String = "",
    @SerializedName("rel") val rel: String = ""
)

data class CrackedService(
    @SerializedName("name") val name: String = "",
    @SerializedName("count") val count: Int = 0
)

data class CredsCrackedData(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("services") val services: List<CrackedService> = emptyList()
)

data class WpsReport(
    @SerializedName("file") val file: String = "",
    @SerializedName("content") val content: String = ""
)

data class StoreDataFull(
    @SerializedName("handshakes") val handshakes: List<StoreFileItem> = emptyList(),
    @SerializedName("pmkid") val pmkid: List<StoreFileItem> = emptyList(),
    @SerializedName("creds_evil") val credsEvil: List<EvilCredEntry> = emptyList(),
    @SerializedName("wps_reports") val wpsReports: List<WpsReport> = emptyList(),
    @SerializedName("creds_cracked") val credsCracked: CredsCrackedData = CredsCrackedData(),
    @SerializedName("stolen_files") val stolenFiles: List<StoreFileItem> = emptyList(),
    @SerializedName("scan_results") val scanResults: List<StoreFileItem> = emptyList(),
    @SerializedName("vuln_scans") val vulnScans: List<StoreFileItem> = emptyList(),
    @SerializedName("zombies") val zombies: List<StoreFileItem> = emptyList(),
    @SerializedName("netkb_count") val netkbCount: Int = 0
)

data class BackupResponse(
    @SerializedName("status") val status: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("filename") val filename: String = "",
    @SerializedName("message") val message: String = ""
)

data class ScanTargetResult(
    @SerializedName("bssid") val bssid: String = "",
    @SerializedName("ssid") val ssid: String = "",
    @SerializedName("channel") val channel: String = ""
)

data class ScanTargetsResponse(
    @SerializedName("networks") val networks: List<ScanTargetResult> = emptyList()
)

data class WebDelayResponse(
    @SerializedName("web_delay") val webDelay: Int = 5000
)
