package com.devidea.chevy.eventbus

import kotlinx.coroutines.flow.SharedFlow

interface IEventBusBehavior<T> {
    val events: SharedFlow<T>
    suspend fun post(event: T)
}