package com.devidea.chevy.k10s.obd.protocol.pid

data class XJPIDDATA(
    var A: Int = 0,
    var B: Int = 0,
    var C: Int = 0,
    var D: Int = 0,
    var support: Int = -1
)
