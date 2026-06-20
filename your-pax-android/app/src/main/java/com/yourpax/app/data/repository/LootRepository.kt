package com.yourpax.app.data.repository

import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.demo.DemoData

class LootRepository(private val comm: CommunicationManager = CommHolder.comm) {

    @Suppress("UNCHECKED_CAST")
    suspend fun getCredentials(): Result<List<CredentialFile>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoCredentials
        (comm.request("list_credentials_json", emptyMap(), List::class.java).getOrThrow() as List<CredentialFile>)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getLootFiles(): Result<List<LootFile>> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoLootFiles
        (comm.request("list_files", emptyMap(), List::class.java).getOrThrow() as List<LootFile>)
    }

    suspend fun getStoreData(): Result<StoreDataFull> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoStoreDataFull
        comm.request("store_data", emptyMap(), StoreDataFull::class.java).getOrThrow()
    }

    suspend fun downloadFile(path: String): Result<ByteArray> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching "Demo file content".toByteArray()
        comm.downloadFile(path).getOrThrow()
    }

    suspend fun downloadStoreFile(path: String): Result<ByteArray> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching "Demo store file".toByteArray()
        comm.downloadStoreFile(path).getOrThrow()
    }

    suspend fun getLogs(): Result<LogResponse> = runCatching {
        if (ConnectionState.isDemoMode) return@runCatching DemoData.demoLogResponse
        comm.request("get_logs", emptyMap(), LogResponse::class.java).getOrThrow()
    }
}
