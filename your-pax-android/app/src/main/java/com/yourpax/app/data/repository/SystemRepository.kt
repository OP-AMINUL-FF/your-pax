package com.yourpax.app.data.repository

import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class SystemRepository {
    private val api get() = RetrofitProvider.getApiService()

    suspend fun backup(): Result<BackupResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching BackupResponse(
            status = "success", url = "/download_backup", filename = "backup.zip",
            message = "Demo backup created"
        )
        val response = api.backup()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Backup failed: ${response.code()}")
    }

    suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val reqBody = fileBytes.toRequestBody("application/zip".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, reqBody)
        val response = api.restore(part)
        if (response.isSuccessful) response.body()!!
        else throw Exception("Restore failed: ${response.code()}")
    }

    suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val reqBody = fileBytes.toRequestBody("text/html".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, reqBody)
        val nameBody = fileName.toRequestBody("text/plain".toMediaTypeOrNull())
        val response = api.uploadPortal(part, nameBody)
        if (response.isSuccessful) response.body()!!
        else throw Exception("Upload failed: ${response.code()}")
    }

    suspend fun deletePortal(name: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.deletePortal(mapOf("filename" to name))
        if (response.isSuccessful) response.body()!!
        else throw Exception("Delete portal failed: ${response.code()}")
    }

    suspend fun getWebDelay(): Result<Int> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching 5000
        val response = api.getWebDelay()
        if (response.isSuccessful) response.body()!!.webDelay
        else throw Exception("Failed to get web delay: ${response.code()}")
    }

    suspend fun executeManualAttack(ip: String, port: String, action: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.executeManualAttack(mapOf("ip" to ip, "port" to port, "action" to action))
        if (response.isSuccessful) response.body()!!
        else throw Exception("Manual attack failed: ${response.code()}")
    }

    suspend fun executeCommand(params: Map<String, String>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.executeManualAttack(params)
        if (response.isSuccessful) response.body()!!
        else throw Exception("Command execution failed: ${response.code()}")
    }
}
