package com.devidea.chevy

import android.util.Log

object Logger {
    private const val NAME = "chevy"

    fun v(tag: String? = null, trace: () -> String) {
        Log.v(tag ?: NAME, trace())
    }

    fun i(tag: String? = null, trace: () -> String) {
        Log.i(tag ?: NAME, trace())
    }

    fun d(tag: String? = null, trace: () -> String) {
        Log.d(tag ?: NAME, trace())
    }

    fun w(tag: String? = null, trace: () -> String) {
        Log.w(tag ?: NAME, trace())
    }

    fun e(tag: String? = null, trace: () -> String) {
        Log.e(tag ?: NAME, trace())
    }

    fun s(tag: String? = null, trace: () -> String) {
        Log.i(tag ?: NAME, trace())
    }
}