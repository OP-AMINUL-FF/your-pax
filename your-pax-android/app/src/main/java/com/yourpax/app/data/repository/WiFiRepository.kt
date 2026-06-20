package com.yourpax.app.data.repository

import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class WiFiRepository(private val comm: CommunicationManager = CommHolder.comm) {

    @Suppress("UNCHECKED_CAST")
    suspend fun scanNetworks(): Result<List<WiFiNetwork>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoNetworks
        (comm.request("wifi_scan_advanced", emptyMap(), List::class.java).getOrThrow() as List<WiFiNetwork>)
    }

    suspend fun getWifiStatus(): Result<WiFiStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoWifiStatus
        comm.request("wifi_status", emptyMap(), WiFiStatusResponse::class.java).getOrThrow()
    }

    suspend fun getHandshakeStatus(): Result<AttackStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoAttackStatus
        comm.request("handshake_status", emptyMap(), AttackStatusResponse::class.java).getOrThrow()
    }

    suspend fun getPmkidStatus(): Result<AttackStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoAttackStatus
        comm.request("pmkid_status", emptyMap(), AttackStatusResponse::class.java).getOrThrow()
    }

    suspend fun getOneshotStatus(): Result<AttackStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoAttackStatus
        comm.request("oneshot_status", emptyMap(), AttackStatusResponse::class.java).getOrThrow()
    }

    suspend fun startHandshake(bssid: String, channel: String, prefix: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("handshake_start", mapOf("bssid" to bssid, "channel" to channel, "prefix" to prefix), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopHandshake(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("handshake_stop", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun startPmkid(bssid: String, channel: String, prefix: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("pmkid_start", mapOf("bssid" to bssid, "channel" to channel, "prefix" to prefix), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopPmkid(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("pmkid_stop", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun deauth(bssid: String, client: String, count: Int, channel: Int): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("deauth_attack", mapOf("bssid" to bssid, "client" to client, "count" to count, "channel" to channel), ActionResponse::class.java).getOrThrow()
    }

    suspend fun startOneshot(params: Map<String, Any>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("oneshot", params, ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopOneshot(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("oneshot_stop", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun connectWifi(ssid: String, password: String, hidden: Boolean = false): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("connect_wifi", mapOf("ssid" to ssid, "password" to password, "hidden" to hidden), ActionResponse::class.java).getOrThrow()
    }

    suspend fun disconnectWifi(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("disconnect_wifi", emptyMap(), ActionResponse::class.java).getOrThrow()
    }
}
