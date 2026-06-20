package com.yourpax.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.CommunicationManager
import com.yourpax.app.data.comm.CommType
import com.yourpax.app.data.comm.ConnectionStatus
import com.yourpax.app.data.demo.ConnectionState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class RepositoryContractTest {

    private lateinit var mockComm: RepoMockComm

    @Before
    fun setUp() {
        ConnectionState.isDemoMode = false
        mockComm = RepoMockComm()
        CommHolder.comm = mockComm
    }

    @After
    fun tearDown() {
        ConnectionState.isDemoMode = false
    }

    @Test
    fun `LootRepository getCredentials delegates`() = runTest {
        val repo = LootRepository(mockComm)
        mockComm.nextResult = Result.success(emptyList<CredentialFile>())

        val result = repo.getCredentials()
        assertThat(result.isSuccess).isTrue()
        assertThat(mockComm.lastCmd).isEqualTo("list_credentials_json")
    }

    @Test
    fun `LootRepository getLootFiles delegates`() = runTest {
        val repo = LootRepository(mockComm)
        mockComm.nextResult = Result.success(emptyList<LootFile>())

        repo.getLootFiles()
        assertThat(mockComm.lastCmd).isEqualTo("list_files")
    }

    @Test
    fun `LootRepository getStoreData delegates`() = runTest {
        val repo = LootRepository(mockComm)
        mockComm.nextResult = Result.success(StoreDataFull())

        repo.getStoreData()
        assertThat(mockComm.lastCmd).isEqualTo("store_data")
    }

    @Test
    fun `LootRepository downloadFile delegates`() = runTest {
        val repo = LootRepository(mockComm)
        mockComm.nextDownloadResult = Result.success("data".toByteArray())

        repo.downloadFile("test.txt")
        assertThat(mockComm.downloadLastPath).isEqualTo("test.txt")
    }

    @Test
    fun `ConfigRepository loadConfig delegates`() = runTest {
        val repo = ConfigRepository(mockComm)
        mockComm.nextResult = Result.success(ConfigData())

        repo.loadConfig()
        assertThat(mockComm.lastCmd).isEqualTo("load_config")
    }

    @Test
    fun `ConfigRepository saveConfig delegates`() = runTest {
        val repo = ConfigRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "saved"))

        val input = mapOf<String, Any>("key" to "value")
        repo.saveConfig(input)
        assertThat(mockComm.lastCmd).isEqualTo("save_config")
    }

    @Test
    fun `ConfigRepository reboot delegates`() = runTest {
        val repo = ConfigRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "rebooting"))

        repo.reboot()
        assertThat(mockComm.lastCmd).isEqualTo("reboot")
    }

    @Test
    fun `ConfigRepository shutdown delegates`() = runTest {
        val repo = ConfigRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "shutdown"))

        repo.shutdown()
        assertThat(mockComm.lastCmd).isEqualTo("shutdown")
    }

    @Test
    fun `ConfigRepository restoreDefaults delegates`() = runTest {
        val repo = ConfigRepository(mockComm)
        mockComm.nextResult = Result.success(ConfigData())

        repo.restoreDefaultConfig()
        assertThat(mockComm.lastCmd).isEqualTo("restore_default_config")
    }

    @Test
    fun `EvilApRepository getStatus delegates`() = runTest {
        val repo = EvilApRepository(mockComm)
        mockComm.nextResult = Result.success(EvilApStatusResponse(running = false, mode = "basic"))

        repo.getStatus()
        assertThat(mockComm.lastCmd).isEqualTo("evil_ap_status")
    }

    @Test
    fun `EvilApRepository getClients delegates`() = runTest {
        val repo = EvilApRepository(mockComm)
        mockComm.nextResult = Result.success(EvilClientsResponse(emptyList()))

        repo.getClients()
        assertThat(mockComm.lastCmd).isEqualTo("evil_clients")
    }

    @Test
    fun `EvilApRepository startEvilAp delegates`() = runTest {
        val repo = EvilApRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "started"))

        repo.startAp(mapOf("ssid" to "FreeWiFi", "channel" to 6))
        assertThat(mockComm.lastCmd).isEqualTo("start_evil_ap")
    }

    @Test
    fun `EvilApRepository stopEvilAp delegates`() = runTest {
        val repo = EvilApRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "stopped"))

        repo.stopAp()
        assertThat(mockComm.lastCmd).isEqualTo("stop_evil_ap")
    }

    @Test
    fun `NetworkRepository triggerScan delegates`() = runTest {
        val repo = NetworkRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "scanning"))

        repo.triggerScan()
        assertThat(mockComm.lastCmd).isEqualTo("trigger_scan")
    }

    @Test
    fun `NetworkRepository getNetworkData delegates`() = runTest {
        val repo = NetworkRepository(mockComm)
        mockComm.nextResult = Result.success(NetworkScanResponse())

        repo.getNetworkData()
        assertThat(mockComm.lastCmd).isEqualTo("network_data_json")
    }

    @Test
    fun `NetworkRepository getNetKBMeta delegates`() = runTest {
        val repo = NetworkRepository(mockComm)
        mockComm.nextResult = Result.success(NetKBMetaResponse())

        repo.getNetKBMeta()
        assertThat(mockComm.lastCmd).isEqualTo("netkb_data_json")
    }

    @Test
    fun `NetworkRepository stopAll delegates`() = runTest {
        val repo = NetworkRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "stopped"))

        repo.stopAll()
        assertThat(mockComm.lastCmd).isEqualTo("stop_all")
    }

    @Test
    fun `SystemRepository backup delegates`() = runTest {
        val repo = SystemRepository(mockComm)
        mockComm.nextBackupResult = Result.success(
            BackupResponse(status = "ok", url = "/backup", filename = "b.zip", message = "")
        )

        repo.backup()
        assertThat(mockComm.backupCalled).isTrue()
    }

    @Test
    fun `SystemRepository restore delegates`() = runTest {
        val repo = SystemRepository(mockComm)
        mockComm.nextResult = Result.success(ActionResponse("success", "restored"))

        repo.restore("data".toByteArray(), "backup.zip")
        assertThat(mockComm.restoreLastBytes).isNotNull()
        assertThat(mockComm.restoreLastFileName).isEqualTo("backup.zip")
    }

    @Test
    fun `BluetoothRepository getStatus delegates`() = runTest {
        val repo = BluetoothRepository(mockComm)
        mockComm.nextResult = Result.success(BluetoothStatus())

        repo.getBluetoothStatus()
        assertThat(mockComm.lastCmd).isEqualTo("bluetooth_status")
    }

    @Test
    fun `BluetoothRepository getDevices delegates`() = runTest {
        val repo = BluetoothRepository(mockComm)
        mockComm.nextResult = Result.success(
            BluetoothDevicesResponse(devices = emptyList())
        )

        repo.getBluetoothDevices()
        assertThat(mockComm.lastCmd).isEqualTo("bluetooth_devices")
    }

    @Test
    fun `all repositories use demo mode data`() = runTest {
        ConnectionState.isDemoMode = true

        assertThat(WiFiRepository(mockComm).scanNetworks().isSuccess).isTrue()
        assertThat(LootRepository(mockComm).getCredentials().isSuccess).isTrue()
        assertThat(LootRepository(mockComm).getLootFiles().isSuccess).isTrue()
        assertThat(LootRepository(mockComm).getStoreData().isSuccess).isTrue()
        assertThat(ConfigRepository(mockComm).loadConfig().isSuccess).isTrue()
        assertThat(EvilApRepository(mockComm).getStatus().isSuccess).isTrue()
        assertThat(NetworkRepository(mockComm).getNetworkData().isSuccess).isTrue()
        assertThat(SystemRepository(mockComm).backup().isSuccess).isTrue()
        assertThat(BluetoothRepository(mockComm).getBluetoothStatus().isSuccess).isTrue()
    }

    @Test
    fun `repositories default to CommHolder comm`() {
        assertThat(WiFiRepository().let { true }).isTrue()
        assertThat(LootRepository().let { true }).isTrue()
        assertThat(ConfigRepository().let { true }).isTrue()
        assertThat(EvilApRepository().let { true }).isTrue()
        assertThat(NetworkRepository().let { true }).isTrue()
        assertThat(SystemRepository().let { true }).isTrue()
        assertThat(BluetoothRepository().let { true }).isTrue()
    }
}

private class RepoMockComm : CommunicationManager {

    var lastCmd: String? = null
    var lastParams: Map<String, Any> = emptyMap()
    var nextResult: Result<Any> = Result.failure(Exception("no mock set"))
    var nextDownloadResult: Result<ByteArray> = Result.failure(Exception("no mock"))
    var nextBackupResult: Result<BackupResponse> = Result.failure(Exception("no mock"))
    var downloadLastPath: String? = null
    var backupCalled = false
    var restoreLastBytes: ByteArray? = null
    var restoreLastFileName: String? = null

    override suspend fun <T> request(cmd: String, params: Map<String, Any>, responseType: Class<T>): Result<T> {
        lastCmd = cmd
        lastParams = params
        @Suppress("UNCHECKED_CAST")
        return nextResult as Result<T>
    }

    override suspend fun <T> requestJson(cmd: String, params: Map<String, Any>, parser: (JsonObject) -> T): Result<T> {
        lastCmd = cmd
        lastParams = params
        @Suppress("UNCHECKED_CAST")
        return nextResult as Result<T>
    }

    override suspend fun downloadFile(path: String): Result<ByteArray> {
        downloadLastPath = path
        return nextDownloadResult
    }

    override suspend fun downloadStoreFile(path: String): Result<ByteArray> {
        downloadLastPath = path
        return nextDownloadResult
    }

    override suspend fun backup(): Result<BackupResponse> {
        backupCalled = true
        return nextBackupResult
    }

    override suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        restoreLastBytes = fileBytes
        restoreLastFileName = fileName
        @Suppress("UNCHECKED_CAST")
        return nextResult as Result<ActionResponse>
    }

    override suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        @Suppress("UNCHECKED_CAST")
        return nextResult as Result<ActionResponse>
    }

    override suspend fun connect(): Boolean = true
    override suspend fun disconnect() {}
    override fun isConnected(): Boolean = true
    override fun getType(): CommType = CommType.HTTP
    override var onStatusChange: ((ConnectionStatus) -> Unit)? = null
}
