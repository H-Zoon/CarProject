package com.devidea.aicar.drive

import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.ui.main.components.history.SessionSummary
import java.time.Duration

class SessionSummaryAccumulator() {

    companion object {
        private const val STOICH_AFR = 14.7f                // 가솔린 스토이키 AFR
        private const val FUEL_DENSITY = 720.0f            // 연료 밀도 (g/L)
        private const val FUEL_PRICE = 1500            // 연료 밀도 (g/L)
        private const val accelThreshold: Double = 3.0      // 3 m/s² 이상을 급가속
        private const val decelThreshold: Double = -3.0      // -3 m/s² 이하를 급감속
    }

    private var prevPoint: DrivingDataPoint? = null
    private var distanceMeters = 0.0
    private var speedSum = 0L
    private var fuelConsumptionSum = 0.0
    private var accelEvents = 0
    private var brakeEvents = 0
    private var pointCount = 0

    /**
     * 새 포인트 하나만 받아서 내부 상태 갱신 후
     * 최신 SessionSummary를 반환
     */
    fun add(curr: DrivingDataPoint): SessionSummary {
        pointCount++
        // 1) 속도·연비 합계
        speedSum += curr.speed
        if (curr.instantKPL > 0f) {
            fuelConsumptionSum += (curr.speed / 3600.0) / curr.instantKPL
        }

        // 2) 거리 적분 & 이벤트 카운트
        prevPoint?.let { prev ->
            val vPrev = prev.speed * 1000.0 / 3600.0   // m/s
            val vCurr = curr.speed * 1000.0 / 3600.0   // m/s
            val dt = Duration.between(prev.timestamp, curr.timestamp).toMillis() / 1000.0
            if (dt > 0) {
                distanceMeters += (vPrev + vCurr) / 2 * dt
                val accel = (vCurr - vPrev) / dt
                if (accel >= accelThreshold) accelEvents++
                if (accel <= decelThreshold) brakeEvents++
            }
        }
        prevPoint = curr

        // 3) 최종 요약 생성
        val distanceKm = (distanceMeters / 1000).toFloat()
        val avgSpeed   = (speedSum.toDouble() / pointCount).toFloat()
        val avgKPL     = if (fuelConsumptionSum > 0)
            (distanceKm / fuelConsumptionSum).toFloat()
        else 0f
        val fuelUsed  = if (avgKPL > 0) distanceKm / avgKPL else 0f
        val fuelPrice = (fuelUsed * FUEL_PRICE).toInt()

        return SessionSummary(
            distance   = distanceKm,
            avgSpeed   = avgSpeed,
            avgKPL     = avgKPL,
            fuelPrice  = fuelPrice,
            accelEvent = accelEvents,
            brakeEvent = brakeEvents
        )
    }
}