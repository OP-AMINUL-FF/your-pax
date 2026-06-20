package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class StoreData(
    @SerializedName("handshakes") val handshakes: Int = 0,
    @SerializedName("pmkid") val pmkid: Int = 0,
    @SerializedName("creds_evil") val credsEvil: Int = 0,
    @SerializedName("creds_cracked") val credsCracked: Int = 0,
    @SerializedName("stolen_files") val stolenFiles: Int = 0,
    @SerializedName("scan_results") val scanResults: Int = 0,
    @SerializedName("vuln_scans") val vulnScans: Int = 0,
    @SerializedName("netkb_count") val netkbCount: Int = 0
)
