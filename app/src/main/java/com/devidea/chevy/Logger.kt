package com.devidea.chevy

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object Logger {
    private const val NAME = "chevy"

    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    fun v(
        tag: String? = null,
        shouldUpdate: Boolean = false,
        trace: () -> String
    ) {
        val message = trace()
        Log.v(tag ?: NAME, message)
        if (shouldUpdate)
            _logMessages.update { it + "V/${tag ?: NAME}: $message" }
    }

    fun i(
        tag: String? = null,
        shouldUpdate: Boolean = false,
        trace: () -> String
    ) {
        val message = trace()
        Log.i(tag ?: NAME, message)
        if (shouldUpdate)
            _logMessages.update { it + "I/${tag ?: NAME}: $message" }
    }

    fun d(
        tag: String? = null,
        shouldUpdate: Boolean = false,
        trace: () -> String
    ) {
        val message = trace()
        Log.d(tag ?: NAME, message)
        if (shouldUpdate)
            _logMessages.update { it + "D/${tag ?: NAME}: $message" }
    }

    fun w(
        tag: String? = null,
        shouldUpdate: Boolean = false,
        trace: () -> String
    ) {
        val message = trace()
        Log.w(tag ?: NAME, message)
        if (shouldUpdate)
            _logMessages.update { it + "W/${tag ?: NAME}: $message" }
    }

    fun e(
        tag: String? = null,
        shouldUpdate: Boolean = false,
        trace: () -> String
    ) {
        val message = trace()
        Log.e(tag ?: NAME, message)
        if (shouldUpdate)
            _logMessages.update { it + "E/${tag ?: NAME}: $message" }
    }

    fun s(
        tag: String? = null,
        shouldUpdate: Boolean = false,
        trace: () -> String
    ) {
        val message = trace()
        Log.i(tag ?: NAME, message)
        if (shouldUpdate)
            _logMessages.update { it + "S/${tag ?: NAME}: $message" }
    }
}