package com.yourpax.app.data.repository

import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class ConfigRepository(private val comm: CommunicationManager = CommHolder.comm) {

    suspend fun loadConfig(): Result<ConfigData> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoConfig
        comm.request("load_config", emptyMap(), ConfigData::class.java).getOrThrow()
    }

    suspend fun saveConfig(config: Map<String, Any>): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("save_config", config, ActionResponse::class.java).getOrThrow()
    }

    suspend fun restoreDefaultConfig(): Result<ConfigData> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoConfig
        comm.request("restore_default_config", emptyMap(), ConfigData::class.java).getOrThrow()
    }

    suspend fun reboot(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("reboot", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun shutdown(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("shutdown", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun restartService(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("restart_your_pax_service", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun clearFiles(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("clear_files", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun clearFilesLight(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("clear_files_light", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun initializeCsv(): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("initialize_csv", emptyMap(), ActionResponse::class.java).getOrThrow()
    }

    suspend fun switchMode(mode: String): Result<ActionResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoActionResponse
        comm.request("switch_mode", mapOf("mode" to mode), ActionResponse::class.java).getOrThrow()
    }

    suspend fun getModeConfig(): Result<Map<String, Any>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching mapOf("status" to "success", "connection_mode" to "web_app")
        comm.requestJson("get_mode_config", emptyMap()) { obj ->
            val mode = obj.get("connection_mode")?.asString ?: "web_app"
            mapOf("status" to "success", "connection_mode" to mode)
        }.getOrThrow()
    }
}
