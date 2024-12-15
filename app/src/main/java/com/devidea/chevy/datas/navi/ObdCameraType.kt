package com.devidea.chevy.datas.navi

enum class ObdCameraType(val value: Int) {
    SPEED(0),
    SURVEILLANCE(1),
    TRAFFICLIGHT(2),
    BREAKRULE(3),
    BUSWAY(4),
    EMERGENCY(5),
    BICYCLE(6),
    INTERVALVELOCITYSTART(8),
    INTERVALVELOCITYEND(9),
    FLOWSPEED(10),
    ETC(11);
}