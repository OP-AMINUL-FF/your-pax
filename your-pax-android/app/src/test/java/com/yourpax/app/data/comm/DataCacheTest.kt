package com.yourpax.app.data.comm

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataCacheTest {

    private val gson = Gson()

    @Before
    fun setUp() {
        DataCache.invalidateAll()
    }

    @After
    fun tearDown() {
        DataCache.invalidateAll()
    }

    @Test
    fun `put and get returns cached data`() = runTest {
        val json = gson.fromJson("""{"status":"ok","data":"test"}""", JsonObject::class.java)
        DataCache.put("ping", emptyMap(), json)

        val cached = DataCache.get("ping", emptyMap())
        assertNotNull(cached)
        assertEquals("ok", cached?.get("status")?.asString)
        assertEquals("test", cached?.get("data")?.asString)
    }

    @Test
    fun `get returns null for unknown key`() = runTest {
        assertNull(DataCache.get("unknown_cmd", emptyMap()))
    }

    @Test
    fun `invalidate removes entry`() = runTest {
        val json = gson.fromJson("""{"status":"ok"}""", JsonObject::class.java)
        DataCache.put("test_cmd", emptyMap(), json)
        assertNotNull(DataCache.get("test_cmd", emptyMap()))

        DataCache.invalidate("test_cmd", emptyMap())
        assertNull(DataCache.get("test_cmd", emptyMap()))
    }

    @Test
    fun `invalidateAll clears all entries`() = runTest {
        DataCache.put("cmd1", emptyMap(), gson.fromJson("{}", JsonObject::class.java))
        DataCache.put("cmd2", emptyMap(), gson.fromJson("{}", JsonObject::class.java))
        DataCache.invalidateAll()

        assertNull(DataCache.get("cmd1", emptyMap()))
        assertNull(DataCache.get("cmd2", emptyMap()))
    }

    @Test
    fun `params differentiate cache keys`() = runTest {
        val json1 = gson.fromJson("""{"data":"first"}""", JsonObject::class.java)
        val json2 = gson.fromJson("""{"data":"second"}""", JsonObject::class.java)

        DataCache.put("cmd", mapOf("id" to "1"), json1)
        DataCache.put("cmd", mapOf("id" to "2"), json2)

        val r1 = DataCache.get("cmd", mapOf("id" to "1"))
        val r2 = DataCache.get("cmd", mapOf("id" to "2"))

        assertEquals("first", r1?.get("data")?.asString)
        assertEquals("second", r2?.get("data")?.asString)
    }

    @Test
    fun `download cache put and get`() = runTest {
        val data = "hello world".toByteArray()
        DataCache.putDownload("test.txt", data)

        val cached = DataCache.getDownload("test.txt")
        assertNotNull(cached)
        assertArrayEquals(data, cached)
    }

    @Test
    fun `download cache miss returns null`() = runTest {
        assertNull(DataCache.getDownload("nonexistent.txt"))
    }

    @Test
    fun `invalidateMutations removes all entries`() = runTest {
        DataCache.put("save_config", mapOf("key" to "value"), gson.fromJson("{}", JsonObject::class.java))
        DataCache.put("load_config", emptyMap(), gson.fromJson("{}", JsonObject::class.java))
        DataCache.put("trigger_scan", emptyMap(), gson.fromJson("{}", JsonObject::class.java))

        DataCache.invalidateMutations()

        assertNull(DataCache.get("save_config", mapOf("key" to "value")))
        assertNull(DataCache.get("trigger_scan", emptyMap()))
        assertNull(DataCache.get("load_config", emptyMap()))
    }
}
