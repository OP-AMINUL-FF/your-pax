package com.yourpax.app.data.api

import com.yourpax.app.data.api.models.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface YourPaxApiService {

    @GET("/load_config")
    suspend fun loadConfig(): Response<ConfigData>

    @POST("/save_config")
    suspend fun saveConfig(@Body config: Map<String, @JvmSuppressWildcards Any>): Response<ActionResponse>

    @GET("/network_data_json")
    suspend fun getNetworkData(): Response<NetworkScanResponse>

    @GET("/netkb_data_json_full")
    suspend fun getNetKBData(): Response<NetKBResponse>

    @GET("/netkb_data_json")
    suspend fun getNetKBMeta(): Response<NetKBMetaResponse>

    @GET("/wifi_scan_advanced")
    suspend fun wifiScanAdvanced(): Response<List<WiFiNetwork>>

    @GET("/wifi_status")
    suspend fun wifiStatus(): Response<WiFiStatusResponse>

    @GET("/handshake_status")
    suspend fun handshakeStatus(): Response<AttackStatusResponse>

    @GET("/pmkid_status")
    suspend fun pmkidStatus(): Response<AttackStatusResponse>

    @GET("/oneshot_status")
    suspend fun oneshotStatus(): Response<AttackStatusResponse>

    @GET("/evil_ap_status")
    suspend fun evilApStatus(): Response<EvilApStatusResponse>

    @GET("/evil_clients")
    suspend fun evilClients(): Response<EvilClientsResponse>

    @GET("/loot_monitor_data")
    suspend fun lootMonitorData(): Response<LootMonitorData>

    @GET("/bluetooth_status")
    suspend fun bluetoothStatus(): Response<BluetoothStatus>

    @GET("/bluetooth_devices")
    suspend fun bluetoothDevices(): Response<BluetoothDevicesResponse>

    @GET("/list_credentials_json")
    suspend fun listCredentials(): Response<List<CredentialFile>>

    @GET("/list_files")
    suspend fun listFiles(): Response<List<LootFile>>

    @GET("/get_logs")
    suspend fun getLogs(): Response<ResponseBody>

    @GET("/store_data")
    suspend fun getStoreData(): Response<StoreDataFull>

    @GET("/screen.png")
    suspend fun getScreenImage(): Response<ResponseBody>

    @GET("/download_file")
    suspend fun downloadFile(@Query("path") path: String): Response<ResponseBody>

    @GET("/download_store")
    suspend fun downloadStoreFile(@Query("path") path: String): Response<ResponseBody>

    @GET("/list_interfaces")
    suspend fun listInterfaces(): Response<List<String>>

    @GET("/portal_list")
    suspend fun portalList(): Response<PortalListResponse>

    @GET("/conflict_status")
    suspend fun conflictStatus(): Response<ConflictStatus>

    @GET("/scan_targets")
    suspend fun scanTargets(@Query("iface") iface: String = "wlan0"): Response<ScanTargetsResponse>

    @GET("/get_web_delay")
    suspend fun getWebDelay(): Response<WebDelayResponse>

    @GET("/csrf_token")
    suspend fun getCsrfToken(): Response<CsrfResponse>

    @GET("/wpa_validate_status")
    suspend fun wpaValidateStatus(): Response<Map<String, @JvmSuppressWildcards Any>>

    @GET("/network_data")
    suspend fun getNetworkDataRaw(): Response<ResponseBody>

    @POST("/trigger_scan")
    suspend fun triggerScan(): Response<ActionResponse>

    @POST("/trigger_bruteforce")
    suspend fun triggerBruteforce(@Body params: Map<String, String>): Response<ActionResponse>

    @POST("/trigger_vulnscan")
    suspend fun triggerVulnScan(): Response<ActionResponse>

    @POST("/trigger_steal")
    suspend fun triggerSteal(): Response<ActionResponse>

    @POST("/stop_all")
    suspend fun stopAll(): Response<ActionResponse>

    @POST("/handshake_start")
    suspend fun handshakeStart(@Body params: Map<String, String>): Response<ActionResponse>

    @POST("/handshake_stop")
    suspend fun handshakeStop(): Response<ActionResponse>

    @POST("/pmkid_start")
    suspend fun pmkidStart(@Body params: Map<String, String>): Response<ActionResponse>

    @POST("/pmkid_stop")
    suspend fun pmkidStop(): Response<ActionResponse>

    @POST("/deauth_attack")
    suspend fun deauthAttack(@Body params: Map<String, @JvmSuppressWildcards Any>): Response<ActionResponse>

    @POST("/oneshot")
    suspend fun oneshot(@Body params: Map<String, @JvmSuppressWildcards Any>): Response<ActionResponse>

    @POST("/oneshot_stop")
    suspend fun oneshotStop(): Response<ActionResponse>

    @POST("/start_evil_ap")
    suspend fun startEvilAp(@Body params: Map<String, @JvmSuppressWildcards Any>): Response<ActionResponse>

    @POST("/stop_evil_ap")
    suspend fun stopEvilAp(): Response<ActionResponse>

    @POST("/stop_evil_clone")
    suspend fun stopEvilClone(): Response<ActionResponse>

    @POST("/connect_wifi")
    suspend fun connectWifi(@Body params: Map<String, @JvmSuppressWildcards Any>): Response<ActionResponse>

    @POST("/disconnect_wifi")
    suspend fun disconnectWifi(): Response<ActionResponse>

    @POST("/reboot")
    suspend fun reboot(): Response<ActionResponse>

    @POST("/shutdown")
    suspend fun shutdown(): Response<ActionResponse>

    @POST("/restart_your_pax_service")
    suspend fun restartService(): Response<ActionResponse>

    @POST("/stop_orchestrator")
    suspend fun stopOrchestrator(): Response<ActionResponse>

    @POST("/start_orchestrator")
    suspend fun startOrchestrator(): Response<ActionResponse>

    @POST("/clear_files")
    suspend fun clearFiles(): Response<ActionResponse>

    @POST("/clear_files_light")
    suspend fun clearFilesLight(): Response<ActionResponse>

    @POST("/initialize_csv")
    suspend fun initializeCsv(): Response<ActionResponse>

    @POST("/restore_default_config")
    suspend fun restoreDefaultConfig(): Response<ConfigData>

    @POST("/backup")
    suspend fun backup(): Response<BackupResponse>

    @POST("/restore")
    @Multipart
    suspend fun restore(@Part file: MultipartBody.Part): Response<ActionResponse>

    @POST("/execute_manual_attack")
    suspend fun executeManualAttack(@Body params: Map<String, String>): Response<ActionResponse>

    @POST("/upload_portal")
    @Multipart
    suspend fun uploadPortal(@Part file: MultipartBody.Part, @Part("filename") filename: okhttp3.RequestBody): Response<ActionResponse>

    @POST("/delete_portal")
    suspend fun deletePortal(@Body params: Map<String, String>): Response<ActionResponse>
}
