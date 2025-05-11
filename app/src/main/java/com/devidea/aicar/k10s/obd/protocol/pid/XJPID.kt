package com.devidea.aicar.k10s.obd.protocol.pid

data class XJPID(
    var number: Int = 0,
    var PID: Int = 0,
    var unit: String = "",
    var description: String = "",
    var status: String = "--|--"
)