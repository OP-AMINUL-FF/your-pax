package com.yourpax.app.data.repository

import com.google.common.truth.Truth.assertThat
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

class WiFiRepositoryTest {

    private lateinit var repo: WiFiRepository
    private lateinit var mockComm: WiFiMockComm

    @Before
    fun setUp() {
        ConnectionState.isDemoMode = false
        mockComm = WiFiMockComm()
        CommHolder.comm = mockComm
        repo = WiFiRepository(mockComm)
    }

    @After
    fun tearDown() {
        ConnectionState.isDemoMode = false
    }

    @Test
    fun `scanNetworks delegates to comm`() = runTest {
        val expected = listOf(
            WiFiNetwork(bssid = "AA:BB:CC:DD:EE:01", ssid = "TestNet", channel = "6", signal = "-45", wpa = true, wps = false)
        )
        mockComm.nextValue = expected

        val result = repo.scanNetworks()
        assertThat(result.isSuccess).isTrue()
        assertThat(mockComm.lastCmd).isEqualTo("wifi_scan_advanced")
    }

    @Test
    fun `scanNetworks returns demo data in demo mode`() = runTest {
        ConnectionState.isDemoMode = true
        val result = repo.scanNetworks()
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNotEmpty()
        assertThat(mockComm.lastCmd).isNull()
    }

    @Test
    fun `getWifiStatus delegates to comm`() = runTest {
        val expected = WiFiStatusResponse(connected = true, ssid = "MyWiFi", signal = "-50")
        mockComm.nextValue = expected

        val result = repo.getWifiStatus()
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.ssid).isEqualTo("MyWiFi")
        assertThat(mockComm.lastCmd).isEqualTo("wifi_status")
    }

    @Test
    fun `startHandshake passes correct params`() = runTest {
        mockComm.nextValue = ActionResponse("success", "started")
        val result = repo.startHandshake("AA:BB:CC:DD:EE:FF", "6", "test")
        assertThat(result.isSuccess).isTrue()
        assertThat(mockComm.lastCmd).isEqualTo("handshake_start")
        assertThat(mockComm.lastParams["bssid"]).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(mockComm.lastParams["channel"]).isEqualTo("6")
        assertThat(mockComm.lastParams["prefix"]).isEqualTo("test")
    }

    @Test
    fun `stopHandshake delegates`() = runTest {
        mockComm.nextValue = ActionResponse("success", "stopped")
        val result = repo.stopHandshake()
        assertThat(result.isSuccess).isTrue()
        assertThat(mockComm.lastCmd).isEqualTo("handshake_stop")
    }

    @Test
    fun `deauth passes all params`() = runTest {
        mockComm.nextValue = ActionResponse("success", "deauth")
        val result = repo.deauth("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66", 5, 1)
        assertThat(result.isSuccess).isTrue()
        assertThat(mockComm.lastCmd).isEqualTo("deauth_attack")
        assertThat(mockComm.lastParams["count"]).isEqualTo(5)
        assertThat(mockComm.lastParams["channel"]).isEqualTo(1)
    }

    @Test
    fun `connectWifi passes params`() = runTest {
        mockComm.nextValue = ActionResponse("success", "connected")
        val result = repo.connectWifi("MyWiFi", "secret123", true)
        assertThat(result.isSuccess).isTrue()
        assertThat(mockComm.lastCmd).isEqualTo("connect_wifi")
        assertThat(mockComm.lastParams["hidden"]).isEqualTo(true)
    }
}

@Suppress("UNCHECKED_CAST")
private class WiFiMockComm : CommunicationManager {

    var lastCmd: String? = null
    var lastParams: Map<String, Any> = emptyMap()
    var nextValue: Any? = null
    var shouldSucceed = true

    override suspend fun <T> request(cmd: String, params: Map<String, Any>, responseType: Class<T>): Result<T> {
        lastCmd = cmd
        lastParams = params
        return if (shouldSucceed) Result.success(nextValue as T)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun <T> requestJson(cmd: String, params: Map<String, Any>, parser: (JsonObject) -> T): Result<T> {
        lastCmd = cmd
        lastParams = params
        return if (shouldSucceed) Result.success(nextValue as T)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun downloadFile(path: String): Result<ByteArray> = Result.failure(Exception("no mock"))
    override suspend fun downloadStoreFile(path: String): Result<ByteArray> = Result.failure(Exception("no mock"))
    override suspend fun backup() = Result.failure<BackupResponse>(Exception("no mock"))
    override suspend fun restore(fileBytes: ByteArray, fileName: String) = Result.failure<ActionResponse>(Exception("no mock"))
    override suspend fun uploadPortal(fileBytes: ByteArray, fileName: String) = Result.failure<ActionResponse>(Exception("no mock"))
    override suspend fun connect(): Boolean = true
    override suspend fun disconnect() {}
    override fun isConnected(): Boolean = true
    override fun getType(): CommType = CommType.HTTP
    override var onStatusChange: ((ConnectionStatus) -> Unit)? = null
}
