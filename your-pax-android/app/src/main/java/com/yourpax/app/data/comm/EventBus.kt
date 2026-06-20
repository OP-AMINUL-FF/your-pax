package com.yourpax.app.data.comm

import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class BtEvent(
    val event: String,
    val data: JsonObject
)

object EventBus {
    private val _events = MutableSharedFlow<BtEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BtEvent> = _events.asSharedFlow()

    fun emit(event: String, data: JsonObject) {
        _events.tryEmit(BtEvent(event, data))
    }
}
