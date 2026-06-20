package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class LogResponse(
    @SerializedName("logs") val logs: String = ""
)
