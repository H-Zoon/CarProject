package com.devidea.chevy.bluetooth

enum class BTState(val state: Int,  val description: String) {
    CONNECTED(1, "Connected"),
    CONNECTING(2, "Connecting"),
    DISCONNECTED(0, "Disconnected"),
    NOT_FOUND(4, "Not Found"),
    SCANNING(3, "Scanning");

    companion object {
        fun fromState(state: Int): BTState? {
            return entries.find { it.state == state }
        }
    }
}