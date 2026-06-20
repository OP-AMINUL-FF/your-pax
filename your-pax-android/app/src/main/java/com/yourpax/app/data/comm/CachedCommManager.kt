package com.yourpax.app.data.comm

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yourpax.app.data.api.models.ActionResponse
import com.yourpax.app.data.api.models.BackupResponse

class CachedCommManager(
    private val delegate: CommunicationManager
) : CommunicationManager {

    private val gson = Gson()

    private val mutationCommands = setOf(
        "save_config", "trigger_scan", "trigger_bruteforce", "trigger_vulnscan",
        "trigger_steal", "stop_all", "handshake_start", "handshake_stop",
        "pmkid_start", "pmkid_stop", "deauth_attack", "oneshot", "oneshot_stop",
        "start_evil_ap", "stop_evil_ap", "stop_evil_clone", "connect_wifi",
        "disconnect_wifi", "reboot", "shutdown", "restart_your_pax_service",
        "stop_orchestrator", "start_orchestrator", "clear_files", "clear_files_light",
        "initialize_csv", "restore_default_config", "execute_manual_attack",
        "delete_portal", "restore", "upload_portal"
    )

    override suspend fun <T> request(
        cmd: String,
        params: Map<String, Any>,
        responseType: Class<T>
    ): Result<T> {
        if (cmd in mutationCommands) {
            DataCache.invalidateMutations()
        }
        val cached = DataCache.get(cmd, params)
        if (cached != null) {
            return Result.success(gson.fromJson(cached, responseType))
        }
        val result = delegate.request(cmd, params, responseType)
        result.onSuccess { value ->
            val json = gson.toJsonTree(value)
            if (json.isJsonObject) {
                DataCache.put(cmd, params, json.asJsonObject)
            }
        }
        return result
    }

    override suspend fun <T> requestJson(
        cmd: String,
        params: Map<String, Any>,
        parser: (JsonObject) -> T
    ): Result<T> {
        if (cmd in mutationCommands) {
            DataCache.invalidateMutations()
        }
        val cached = DataCache.get(cmd, params)
        if (cached != null) {
            return Result.success(parser(cached))
        }
        val rawResult = delegate.requestJson(cmd, params) { it }
        val result = rawResult.map { parser(it) }
        result.onSuccess {
            rawResult.onSuccess { json ->
                DataCache.put(cmd, params, json)
            }
        }
        return result
    }

    override suspend fun downloadFile(path: String): Result<ByteArray> {
        val cached = DataCache.getDownload(path)
        if (cached != null) return Result.success(cached)
        val result = delegate.downloadFile(path)
        result.onSuccess { DataCache.putDownload(path, it) }
        return result
    }

    override suspend fun downloadStoreFile(path: String): Result<ByteArray> {
        val cached = DataCache.getDownload(path)
        if (cached != null) return Result.success(cached)
        val result = delegate.downloadStoreFile(path)
        result.onSuccess { DataCache.putDownload(path, it) }
        return result
    }

    override suspend fun backup(): Result<BackupResponse> {
        return delegate.backup()
    }

    override suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        DataCache.invalidateAll()
        return delegate.restore(fileBytes, fileName)
    }

    override suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        DataCache.invalidateAll()
        return delegate.uploadPortal(fileBytes, fileName)
    }

    override suspend fun connect(): Boolean = delegate.connect()

    override suspend fun disconnect() {
        DataCache.invalidateAll()
        delegate.disconnect()
    }

    override fun isConnected(): Boolean = delegate.isConnected()

    override fun getType(): CommType = delegate.getType()

    override var onStatusChange: ((ConnectionStatus) -> Unit)?
        get() = delegate.onStatusChange
        set(value) { delegate.onStatusChange = value }

    override var onEvent: ((event: String, data: JsonObject) -> Unit)?
        get() = delegate.onEvent
        set(value) { delegate.onEvent = value }
}
