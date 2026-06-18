package com.yourpax.app.data.repository

import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class NetworkRepository {
    private val api get() = RetrofitProvider.getApiService()

    suspend fun getNetworkData(): Result<NetworkScanResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoScanResponse
        val response = api.getNetworkData()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get network data: ${response.code()}")
    }

    suspend fun getNetKBData(): Result<NetKBResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoNetKBResponse
        val response = api.getNetKBData()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get NetKB data: ${response.code()}")
    }

    suspend fun getNetKBMeta(): Result<NetKBMetaResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching NetKBMetaResponse()
        val response = api.getNetKBMeta()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get NetKB meta: ${response.code()}")
    }

    suspend fun triggerScan(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.triggerScan()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Scan trigger failed: ${response.code()}")
    }

    suspend fun triggerBruteforce(protocol: String = "all"): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.triggerBruteforce(mapOf("protocol" to protocol))
        if (response.isSuccessful) response.body()!!
        else throw Exception("Bruteforce trigger failed: ${response.code()}")
    }

    suspend fun triggerVulnScan(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.triggerVulnScan()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Vuln scan trigger failed: ${response.code()}")
    }

    suspend fun triggerSteal(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.triggerSteal()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Steal trigger failed: ${response.code()}")
    }

    suspend fun stopAll(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.stopAll()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Stop all failed: ${response.code()}")
    }

    suspend fun startOrchestrator(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.startOrchestrator()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Start orchestrator failed: ${response.code()}")
    }

    suspend fun stopOrchestrator(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.stopOrchestrator()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Stop orchestrator failed: ${response.code()}")
    }
}
