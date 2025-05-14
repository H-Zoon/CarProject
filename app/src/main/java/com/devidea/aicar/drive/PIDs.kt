package com.devidea.aicar.drive

enum class PIDs (val code: String, val header: String){
    S_FUEL_TRIM("0106", "7E0"), //(Short-term Fuel Trim) 응답에서 즉각적인 연료 보정 비율을 % 단위로 파싱합니다.
    L_FUEL_TRIM("0107", "7E0"), //(Long-term Fuel Trim) 응답에서 장기적인 연료 보정 비율을 % 단위로 파싱합니다.
    BAROMETRIC("0133", "7E0"), //(Barometric Pressure) 응답에서 대기압을 kPa 단위로 파싱합니다.
    RPM("010C", "7E0"),
    SPEED("010D", "7E0"),
    ECT("0105", "7E0"),
    THROTTLE("0111", "7E0"),
    LOAD("0104", "7E0"),
    IAT("010F", "7E0"),
    MAF("0110", "7E0"),
    BATT("0142", "7E0"),
    FUEL_RATE("5E", "7E0"),
    CURRENT_GEAR("22199A", "7E1"),
    OIL_PRESSURE("22115C", "7E0"),
    OIL_TEMP("221154", "7E0"),
    TRANS_FLUID_TEMP("221940", "7E1");


    companion object {
        private val map = PIDs.entries.associateBy(PIDs::code)
        fun fromCode(code: String): PIDs? = map[code.uppercase()]
    }
}