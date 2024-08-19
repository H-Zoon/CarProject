package com.devidea.chevy.bluetooth

import android.content.Context
import com.devidea.chevy.R

enum class BTState(val description: Int) {
    CONNECTED(R.string.bt_connected),
    CONNECTING(R.string.bt_connecting),
    DISCONNECTED(R.string.bt_disconnected),
    NOT_FOUND(R.string.bt_not_found),
    SCANNING(R.string.bt_scanning);

    fun description(context: Context): String {
        return context.getString(this.description)
    }
}