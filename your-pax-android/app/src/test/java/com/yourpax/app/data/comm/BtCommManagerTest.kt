package com.yourpax.app.data.comm

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class BtCommManagerTest {

    private val testAddress = "00:11:22:33:44:55"
    private val mgr = BtCommManager(testAddress)

    @After
    fun tearDown() = runTest {
        mgr.disconnect()
    }

    @Test
    fun `getType returns BLUETOOTH`() {
        assertThat(mgr.getType()).isEqualTo(CommType.BLUETOOTH)
    }

    @Test
    fun `isConnected returns false before connect`() {
        assertThat(mgr.isConnected()).isFalse()
    }

    @Test
    fun `disconnect does not throw when not connected`() = runTest {
        mgr.disconnect()
    }

    @Test
    fun `request returns failure when not connected`() = runTest {
        val result = mgr.request("ping", emptyMap(), String::class.java)
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `requestJson returns failure when not connected`() = runTest {
        val result = mgr.requestJson("ping", emptyMap()) { it }
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `downloadFile returns failure when not connected`() = runTest {
        val result = mgr.downloadFile("test.txt")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `downloadStoreFile returns failure when not connected`() = runTest {
        val result = mgr.downloadStoreFile("store.txt")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `backup returns failure when not connected`() = runTest {
        val result = mgr.backup()
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `restore returns failure when not connected`() = runTest {
        val result = mgr.restore("data".toByteArray(), "backup.zip")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `uploadPortal returns failure when not connected`() = runTest {
        val result = mgr.uploadPortal("<html></html>".toByteArray(), "portal.html")
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `onStatusChange is null by default`() {
        assertThat(mgr.onStatusChange).isNull()
    }

    @Test
    fun `constructor accepts custom coroutine context`() {
        val custom = BtCommManager(testAddress, Dispatchers.Unconfined)
        assertThat(custom.getType()).isEqualTo(CommType.BLUETOOTH)
        runBlocking { custom.disconnect() }
    }
}
