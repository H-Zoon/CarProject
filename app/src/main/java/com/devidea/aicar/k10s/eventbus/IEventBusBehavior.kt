package com.devidea.aicar.k10s.eventbus

import kotlinx.coroutines.flow.SharedFlow

interface IEventBusBehavior<T> {
    val events: SharedFlow<T>
    suspend fun post(event: T)
}