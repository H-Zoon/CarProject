package com.devidea.chevy.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    suspend fun post(event: Event) {
        _events.emit(event)
    }
}

sealed class Event {
    data class carStateEvent(val message: ByteArray) : Event()
    data class TPMSEvent(val message: ByteArray) : Event()
    data class controlEvent(val message: ByteArray) : Event()
}