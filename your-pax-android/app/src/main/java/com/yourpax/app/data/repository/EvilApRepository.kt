package com.yourpax.app.data.repository

import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class EvilApRepository(private val comm: CommunicationManager = CommHolder.comm) {

    suspend fun getStatus(): Result<EvilApStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoEvilApStatus
        comm.request("evil_ap_status", emptyMap(), EvilApStatusResponse::class.java).getOrThrow()
    }

    suspend fun getClients(): Result<EvilClientsResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoEvilClients
        comm.request("evil_clients", emptyMap(), EvilClientsResponse::class.java).getOrThrow()
    }

    suspend fun getMonitorData(): Result<LootMonitorData> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoLootMonitor
        comm.request("loot_monitor_data", emptyMap(), LootMonitorData::class.java).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun listInterfaces(): Result<List<String>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching listOf("wlan0", "wlan1", "wlan0mon")
        (comm.request("list_interfaces", emptyMap(), List::class.java).getOrThrow() as List<String>)
    }

    suspend fun listPortals(): Result<PortalListResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoPortalList
        comm.request("portal_list", emptyMap(), PortalListResponse::class.java).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun scanTargets(iface: String = "wlan0"): Result<List<ScanTargetResult>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoScanTargets
        val response = comm.request("scan_targets", mapOf("iface" to iface), ScanTargetsResponse::class.java).getOrThrow()
        response.networks
    }

    suspend fun startAp(params: Map<String, Any>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("start_evil_ap", params, ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopAp(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("stop_evil_ap", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopClone(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("stop_evil_clone", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun getConflictStatus(): Result<ConflictStatus> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching ConflictStatus()
        comm.request("conflict_status", emptyMap(), ConflictStatus::class.java).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun wpaValidateStatus(): Result<Map<String, Any>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching mapOf("status" to "idle")
        (comm.request("wpa_validate_status", emptyMap(), Map::class.java).getOrThrow() as Map<String, Any>)
    }
}
