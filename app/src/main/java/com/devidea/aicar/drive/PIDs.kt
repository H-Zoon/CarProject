package com.devidea.aicar.drive

enum class PIDs (val code: String, val header: String){
    RPM("0C", "7E0"),
    SPEED("0D", "7E0"),
    ECT("05", "7E0"),
    THROTTLE("11", "7E0"),
    LOAD("04", "7E0"),
    IAT("0F", "7E0"),
    MAF("10", "7E0"),
    BATT("42", "7E0"),
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