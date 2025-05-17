package com.devidea.aicar.drive

/**
 * OBD-II 및 확장 PID 정의
 */
enum class PIDs(val code: String, val header: String) {
    /** Short-term Fuel Trim: 단기간 연료 보정 비율 (STFT, %) */
    S_FUEL_TRIM("0106", "7E0"),
    /** Long-term Fuel Trim: 장기간 연료 보정 비율 (LTFT, %) */
    L_FUEL_TRIM("0107", "7E0"),
    /** Barometric Pressure: 대기압 (kPa) */
    BAROMETRIC("0133", "7E0"),
    /** Engine Coolant Temperature: 냉각수 온도 (°C, A-40) */
    ECT("0105", "7E0"),
    /** Engine Load: 엔진 부하 (% of max load, A*100/255) */
    ENGIN_LOAD("0104", "7E0"),
    /** Engine RPM: 엔진 회전수 (0.25 rpm 단위, A*256+B)/4 */
    RPM("010C", "7E0"),
    /** Vehicle Speed: 차량 속도 (1 km/h 단위, A) */
    SPEED("010D", "7E0"),
    /** Intake Air Temperature: 흡기 공기 온도 */
    INTAKE_TEMP("010F", "7E0"),
    /** Mass Air Flow: 흡입 공기 질량 유량 (g/s, (A*256+B)/100) */
    MAF("0110", "7E0"),
    /** Throttle Position: 스로틀 개도율 (%) (A*100/255) */
    THROTTLE("0111", "7E0"),
    /** Control Module Voltage: 배터리 전압 (V, A/100) */
    BATT("0142", "7E0"),
    /** Fuel Rate: 연료 소비 유량 (L/h, Mode 01 PID 0x5E) */
    FUEL_RATE("5E", "7E0"),
    /** Ambient Air Temperature: 외기 온도 (°C, A-40) */
    AMBIENT_AIR_TEMP("0146", "7E0"),
    /** Catalyst Temperature Bank 1: 촉매 온도 (°C, A-40) */
    CATALYST_TEMP_BANK1("013C", "7E0"),
    /** Commanded Equivalence Ratio: ECU 목표 λ (Lambda, 무차원) */
    COMMANDED_EQUIVALENCE_RATIO("0144", "7E0"),
    /** Fuel Level Input: 연료 잔량 (%) (A*100/255) */
    FUEL_LEVEL("012F", "7E0"),
    /** Intake Manifold Pressure: 흡기 매니폴드 절대 압력 (kPa, A) */
    INTAKE_PRESSURE("010B", "7E0"),

    // 제조사 확장 PID
    /** Current Gear: 현재 변속 기어 */
    CURRENT_GEAR("22199A", "7E1"),
    /** Oil Pressure: 오일 압력 (kPa) */
    OIL_PRESSURE("22115C", "7E0"),
    /** Oil Temperature: 오일 온도 (°C) */
    OIL_TEMP("221154", "7E0"),
    /** Transmission Fluid Temperature: 변속기 오일 온도 (°C) */
    TRANS_FLUID_TEMP("221940", "7E1");

    companion object {
        private val map = PIDs.entries.associateBy(PIDs::code)
        fun fromCode(code: String): PIDs? = map[code.uppercase()]
    }
}