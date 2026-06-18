package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class CredentialFile(
    @SerializedName("name") val name: String = "",
    @SerializedName("headers") val headers: List<String> = emptyList(),
    @SerializedName("rows") val rows: List<List<String>> = emptyList()
)
