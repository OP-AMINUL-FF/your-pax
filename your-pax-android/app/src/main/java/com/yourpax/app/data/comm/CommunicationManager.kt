package com.yourpax.app.data.comm

import com.google.gson.JsonObject
import com.yourpax.app.data.api.models.ActionResponse
import com.yourpax.app.data.api.models.BackupResponse

enum class CommType { HTTP, BLUETOOTH }
enum class ConnectionStatus { CONNECTED, DISCONNECTED, RECONNECTING }

interface CommunicationManager {

    suspend fun <T> request(
        cmd: String,
        params: Map<String, Any> = emptyMap(),
        responseType: Class<T>
    ): Result<T>

    suspend fun <T> requestJson(
        cmd: String,
        params: Map<String, Any> = emptyMap(),
        parser: (JsonObject) -> T
    ): Result<T>

    suspend fun downloadFile(path: String): Result<ByteArray>

    suspend fun downloadStoreFile(path: String): Result<ByteArray>

    suspend fun backup(): Result<BackupResponse>

    suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse>

    suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse>

    suspend fun connect(): Boolean
    suspend fun disconnect()
    fun isConnected(): Boolean
    fun getType(): CommType

    var onStatusChange: ((ConnectionStatus) -> Unit)?
        get() = null
        set(value) {}

    var onEvent: ((event: String, data: JsonObject) -> Unit)?
        get() = null
        set(value) {}
}
