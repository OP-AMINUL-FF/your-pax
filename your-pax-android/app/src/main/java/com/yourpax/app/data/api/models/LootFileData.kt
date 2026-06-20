package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class LootFile(
    @SerializedName("name") val name: String = "",
    @SerializedName("is_directory") val isDirectory: Boolean = false,
    @SerializedName("children") val children: List<LootFile> = emptyList(),
    @SerializedName("path") val path: String = ""
)
