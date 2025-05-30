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
    private const val FUEL_PRICE = 1500            // 연료 밀도 (g/L)
    private const val accelThreshold: Double = 3.0      // 3 m/s² 이상을 급가속
    private const val decelThreshold: Double = -3.0      // -3 m/s² 이하를 급감속

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

    fun calculateSessionSummary(dataPoints: List<DrivingDataPoint>): SessionSummary {
        if (dataPoints.isEmpty()) return SessionSummary(0f, 0f, 0f, 0, 0, 0)

        // 누적할 변수들
        var distanceMeters = 0.0
        var accelEvents = 0
        var brakeEvents = 0
        var speedSum = 0L                   // 속도 합 (km/h)
        var fuelConsumptionSum = 0.0        // 즉시 연비 기반 연료 소비량 (L)

        // 첫 포인트 준비
        var prev = dataPoints[0]
        speedSum += prev.speed
        if (prev.instantKPL > 0f) {
            fuelConsumptionSum += (prev.speed / 3600.0) / prev.instantKPL
        }

        // 나머지 포인트들에 대해 한 번만 순회
        for (i in 1 until dataPoints.size) {
            val curr = dataPoints[i]

            // 1) 평균속도·연료소비 누적
            speedSum += curr.speed
            if (curr.instantKPL > 0f) {
                fuelConsumptionSum += (curr.speed / 3600.0) / curr.instantKPL
            }

            // 2) 거리 적분 (사다리꼴 룰)
            val vPrev = prev.speed * 1000.0 / 3600.0    // m/s
            val vCurr = curr.speed * 1000.0 / 3600.0    // m/s
            val dtSec = Duration
                .between(prev.timestamp, curr.timestamp)
                .toMillis() / 1000.0

            if (dtSec > 0) {
                distanceMeters += (vPrev + vCurr) / 2.0 * dtSec

                // 3) 가속도 계산 및 이벤트 카운트
                val accel = (vCurr - vPrev) / dtSec
                if (accel >= accelThreshold) accelEvents++
                if (accel <= decelThreshold) brakeEvents++
            }

            prev = curr  // 다음 루프를 위해 현재를 이전으로
        }

        // 최종 계산
        val distanceKm = (distanceMeters / 1000.0).toFloat()
        val avgSpeed = (speedSum.toDouble() / dataPoints.size).toFloat()
        val avgKPL = if (fuelConsumptionSum > 0) {
            (distanceKm / fuelConsumptionSum).toFloat()
        } else 0f
        val fuelUsed = if (avgKPL > 0) distanceKm / avgKPL else 0f
        val fuelPrice = (fuelUsed * FUEL_PRICE).toInt()

        return SessionSummary(
            distance = distanceKm,
            avgSpeed = avgSpeed,
            avgKPL = avgKPL,
            fuelPrice = fuelPrice,
            accelEvent = accelEvents,
            brakeEvent = brakeEvents
        )
    }
}