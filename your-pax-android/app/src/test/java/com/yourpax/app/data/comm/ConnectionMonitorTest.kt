package com.yourpax.app.data.comm

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import com.yourpax.app.data.api.models.ActionResponse
import com.yourpax.app.data.api.models.BackupResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionMonitorTest {

    private val testScope = TestScope()
    private lateinit var monitor: ConnectionMonitor
    private lateinit var mockComm: MonitorMockComm

    @Before
    fun setUp() {
        mockComm = MonitorMockComm()
        monitor = ConnectionMonitor(mockComm, testScope)
    }

    @After
    fun tearDown() {
        monitor.stop()
    }

    @Test
    fun `status starts as CONNECTED`() {
        assertThat(monitor.status.value).isEqualTo(ConnectionStatus.CONNECTED)
    }

    @Test
    fun `reconnectNow returns true on success`() = testScope.runTest {
        mockComm.commType = CommType.BLUETOOTH
        mockComm.connectResult = true

        val ok = monitor.reconnectNow()
        assertThat(ok).isTrue()
        assertThat(mockComm.connectCalled).isTrue()
        assertThat(mockComm.disconnectCalled).isTrue()
        monitor.stop()
    }

    @Test
    fun `reconnectNow returns false on failure`() = testScope.runTest {
        mockComm.commType = CommType.BLUETOOTH
        mockComm.connectResult = false

        val ok = monitor.reconnectNow()
        assertThat(ok).isFalse()
    }

    @Test
    fun `stop cancels the job`() = testScope.runTest {
        mockComm.commType = CommType.BLUETOOTH
        monitor.start()
        monitor.stop()
        val prev = mockComm.pingCount
        testScope.advanceTimeBy(30_000)
        testScope.runCurrent()
        assertThat(mockComm.pingCount).isEqualTo(prev)
    }
}

private class MonitorMockComm : CommunicationManager {

    var commType = CommType.HTTP
    var pingCount = 0
    var pingShouldSucceed = true
    var connectResult = true
    var connectCalled = false
    var disconnectCalled = false

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> request(cmd: String, params: Map<String, Any>, responseType: Class<T>): Result<T> {
        if (cmd == "ping") pingCount++
        return if (pingShouldSucceed) Result.success("pong" as T)
        else Result.failure(Exception("fail"))
    }

    override suspend fun <T> requestJson(cmd: String, params: Map<String, Any>, parser: (JsonObject) -> T): Result<T> {
        @Suppress("UNCHECKED_CAST")
        return Result.success(parser(JsonObject()) as T)
    }

    override suspend fun downloadFile(path: String): Result<ByteArray> = Result.failure(Exception())
    override suspend fun downloadStoreFile(path: String): Result<ByteArray> = Result.failure(Exception())
    override suspend fun backup(): Result<BackupResponse> = Result.failure(Exception())
    override suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse> = Result.failure(Exception())
    override suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse> = Result.failure(Exception())
    override suspend fun connect(): Boolean { connectCalled = true; return connectResult }
    override suspend fun disconnect() { disconnectCalled = true }
    override fun isConnected(): Boolean = true
    override fun getType(): CommType = commType
    override var onStatusChange: ((ConnectionStatus) -> Unit)? = null
}
