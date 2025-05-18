package com.devidea.aicar.drive

/**
 * GaugeManager 데이터 디코딩 유틸
 *
 * OBD-II 응답 문자열(f)의 특정 PID별 물리량으로 파싱합니다.
 */
object Decoders {
    private const val TAG = "GaugeManager"

    internal val parsers: Map<PIDs, (String) -> Number> = mapOf(
        PIDs.RPM to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            (a shl 8 or b) / 4
        },
        PIDs.SPEED to { f -> f.substring(4,6).toInt(16) },
        PIDs.ECT to { f -> f.substring(4,6).toInt(16) - 40 },
        PIDs.THROTTLE to { f -> f.substring(4,6).toInt(16) * 100 / 255 },
        PIDs.ENGIN_LOAD to { f -> f.substring(4,6).toInt(16) * 100 / 255 },
        PIDs.INTAKE_TEMP to { f -> f.substring(4,6).toInt(16) - 40 },
        PIDs.MAF to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            (a shl 8 or b) / 100f
        },
        PIDs.BATT to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            (a shl 8 or b) / 1000f
        },
        PIDs.FUEL_RATE to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            (a shl 8 or b) / 20f
        },
        PIDs.CURRENT_GEAR to { f ->
            f.substring(6,8).toInt(16)
        },
        PIDs.OIL_PRESSURE to { f ->
            val a = f.substring(6,8).toInt(16)
            (a * 4.0 - 101) * 0.145038
        },
        PIDs.OIL_TEMP to { f -> f.substring(6,8).toInt(16) - 40 },
        PIDs.TRANS_FLUID_TEMP to { f -> f.substring(6,8).toInt(16) - 40 },
        // Short-term Fuel Trim (PID 0x06): (A - 128) * 100 / 128 [%]
        PIDs.S_FUEL_TRIM to { f ->
            val a = f.substring(4,6).toInt(16)
            (a - 128) * 100f / 128f
        },
        // Long-term Fuel Trim (PID 0x07): (A - 128) * 100 / 128 [%]
        PIDs.L_FUEL_TRIM to { f ->
            val a = f.substring(4,6).toInt(16)
            (a - 128) * 100f / 128f
        },
        // Barometric Pressure (PID 0x33): A [kPa]
        PIDs.BAROMETRIC to { f -> f.substring(4,6).toInt(16).toFloat() },
        // Ambient Air Temperature (PID 0x46): A - 40 [°C]
        PIDs.AMBIENT_AIR_TEMP to { f -> f.substring(4,6).toInt(16) - 40 },
        // Catalyst Temperature Bank1 (PID 0x3C): (256*A + B)/10 - 40 [°C]
        PIDs.CATALYST_TEMP_BANK1 to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            ( (a shl 8 or b) / 10f ) - 40f
        },
        // Commanded Equivalence Ratio (PID 0x44): (256*A + B) / 32768 [lambda]
        PIDs.COMMANDED_EQUIVALENCE_RATIO to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            (a shl 8 or b) / 32768f
        },
        // Fuel Level Input (PID 0x2F): A * 100 / 255 [%]
        PIDs.FUEL_LEVEL to { f -> f.substring(4,6).toInt(16) * 100f / 255f },
        // Intake Manifold Pressure (PID 0x0B): A [kPa]
        PIDs.INTAKE_PRESSURE to { f -> f.substring(4,6).toInt(16).toFloat() }
    )
}
