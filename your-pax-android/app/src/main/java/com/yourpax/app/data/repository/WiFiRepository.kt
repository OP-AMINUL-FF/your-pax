package com.yourpax.app.data.repository

import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class WiFiRepository {
    private val api get() = RetrofitProvider.getApiService()

    suspend fun scanNetworks(): Result<List<WiFiNetwork>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoNetworks
        val response = api.wifiScanAdvanced()
        if (response.isSuccessful) response.body()!!
        else throw Exception("WiFi scan failed: ${response.code()}")
    }

    suspend fun getWifiStatus(): Result<WiFiStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoWifiStatus
        val response = api.wifiStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("WiFi status failed: ${response.code()}")
    }

    suspend fun getHandshakeStatus(): Result<AttackStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoAttackStatus
        val response = api.handshakeStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Handshake status failed: ${response.code()}")
    }

    suspend fun getPmkidStatus(): Result<AttackStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoAttackStatus
        val response = api.pmkidStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("PMKID status failed: ${response.code()}")
    }

    suspend fun getOneshotStatus(): Result<AttackStatusResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoAttackStatus
        val response = api.oneshotStatus()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Oneshot status failed: ${response.code()}")
    }

    suspend fun startHandshake(bssid: String, channel: String, prefix: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.handshakeStart(mapOf(
            "bssid" to bssid, "channel" to channel, "prefix" to prefix
        ))
        if (response.isSuccessful) response.body()!!
        else throw Exception("Handshake start failed: ${response.code()}")
    }

    suspend fun stopHandshake(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.handshakeStop()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Handshake stop failed: ${response.code()}")
    }

    suspend fun startPmkid(bssid: String, channel: String, prefix: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.pmkidStart(mapOf(
            "bssid" to bssid, "channel" to channel, "prefix" to prefix
        ))
        if (response.isSuccessful) response.body()!!
        else throw Exception("PMKID start failed: ${response.code()}")
    }

    suspend fun stopPmkid(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.pmkidStop()
        if (response.isSuccessful) response.body()!!
        else throw Exception("PMKID stop failed: ${response.code()}")
    }

    suspend fun deauth(bssid: String, client: String, count: Int, channel: Int): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.deauthAttack(mapOf(
            "bssid" to bssid, "client" to client,
            "count" to count, "channel" to channel
        ))
        if (response.isSuccessful) response.body()!!
        else throw Exception("Deauth failed: ${response.code()}")
    }

    suspend fun startOneshot(params: Map<String, Any>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.oneshot(params)
        if (response.isSuccessful) response.body()!!
        else throw Exception("Oneshot start failed: ${response.code()}")
    }

    suspend fun stopOneshot(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.oneshotStop()
        if (response.isSuccessful) response.body()!!
        else throw Exception("Oneshot stop failed: ${response.code()}")
    }

    suspend fun connectWifi(ssid: String, password: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.connectWifi(mapOf("ssid" to ssid, "password" to password))
        if (response.isSuccessful) response.body()!!
        else throw Exception("WiFi connect failed: ${response.code()}")
    }

    suspend fun disconnectWifi(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        val response = api.disconnectWifi()
        if (response.isSuccessful) response.body()!!
        else throw Exception("WiFi disconnect failed: ${response.code()}")
    }
}
