package com.yourpax.app.data.comm

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yourpax.app.data.api.models.ActionResponse
import com.yourpax.app.data.api.models.BackupResponse
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class CachedCommManagerTest {

    private val gson = Gson()
    private lateinit var delegate: TestCommManager
    private lateinit var cached: CachedCommManager

    @Before
    fun setUp() {
        DataCache.invalidateAll()
        delegate = TestCommManager()
        cached = CachedCommManager(delegate)
    }

    @After
    fun tearDown() {
        DataCache.invalidateAll()
    }

    @Test
    fun `delegates request and caches on success`() = runTest {
        val data = JsonObject().apply { addProperty("value", "pong") }
        delegate.setValue(data)
        val r1 = cached.request("ping", emptyMap(), JsonObject::class.java)
        assertThat(r1.isSuccess).isTrue()
        assertThat(r1.getOrNull()?.get("value")?.asString).isEqualTo("pong")
        assertThat(delegate.callCount("ping")).isEqualTo(1)

        delegate.setFail()
        val r2 = cached.request("ping", emptyMap(), JsonObject::class.java)
        assertThat(r2.isSuccess).isTrue()
        assertThat(r2.getOrNull()?.get("value")?.asString).isEqualTo("pong")
        assertThat(delegate.callCount("ping")).isEqualTo(1)
    }

    @Test
    fun `delegates requestJson and caches`() = runTest {
        val json = gson.fromJson("""{"status":"ok","data":"hello"}""", JsonObject::class.java)
        delegate.setJsonValue(json)

        val r1 = cached.requestJson("test_cmd", emptyMap()) { it.get("data")?.asString ?: "" }
        assertThat(r1.isSuccess).isTrue()
        assertThat(r1.getOrNull()).isEqualTo("hello")
        assertThat(delegate.jsonCallCount("test_cmd")).isEqualTo(1)

        delegate.setJsonFail()
        val r2 = cached.requestJson("test_cmd", emptyMap()) { it.get("data")?.asString ?: "" }
        assertThat(r2.isSuccess).isTrue()
        assertThat(r2.getOrNull()).isEqualTo("hello")
        assertThat(delegate.jsonCallCount("test_cmd")).isEqualTo(1)
    }

    @Test
    fun `delegates downloadFile and caches`() = runTest {
        val data = "file content".toByteArray()
        delegate.setDownloadValue(data)

        val r1 = cached.downloadFile("test.txt")
        assertThat(r1.isSuccess).isTrue()
        assertThat(r1.getOrNull()).isEqualTo(data)
        assertThat(delegate.downloadCallCount("test.txt")).isEqualTo(1)

        delegate.setDownloadFail()
        val r2 = cached.downloadFile("test.txt")
        assertThat(r2.isSuccess).isTrue()
        assertThat(r2.getOrNull()).isEqualTo(data)
        assertThat(delegate.downloadCallCount("test.txt")).isEqualTo(1)
    }

    @Test
    fun `does not cache failures`() = runTest {
        delegate.setFail()
        val r1 = cached.request("fail_cmd", emptyMap(), JsonObject::class.java)
        assertThat(r1.isFailure).isTrue()

        delegate.setValue(JsonObject().apply { addProperty("status", "ok") })
        val r2 = cached.request("fail_cmd", emptyMap(), JsonObject::class.java)
        assertThat(r2.isSuccess).isTrue()
        assertThat(delegate.callCount("fail_cmd")).isEqualTo(2)
    }

    @Test
    fun `mutation commands invalidate cache`() = runTest {
        val data = JsonObject().apply { addProperty("x", "data") }
        delegate.setValue(data)
        cached.request("load_config", emptyMap(), JsonObject::class.java)
        assertThat(delegate.callCount("load_config")).isEqualTo(1)

        val r2 = cached.request("load_config", emptyMap(), JsonObject::class.java)
        assertThat(r2.isSuccess).isTrue()
        assertThat(delegate.callCount("load_config")).isEqualTo(1)

        delegate.setValue(ActionResponse("success", "done"))
        cached.request("save_config", mapOf("key" to "value"), ActionResponse::class.java)

        val newData = JsonObject().apply { addProperty("x", "new_data") }
        delegate.setValue(newData)
        cached.request("load_config", emptyMap(), JsonObject::class.java)
        assertThat(delegate.callCount("load_config")).isEqualTo(2)
    }

    @Test
    fun `backup delegates without caching`() = runTest {
        val expected = BackupResponse(status = "ok", url = "/backup", filename = "b.zip", message = "")
        delegate.setBackupValue(expected)

        val result = cached.backup()
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expected)
        assertThat(delegate.backupCallCount).isEqualTo(1)
    }

    @Test
    fun `restore invalidates all and delegates`() = runTest {
        DataCache.put("cached_key", emptyMap(), gson.fromJson("{}", JsonObject::class.java))
        delegate.setActionValue(ActionResponse("success", "done"))

        val result = cached.restore("data".toByteArray(), "backup.zip")
        assertThat(result.isSuccess).isTrue()
        assertThat(delegate.restoreCallCount).isEqualTo(1)
        assertThat(DataCache.get("cached_key", emptyMap())).isNull()
    }

    @Test
    fun `uploadPortal invalidates all and delegates`() = runTest {
        delegate.setActionValue(ActionResponse("success", "uploaded"))
        val result = cached.uploadPortal("<html></html>".toByteArray(), "portal.html")
        assertThat(result.isSuccess).isTrue()
        assertThat(delegate.uploadCallCount).isEqualTo(1)
    }

    @Test
    fun `connect delegates to inner`() = runTest {
        val result = cached.connect()
        assertThat(result).isTrue()
        assertThat(delegate.connectCalled).isTrue()
    }

    @Test
    fun `disconnect invalidates cache and delegates`() = runTest {
        DataCache.put("key", emptyMap(), gson.fromJson("{}", JsonObject::class.java))
        assertThat(DataCache.get("key", emptyMap())).isNotNull()

        cached.disconnect()
        assertThat(DataCache.get("key", emptyMap())).isNull()
        assertThat(delegate.disconnectCalled).isTrue()
    }

    @Test
    fun `isConnected delegates to inner`() {
        delegate.connected = false
        assertThat(cached.isConnected()).isFalse()
        delegate.connected = true
        assertThat(cached.isConnected()).isTrue()
    }

    @Test
    fun `getType delegates to inner`() {
        delegate.commType = CommType.BLUETOOTH
        assertThat(cached.getType()).isEqualTo(CommType.BLUETOOTH)
    }

    @Test
    fun `requestJson skips cache on failure`() = runTest {
        delegate.setJsonFail()
        val r1 = cached.requestJson("cmd", emptyMap()) { it }
        assertThat(r1.isFailure).isTrue()

        val json = gson.fromJson("""{"status":"ok"}""", JsonObject::class.java)
        delegate.setJsonValue(json)
        val r2 = cached.requestJson("cmd", emptyMap()) { it }
        assertThat(r2.isSuccess).isTrue()
        assertThat(delegate.jsonCallCount("cmd")).isEqualTo(2)
    }

    @Test
    fun `downloadStoreFile caches`() = runTest {
        val data = "store data".toByteArray()
        delegate.setDownloadValue(data)

        cached.downloadStoreFile("store.txt")
        assertThat(delegate.downloadCallCount("store.txt")).isEqualTo(1)

        delegate.setDownloadFail()
        cached.downloadStoreFile("store.txt")
        assertThat(delegate.downloadCallCount("store.txt")).isEqualTo(1)
    }

    @Test
    fun `onStatusChange delegates to inner`() {
        cached.onStatusChange?.invoke(ConnectionStatus.CONNECTED)
    }
}

private class TestCommManager : CommunicationManager {

    private val callCounts = mutableMapOf<String, Int>()
    private val jsonCallCounts = mutableMapOf<String, Int>()
    private val downloadCallCounts = mutableMapOf<String, Int>()

    private var successValue: Any? = null
    private var shouldSucceed = true
    private var jsonSuccessValue: JsonObject? = null
    private var jsonShouldSucceed = true
    private var downloadValue: ByteArray? = null
    private var downloadShouldSucceed = true

    var backupCallCount = 0
    var restoreCallCount = 0
    var uploadCallCount = 0
    var connectCalled = false
    var disconnectCalled = false
    var connected = true
    var commType = CommType.HTTP

    private var backupValue: BackupResponse? = null
    private var backupShouldSucceed = true
    private var actionValue: ActionResponse? = null
    private var actionShouldSucceed = true

    fun setValue(v: Any) { shouldSucceed = true; successValue = v }
    fun setFail() { shouldSucceed = false }
    fun callCount(cmd: String) = callCounts[cmd] ?: 0

    fun setJsonValue(v: JsonObject) { jsonShouldSucceed = true; jsonSuccessValue = v }
    fun setJsonFail() { jsonShouldSucceed = false }
    fun jsonCallCount(cmd: String) = jsonCallCounts[cmd] ?: 0

    fun setDownloadValue(v: ByteArray) { downloadShouldSucceed = true; downloadValue = v }
    fun setDownloadFail() { downloadShouldSucceed = false }
    fun downloadCallCount(path: String) = downloadCallCounts[path] ?: 0

    fun setBackupValue(v: BackupResponse) { backupShouldSucceed = true; backupValue = v }
    fun setActionValue(v: ActionResponse) { actionShouldSucceed = true; actionValue = v }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> request(cmd: String, params: Map<String, Any>, responseType: Class<T>): Result<T> {
        callCounts[cmd] = (callCounts[cmd] ?: 0) + 1
        return if (shouldSucceed) Result.success(successValue as T)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun <T> requestJson(cmd: String, params: Map<String, Any>, parser: (JsonObject) -> T): Result<T> {
        jsonCallCounts[cmd] = (jsonCallCounts[cmd] ?: 0) + 1
        return if (jsonShouldSucceed && jsonSuccessValue != null) Result.success(parser(jsonSuccessValue!!))
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun downloadFile(path: String): Result<ByteArray> {
        downloadCallCounts[path] = (downloadCallCounts[path] ?: 0) + 1
        return if (downloadShouldSucceed && downloadValue != null) Result.success(downloadValue!!)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun downloadStoreFile(path: String): Result<ByteArray> {
        downloadCallCounts[path] = (downloadCallCounts[path] ?: 0) + 1
        return if (downloadShouldSucceed && downloadValue != null) Result.success(downloadValue!!)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun backup(): Result<BackupResponse> {
        backupCallCount++
        return if (backupShouldSucceed && backupValue != null) Result.success(backupValue!!)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun restore(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        restoreCallCount++
        return if (actionShouldSucceed && actionValue != null) Result.success(actionValue!!)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun uploadPortal(fileBytes: ByteArray, fileName: String): Result<ActionResponse> {
        uploadCallCount++
        return if (actionShouldSucceed && actionValue != null) Result.success(actionValue!!)
        else Result.failure(Exception("mock failure"))
    }

    override suspend fun connect(): Boolean { connectCalled = true; return true }
    override suspend fun disconnect() { disconnectCalled = true }
    override fun isConnected(): Boolean = connected
    override fun getType(): CommType = commType
    override var onStatusChange: ((ConnectionStatus) -> Unit)? = null
}
