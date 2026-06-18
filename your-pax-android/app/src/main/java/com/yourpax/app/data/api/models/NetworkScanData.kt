package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class NetworkScanResponse(
    @SerializedName("headers") val headers: List<String> = emptyList(),
    @SerializedName("rows") val rows: List<List<String>> = emptyList()
)

data class NetKBResponse(
    @SerializedName("headers") val headers: List<String> = emptyList(),
    @SerializedName("rows") val rows: List<List<String>> = emptyList()
)

data class NetKBMetaResponse(
    @SerializedName("ips") val ips: List<String> = emptyList(),
    @SerializedName("ports") val ports: Map<String, List<Int>> = emptyMap(),
    @SerializedName("actions") val actions: List<String> = emptyList()
)
