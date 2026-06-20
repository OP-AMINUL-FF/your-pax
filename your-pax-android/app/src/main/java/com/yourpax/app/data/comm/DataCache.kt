package com.yourpax.app.data.comm

import com.google.gson.Gson
import com.google.gson.JsonObject

data class CacheEntry(
    val data: String,
    val timestamp: Long,
    val ttlMs: Long
)

object DataCache {
    private val cache = mutableMapOf<String, CacheEntry>()
    private val gson = Gson()

    private const val DEFAULT_TTL_MS = 30_000L
    private const val LONG_TTL_MS = 120_000L

    private val longTtlCommands = setOf(
        "load_config", "list_interfaces", "portal_list", "list_credentials_json",
        "list_files", "get_web_delay", "wpa_validate_status"
    )

    fun get(cmd: String, params: Map<String, Any>): JsonObject? {
        val key = buildKey(cmd, params)
        val entry = cache[key] ?: return null
        val expired = System.currentTimeMillis() - entry.timestamp > entry.ttlMs
        if (expired) {
            cache.remove(key)
            return null
        }
        return try {
            gson.fromJson(entry.data, JsonObject::class.java)
        } catch (_: Exception) { null }
    }

    fun put(cmd: String, params: Map<String, Any>, data: JsonObject) {
        val key = buildKey(cmd, params)
        val ttl = if (cmd in longTtlCommands) LONG_TTL_MS else DEFAULT_TTL_MS
        cache[key] = CacheEntry(gson.toJson(data), System.currentTimeMillis(), ttl)
    }

    fun putDownload(path: String, bytes: ByteArray) {
        downloadCache[path] = CacheEntry(
            gson.toJson(mapOf("data" to bytes)),
            System.currentTimeMillis(),
            LONG_TTL_MS
        )
    }

    fun getDownload(path: String): ByteArray? {
        val entry = downloadCache[path] ?: return null
        val expired = System.currentTimeMillis() - entry.timestamp > entry.ttlMs
        if (expired) {
            downloadCache.remove(path)
            return null
        }
        return try {
            val map = gson.fromJson(entry.data, Map::class.java)
            @Suppress("UNCHECKED_CAST")
            (map["data"] as? List<Int>)?.map { it.toByte() }?.toByteArray()
        } catch (_: Exception) { null }
    }

    fun invalidate(cmd: String, params: Map<String, Any> = emptyMap()) {
        cache.remove(buildKey(cmd, params))
    }

    fun invalidateAll() {
        cache.clear()
        downloadCache.clear()
    }

    fun invalidateMutations() {
        cache.clear()
        downloadCache.clear()
    }

    private val downloadCache = mutableMapOf<String, CacheEntry>()

    private fun buildKey(cmd: String, params: Map<String, Any>): String {
        return if (params.isEmpty()) cmd else "$cmd:${gson.toJson(params)}"
    }
}
