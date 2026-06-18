package com.yourpax.app.data.repository

import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class ConfigRepository {
    private val api get() = RetrofitProvider.getApiService()

    suspend fun loadConfig(): Result<ConfigData> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoConfig
        val response = api.loadConfig()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to load config: ${response.code()}")
    }

    suspend fun saveConfig(config: Map<String, Any>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.saveConfig(config)
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to save config: ${response.code()}")
    }

    suspend fun restoreDefaultConfig(): Result<ConfigData> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoConfig
        val response = api.restoreDefaultConfig()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to restore config: ${response.code()}")
    }

    suspend fun reboot(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.reboot()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Reboot failed: ${response.code()}")
    }

    suspend fun shutdown(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.shutdown()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Shutdown failed: ${response.code()}")
    }

    suspend fun restartService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.restartService()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Restart failed: ${response.code()}")
    }

    suspend fun clearFiles(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.clearFiles()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Clear files failed: ${response.code()}")
    }

    suspend fun clearFilesLight(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.clearFilesLight()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Clear files light failed: ${response.code()}")
    }

    suspend fun initializeCsv(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.initializeCsv()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Init CSV failed: ${response.code()}")
    }
}
