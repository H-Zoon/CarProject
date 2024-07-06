package com.devidea.chevy.bluetooth

enum class BTState(val state: Int) {
    CONNECTED(1),
    CONNECTING(2),
    DISCONNECTED(0),
    NOT_FOUND(4),
    SCANNING(3);

    companion object {
        fun fromState(state: Int): BTState? {
            return entries.find { it.state == state }
        }
    }
}