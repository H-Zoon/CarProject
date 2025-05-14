package com.devidea.aicar.drive

object Decoders {
    private const val TAG = "GaugeManager"

    internal val parsers: Map<PIDs, (String) -> Any> = mapOf(
        PIDs.RPM       to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            (a shl 8 or b) / 4
        },
        PIDs.SPEED     to { f -> f.substring(4,6).toInt(16) },
        PIDs.ECT       to { f -> f.substring(4,6).toInt(16) - 40 },
        PIDs.THROTTLE  to { f -> f.substring(4,6).toInt(16) * 100 / 255 },
        PIDs.LOAD      to { f -> f.substring(4,6).toInt(16) * 100 / 255 },
        PIDs.IAT       to { f -> f.substring(4,6).toInt(16) - 40 },
        PIDs.MAF       to { f ->
            val a = f.substring(4,6).toInt(16)
            val b = f.substring(6,8).toInt(16)
            (a shl 8 or b) / 100f
        },
        PIDs.BATT      to { f ->
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
            f.substring(4,6).toInt(16)
        },
        PIDs.OIL_PRESSURE to { f ->
            val a = f.substring(4,6).toInt(16)
            a * 0.65f - 17.5f
        },
        PIDs.OIL_TEMP to { f ->
            f.substring(4,6).toInt(16) - 40
        },
        PIDs.TRANS_FLUID_TEMP to { f ->
            f.substring(4,6).toInt(16) - 40
        },
        // Short-term Fuel Trim (PID 0x06): (A - 128) * 100 / 128 [%]
        PIDs.S_FUEL_TRIM   to { f ->
            val a = f.substring(4,6).toInt(16)
            (a - 128) * 100f / 128f
        },
        // Long-term Fuel Trim (PID 0x07): (A - 128) * 100 / 128 [%]
        PIDs.L_FUEL_TRIM   to { f ->
            val a = f.substring(4,6).toInt(16)
            (a - 128) * 100f / 128f
        },
        // Barometric Pressure (PID 0x33): A [kPa]
        PIDs.BAROMETRIC    to { f ->
            f.substring(4,6).toInt(16).toFloat()
        }
    )
}