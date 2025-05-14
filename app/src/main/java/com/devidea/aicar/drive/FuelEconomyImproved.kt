package com.devidea.aicar.drive

/**
 * FuelEconomyImproved:
 * MAF 센서와 ECU 트림, 대기압 보정을 반영하여
 * 순간 및 평균 연비(km/L)를 계산하는 유틸리티 객체입니다.
 */
object FuelEconomyImproved {
    // 기본 상수
    const val DEFAULT_AFR = 14.7f             // 공기:연료비
    const val DEFAULT_FUEL_DENSITY = 720.0f         // 연료 밀도 (g/L)
    const val STANDARD_BARO = 101.3f                // 표준 대기압 (kPa)

    /**
     * 단일 샘플로 순간 연비를 계산합니다 (km/L).
     *
     * @param mafGperS      MAF 값 (g/s)
     * @param speedKmh      차량 속도 (km/h)
     * @param stftPercent   Short-term Fuel Trim (%) — 기본 0
     * @param ltftPercent   Long-term Fuel Trim (%) — 기본 0
     * @param baroKpa       Barometric Pressure (kPa) — 기본 STANDARD_BARO
     * @param afr           공기·연료비 — 기본 DEFAULT_AFR
     * @param fuelDensityGperL 연료 밀도 — 기본 DEFAULT_FUEL_DENSITY
     * @return               순간 연비 (km/L)
     */
    fun calculateInstantKPL(
        mafGperS: Float,
        speedKmh: Int,
        stftPercent: Float = 0f,
        ltftPercent: Float = 0f,
        baroKpa: Float = STANDARD_BARO,
        afr: Float = DEFAULT_AFR,
        fuelDensityGperL: Float = DEFAULT_FUEL_DENSITY
    ): Float {
        // 1) 기본 연료 유량 L/h (모두 Float 연산)
        val baseFuelFlowLh = mafGperS * 3600f / (afr * fuelDensityGperL)

        // 2) 연료 트림 보정 계수
        val trimFactor = (1f + stftPercent / 100f) * (1f + ltftPercent / 100f)

        // 3) 대기압 보정 계수
        val baroFactor = baroKpa / STANDARD_BARO

        // 4) 보정된 연료 유량
        val correctedFuelFlowLh = baseFuelFlowLh * trimFactor * baroFactor
        if (correctedFuelFlowLh <= 0f) return Float.POSITIVE_INFINITY

        // 5) km/h ÷ L/h → km/L (Int/Float ⇒ Float)
        return speedKmh / correctedFuelFlowLh
    }

    /**
     * 평균 연비 계산을 위한 누적 클래스.
     * addSample() 호출로 샘플을 누적한 후 getAverageKPL() 로 평균 연비를 구합니다.
     */
    class AverageKPL(
        private val afr: Float = DEFAULT_AFR,
        private val fuelDensityGperL: Float = DEFAULT_FUEL_DENSITY,
        private val standardBaro: Float = STANDARD_BARO
    ) {
        private var totalFuelL: Double = 0.0
        private var totalDistanceKm: Double = 0.0

        /**
         * 새로운 샘플을 누적합니다.
         *
         * @param mafGperS      MAF 값 (g/s)
         * @param speedKmh      속도 (km/h)
         * @param deltaTimeSec  샘플 간격 (초)
         * @param stftPercent   STFT (%)
         * @param ltftPercent   LTFT (%)
         * @param baroKpa       대기압 (kPa)
         */
        fun addSample(
            mafGperS: Float,
            speedKmh: Int,
            deltaTimeSec: Float,
            stftPercent: Float = 0f,
            ltftPercent: Float = 0f,
            baroKpa: Float = standardBaro
        ) {
            // 기본 연료 유량 L/h
            val baseFuelFlowLh = mafGperS * 3600.0 / (afr * fuelDensityGperL)
            // 보정 계수
            val trimFactor = (1 + stftPercent / 100) * (1 + ltftPercent / 100)
            val baroFactor = baroKpa / standardBaro

            // 보정된 연료 유량
            val correctedFuelFlowLh = baseFuelFlowLh * trimFactor * baroFactor

            // 해당 구간 연료 사용량 L
            val fuelUsedL = correctedFuelFlowLh * (deltaTimeSec / 3600.0)
            // 해당 구간 주행 거리 km
            val distanceKm = speedKmh * (deltaTimeSec / 3600.0)

            totalFuelL += fuelUsedL
            totalDistanceKm += distanceKm
        }

        /**
         * 누적된 데이터를 기반으로 평균 연비를 계산합니다 (km/L).
         * 연료 사용량이 0이면 Infinity 반환.
         */
        fun getAverageKPL(): Double {
            return if (totalFuelL > 0.0) {
                totalDistanceKm / totalFuelL
            } else {
                Double.POSITIVE_INFINITY
            }
        }

        /** 누적 데이터를 초기화합니다. */
        fun reset() {
            totalFuelL = 0.0
            totalDistanceKm = 0.0
        }
    }
}

