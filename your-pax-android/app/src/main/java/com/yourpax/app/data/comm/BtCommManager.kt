package com.yourpax.app.data.comm

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yourpax.app.data.api.models.ActionResponse
import com.yourpax.app.data.api.models.BackupResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class BtCommManager(
    private val deviceAddress: String,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : CommunicationManager {

    companion object {
        private const val BT_MTU = 16384
        private const val CMD_TIMEOUT_MS = 30000L
        private const val CONNECT_TIMEOUT_MS = 10000L
        private const val CHUNK_SIZE = 10000
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var requestId = 0
    private val mutex = Mutex()
    private val pendingResponses = ConcurrentHashMap<Int, CompletableDeferred<JsonObject>>()
    private var readerJob: Job? = null
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())
    private val gson = Gson()
    override var onStatusChange: ((ConnectionStatus) -> Unit)? = null
    override var onEvent: ((event: String, data: JsonObject) -> Unit)? = null

    override suspend fun connect(): Boolean {
        if (isConnected()) return true
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withTimeoutOrNull false
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket?.connect()
            outputStream = socket?.outputStream
            inputStream = socket?.inputStream
            if (socket?.isConnected == true) {
                readerJob = scope.launch { readLoop() }
                onStatusChange?.invoke(ConnectionStatus.CONNECTED)
                true
            } else false
        } ?: false
    }

    override suspend fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
        pendingResponses.forEach { (_, d) -> d.completeExceptionally(DisconnectedException()) }
        pendingResponses.clear()
        onStatusChange?.invoke(ConnectionStatus.DISCONNECTED)
    }

    override fun isConnected(): Boolean = socket?.isConnected == true

    override fun getType(): CommType = CommType.BLUETOOTH

    override suspend fun <T> request(
        cmd: String,
        params: Map<String, Any>,
        responseType: Class<T>
    ): Result<T> {
        return try {
            val json = requestJson(cmd, params) { it }.getOrThrow()
            val data = gson.fromJson(json, responseType)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun <T> requestJson(
        cmd: String,
        params: Map<String, Any>,
        parser: (JsonObject) -> T
    ): Result<T> {
        return try {
            val response = sendCommand(cmd, params)
            when (response.get("status")?.asString) {
                "ok" -> Result.success(parser(response.getAsJsonObject("data")))
                "error" -> Result.failure(
                    BtCommandException(response.get("error")?.asString ?: "unknown error")
                )
                else -> Result.failure(BtCommandException("unexpected status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadStoreFile(path: String): Result<ByteArray> {
        return try {
            val meta = sendCommand("download_store", mapOf("path" to path))
            val data = meta.getAsJsonObject("data")
            if (data.get("inline")?.asBoolean == true) {
                val b64 = data.get("data").asString
                Result.success(Base64.decode(b64, Base64.DEFAULT))
            } else {
                val chunks = data.get("chunks").asInt
                val buffer = mutableListOf<ByteArray>()
                for (i in 0 until chunks) {
                    val chunkResp = sendCommand("download_chunk", mapOf("path" to path, "index" to i))
                    val chunkData = chunkResp.getAsJsonObject("data")
                    val b64 = chunkData.get("data").asString
                    buffer.add(Base64.decode(b64, Base64.DEFAULT))
                }
                Result.success(buffer.fold(ByteArray(0)) { acc, bytes -> acc + bytes })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(path: String): Result<ByteArray> {
        return try {
            val meta = sendCommand("download_file", mapOf("path" to path))
            val data = meta.getAsJsonObject("data")
            if (data.get("inline")?.asBoolean == true) {
                val b64 = data.get("data").asString
                Result.success(Base64.decode(b64, Base64.DEFAULT))
            } else {
                val chunks = data.get("chunks").asInt
                val buffer = mutableListOf<ByteArray>()
                for (i in 0 until chunks) {
                    val chunkResp = sendCommand("download_chunk", mapOf("path" to path, "index" to i))
                    val chunkData = chunkResp.getAsJsonObject("data")
                    val b64 = chunkData.get("data").asString
                    buffer.add(Base64.decode(b64, Base64.DEFAULT))
                }
                Result.success(buffer.fold(ByteArray(0)) { acc, bytes -> acc + bytes })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        return chunkedUpload("restore", fileBytes, fileName)
    }

    override suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        return chunkedUpload("upload_portal", fileBytes, fileName)
    }

    private suspend fun chunkedUpload(cmd: String, fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        return try {
            val total = fileBytes.size
            val chunks = (total + CHUNK_SIZE - 1) / CHUNK_SIZE
            for (i in 0 until chunks) {
                val start = i * CHUNK_SIZE
                val end = minOf(start + CHUNK_SIZE, total)
                val chunk = fileBytes.copyOfRange(start, end)
                val b64 = Base64.encodeToString(chunk, Base64.DEFAULT)
                val isLast = i == chunks - 1
                sendCommand("upload_chunk", mapOf(
                    "cmd" to cmd,
                    "filename" to fileName,
                    "index" to i,
                    "total" to total,
                    "data" to b64,
                    "last" to isLast
                ))
            }
            val json = sendCommand("upload_finish", mapOf("cmd" to cmd, "filename" to fileName, "total" to total))
            val data = json.getAsJsonObject("data")
            Result.success(gson.fromJson(data, ActionResponse::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun backup(): Result<BackupResponse> {
        return try {
            val json = sendCommand("backup", emptyMap())
            val data = json.getAsJsonObject("data")
            Result.success(gson.fromJson(data, BackupResponse::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendCommand(cmd: String, params: Map<String, Any>): JsonObject {
        val id: Int
        val deferred = CompletableDeferred<JsonObject>()
        mutex.withLock {
            id = ++requestId
            pendingResponses[id] = deferred
        }
        val request = buildString {
            append("{\"id\":$id,\"cmd\":\"$cmd\",\"params\":")
            append(gson.toJson(params))
            append("}\n")
        }
        try {
            outputStream?.write(request.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            mutex.withLock { pendingResponses.remove(id) }
            onStatusChange?.invoke(ConnectionStatus.DISCONNECTED)
            throw BtConnectionException("Failed to send command: ${e.message}", e)
        }
        return withTimeout(CMD_TIMEOUT_MS) { deferred.await() }
    }

    private suspend fun readLoop() {
        val reader = inputStream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8), BT_MTU) }
        try {
            while (isActive) {
                val line = reader?.readLine() ?: break
                if (line.isBlank()) continue
                try {
                    val response = JsonParser().parse(line).asJsonObject
                    val id = response.get("id")?.asInt ?: continue
                    if (id == -1) {
                        val event = response.get("event")?.asString ?: continue
                        val data = response.getAsJsonObject("data")
                        onEvent?.invoke(event, data)
                        continue
                    }
                    mutex.withLock {
                        pendingResponses.remove(id)?.complete(response)
                    }
                } catch (_: Exception) {
                    // Skip malformed JSON lines
                }
            }
        } catch (_: CancellationException) {
            // Normal cancellation
        } catch (_: Exception) {
            onStatusChange?.invoke(ConnectionStatus.DISCONNECTED)
        }
    }

    private val isActive: Boolean get() = readerJob?.isActive == true && socket?.isConnected == true
}

class BtCommandException(message: String) : Exception(message)
class BtConnectionException(message: String, cause: Throwable) : Exception(message, cause)
class DisconnectedException : Exception("Bluetooth disconnected")
