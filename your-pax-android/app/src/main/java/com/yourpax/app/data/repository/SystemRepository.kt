package com.yourpax.app.data.repository

import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class SystemRepository(private val comm: CommunicationManager = CommHolder.comm) {

    suspend fun backup(): Result<BackupResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching BackupResponse(
            status = "success", url = "/download_backup", filename = "backup.zip",
            message = "Demo backup created"
        )
        comm.backup().getOrThrow()
    }

    suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.restore(fileBytes, fileName).getOrThrow()
    }

    suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.uploadPortal(fileBytes, fileName).getOrThrow()
    }

    suspend fun deletePortal(name: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("delete_portal", mapOf("filename" to name), ActionResponse::class.java).getOrThrow()
    }

    suspend fun getWebDelay(): Result<Int> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching 5000
        val response = comm.request("get_web_delay", emptyMap(), WebDelayResponse::class.java).getOrThrow()
        response.webDelay
    }

    suspend fun executeManualAttack(ip: String, port: String, action: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("execute_manual_attack", mapOf("ip" to ip, "port" to port, "action" to action), ActionResponse::class.java).getOrThrow()
    }

    suspend fun executeCommand(params: Map<String, String>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("execute_manual_attack", params, ActionResponse::class.java).getOrThrow()
    }

    suspend fun startWebService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("start_web_service", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopWebService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("stop_web_service", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun startNapService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("start_nap_service", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopNapService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("stop_nap_service", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun startSppService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("start_spp_service", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun stopSppService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("stop_spp_service", emptyMap(), ActionResponse::class.java).getOrThrow()
    }
}
