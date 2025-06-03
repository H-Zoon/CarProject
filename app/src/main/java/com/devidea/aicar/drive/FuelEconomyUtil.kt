package com.devidea.aicar.drive

import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.ui.main.components.history.SessionSummary
import java.time.Duration

/**
 * FuelEconomyImproved:
 * MAF 센서와 ECU 트림, 대기압 보정을 반영하여
 * 순간 및 평균 연비(km/L)를 계산하는 유틸리티 객체입니다.
 */
object FuelEconomyUtil {
    private const val STOICH_AFR = 14.7f                // 가솔린 스토이키 AFR
    private const val FUEL_DENSITY = 720.0f            // 연료 밀도 (g/L)

    /**
     * 순간 연비 계산
     * @param maf      MAF 센서값 (g/s)
     * @param speedKmh 속도 (km/h)
     * @param stft     Short-term Fuel Trim (%)
     * @param ltft     Long-term Fuel Trim (%)
     * @return 순간 연비 (km/L)
     */
    fun calculateInstantFuelEconomy(
        maf: Float,
        speedKmh: Int,
        stft: Float,
        ltft: Float
    ): Float {
        val lambda = 1f + (stft + ltft) / 100f
        val fuelMassFlow = maf / (lambda * STOICH_AFR)        // 연료 질량 유량 (g/s)
        val fuelVolFlowLh = (fuelMassFlow / FUEL_DENSITY) * 3600f  // 연료 부피 유량 (L/h)
        return if (fuelVolFlowLh > 0f) speedKmh / fuelVolFlowLh else 0f
    }
}