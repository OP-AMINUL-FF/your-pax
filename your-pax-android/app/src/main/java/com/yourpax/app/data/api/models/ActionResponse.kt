package com.yourpax.app.data.api.models

import com.google.gson.annotations.SerializedName

data class ActionResponse(
    @SerializedName("status") val status: String = "",
    @SerializedName("message") val message: String = ""
) {
    val isSuccess: Boolean get() = status == "success"
}
