package com.yourpax.app.data.repository

import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class NetworkRepository(private val comm: CommunicationManager = CommHolder.comm) {

    suspend fun getNetworkData(): Result<NetworkScanResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoScanResponse
        comm.request("network_data_json", emptyMap(), NetworkScanResponse::class.java).getOrThrow()
    }

    suspend fun getNetKBData(): Result<NetKBResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoNetKBResponse
        comm.request("netkb_data_json_full", emptyMap(), NetKBResponse::class.java).getOrThrow()
    }

    suspend fun getNetKBMeta(): Result<NetKBMetaResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching NetKBMetaResponse()
        comm.request("netkb_data_json", emptyMap(), NetKBMetaResponse::class.java).getOrThrow()
    }

    suspend fun triggerScan(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("trigger_scan", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun triggerBruteforce(protocol: String = "all"): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("trigger_bruteforce", mapOf("protocol" to protocol), ActionResponse::class.java).getOrThrow()
    }

    suspend fun triggerVulnScan(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("trigger_vulnscan", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun triggerSteal(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("trigger_steal", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopAll(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("stop_all", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun startOrchestrator(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("start_orchestrator", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopOrchestrator(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("stop_orchestrator", emptyMap(), ActionResponse::class.java).getOrThrow()
    }
}
