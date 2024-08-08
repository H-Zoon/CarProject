package com.devidea.chevy.eventbus

import com.devidea.chevy.carsystem.CarEventModule
import com.devidea.chevy.carsystem.pid.PIDListData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus : IEventBusBehavior<Event> {
    private val _events = MutableSharedFlow<Event>()
    override val events = _events.asSharedFlow()
    override suspend fun post(event: Event) {
        _events.emit(event)
    }
}

sealed class Event {
    data class carStateEvent(val message: ByteArray) : Event()
    data class TPMSEvent(val message: ByteArray) : Event()
    data class controlEvent(val message: ByteArray) : Event()
}
