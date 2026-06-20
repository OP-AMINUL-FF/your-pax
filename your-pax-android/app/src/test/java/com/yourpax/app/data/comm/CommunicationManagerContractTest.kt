package com.yourpax.app.data.comm

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yourpax.app.data.api.models.ActionResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Contract tests that any CommunicationManager implementation must pass.
 * Subclass and override [createManager] to test a specific implementation.
 */
abstract class CommunicationManagerContractTest {

    protected val gson = Gson()

    protected abstract fun createManager(): CommunicationManager

    @Test
    fun `request ping returns ok`() = runTest {
        val mgr = createManager()
        val result = mgr.request("ping", emptyMap(), String::class.java)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `requestJson ping returns data`() = runTest {
        val mgr = createManager()
        val result = mgr.requestJson("ping", emptyMap()) { it }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `request with params passes through correctly`() = runTest {
        val mgr = createManager()
        val result = mgr.request(
            "echo",
            mapOf("message" to "hello"),
            String::class.java
        )
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `unknown command returns failure`() = runTest {
        val mgr = createManager()
        val result = mgr.request("nonexistent_command_xyz", emptyMap(), String::class.java)
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `requestJson with custom parser works`() = runTest {
        val mgr = createManager()
        val result = mgr.requestJson("ping", emptyMap()) { json ->
            json.get("status")?.asString ?: "unknown"
        }
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("ok")
    }

    @Test
    fun `connect returns true`() = runTest {
        val mgr = createManager()
        val connected = mgr.connect()
        assertThat(connected).isTrue()
    }

    @Test
    fun `isConnected returns true after connect`() = runTest {
        val mgr = createManager()
        mgr.connect()
        assertThat(mgr.isConnected()).isTrue()
    }

    @Test
    fun `getType returns non-null`() = runTest {
        val mgr = createManager()
        assertThat(mgr.getType()).isNotNull()
    }

    @Test
    fun `disconnect does not throw`() = runTest {
        val mgr = createManager()
        mgr.connect()
        mgr.disconnect()
    }
}
