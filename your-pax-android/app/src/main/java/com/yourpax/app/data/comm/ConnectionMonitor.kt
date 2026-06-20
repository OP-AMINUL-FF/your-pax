package com.yourpax.app.data.comm

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionMonitor(
    private val comm: CommunicationManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val _status = MutableStateFlow(ConnectionStatus.CONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private var job: Job? = null
    private var missedHeartbeats = 0

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 10000L
        private const val MAX_MISSED_HEARTBEATS = 3
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (comm.getType() == CommType.HTTP) continue
                val ok = try {
                    comm.request("ping", emptyMap(), String::class.java).isSuccess
                } catch (_: Exception) {
                    false
                }
                if (ok) {
                    missedHeartbeats = 0
                    _status.value = ConnectionStatus.CONNECTED
                } else {
                    missedHeartbeats++
                    if (missedHeartbeats >= MAX_MISSED_HEARTBEATS) {
                        _status.value = ConnectionStatus.DISCONNECTED
                        reconnect()
                    }
                }
            }
        }
    }

    private suspend fun reconnect() {
        _status.value = ConnectionStatus.RECONNECTING
        var delay = INITIAL_RECONNECT_DELAY_MS
        for (attempt in 1..MAX_RECONNECT_ATTEMPTS) {
            delay(delay)
            val ok = try {
                comm.disconnect()
                comm.connect()
            } catch (_: Exception) {
                false
            }
            if (ok) {
                missedHeartbeats = 0
                _status.value = ConnectionStatus.CONNECTED
                return
            }
            delay = (delay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        }
        _status.value = ConnectionStatus.DISCONNECTED
    }

    suspend fun reconnectNow(): Boolean {
        job?.cancel()
        missedHeartbeats = 0
        _status.value = ConnectionStatus.RECONNECTING
        val ok = try {
            comm.disconnect()
            comm.connect()
        } catch (_: Exception) {
            false
        }
        _status.value = if (ok) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
        if (ok) start()
        return ok
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
