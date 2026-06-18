package com.yourpax.app.data.repository

import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class EvilApRepository {
    private val api get() = RetrofitProvider.getApiService()

    suspend fun getStatus(): Result<EvilApStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoEvilApStatus
        val response = api.evilApStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get Evil AP status: ${response.code()}")
    }

    suspend fun getClients(): Result<EvilClientsResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoEvilClients
        val response = api.evilClients()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get Evil AP clients: ${response.code()}")
    }

    suspend fun getMonitorData(): Result<LootMonitorData> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoLootMonitor
        val response = api.lootMonitorData()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get monitor data: ${response.code()}")
    }

    suspend fun listInterfaces(): Result<List<String>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching listOf("wlan0", "wlan1", "wlan0mon")
        val response = api.listInterfaces()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to list interfaces: ${response.code()}")
    }

    suspend fun listPortals(): Result<PortalListResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoPortalList
        val response = api.portalList()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to list portals: ${response.code()}")
    }

    suspend fun scanTargets(iface: String = "wlan0"): Result<List<ScanTargetResult>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoScanTargets
        val response = api.scanTargets(iface)
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to scan targets: ${response.code()}")
    }

    suspend fun startAp(params: Map<String, Any>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.startEvilAp(params)
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to start Evil AP: ${response.code()}")
    }

    suspend fun stopAp(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.stopEvilAp()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to stop Evil AP: ${response.code()}")
    }

    suspend fun stopClone(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.stopEvilClone()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to stop clone: ${response.code()}")
    }

    suspend fun getConflictStatus(): Result<ConflictStatus> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching ConflictStatus()
        val response = api.conflictStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get conflict status: ${response.code()}")
    }

    suspend fun wpaValidateStatus(): Result<Map<String, Any>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching mapOf("status" to "idle")
        val response = api.wpaValidateStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Failed to get WPA validate status: ${response.code()}")
    }
}
