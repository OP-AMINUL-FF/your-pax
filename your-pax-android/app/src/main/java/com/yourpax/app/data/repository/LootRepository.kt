package com.yourpax.app.data.repository

import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class LootRepository {
    private val api get() = RetrofitProvider.getApiService()

    suspend fun getCredentials(): Result<List<CredentialFile>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoCredentials
        val response = api.listCredentials()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get credentials: ${response.code()}")
    }

    suspend fun getLootFiles(): Result<List<LootFile>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoLootFiles
        val response = api.listFiles()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get loot files: ${response.code()}")
    }

    suspend fun getStoreData(): Result<StoreDataFull> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoStoreDataFull
        val response = api.getStoreData()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get store data: ${response.code()}")
    }

    suspend fun downloadFile(path: String): Result<ByteArray> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching "Demo file content".toByteArray()
        val response = api.downloadFile(path)
        if (response.isSuccessful) response.body()!!.bytes()
        else throw Exception("Failed to download file: ${response.code()}")
    }

    suspend fun downloadStoreFile(path: String): Result<ByteArray> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching "Demo store file".toByteArray()
        val response = api.downloadStoreFile(path)
        if (response.isSuccessful) response.body()!!.bytes()
        else throw Exception("Failed to download store file: ${response.code()}")
    }

    suspend fun getLogs(): Result<LogResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoLogResponse
        val response = api.getLogs()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get logs: ${response.code()}")
    }
}
