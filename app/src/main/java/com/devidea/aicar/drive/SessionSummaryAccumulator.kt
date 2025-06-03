package com.devidea.aicar.drive

import com.devidea.aicar.storage.room.drive.DrivingDataPoint
import com.devidea.aicar.ui.main.components.history.SessionSummary
import java.time.Duration

/**
 * SessionSummaryAccumulator는 새 포인트를 받아 내부 상태를 갱신하고
 * 최신 SessionSummary를 반환합니다.
 *
 * @param fuelPricePerLiter: 리터당 유류비 (int, 단위: 원)
 */
class SessionSummaryAccumulator(
    private var fuelPricePerLiter: Int
) {

    companion object {
        private const val accelThreshold: Double = 3.0
        private const val decelThreshold: Double = -3.0
    }

    private var prevPoint: DrivingDataPoint? = null
    private var distanceMeters = 0.0
    private var speedSum = 0L
    private var fuelConsumptionSum = 0.0
    private var accelEvents = 0
    private var brakeEvents = 0
    private var pointCount = 0

    /**
     * 새 포인트 하나를 받아서 내부 상태를 갱신한 뒤,
     * fuelPricePerLiter를 이용해 최신 SessionSummary를 생성하여 반환
     */
    fun add(curr: DrivingDataPoint): SessionSummary {
        pointCount++

        // 1) 속도·연비 합계
        speedSum += curr.speed
        if (curr.instantKPL > 0f) {
            // 초당 연료 소비량 (리터 단위) 누적
            fuelConsumptionSum += (curr.speed / 3600.0) / curr.instantKPL
        }

        // 2) 거리 적분 및 가감속 이벤트 카운트
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
        val avgKPL     = if (fuelConsumptionSum > 0) {
            (distanceKm / fuelConsumptionSum).toFloat()
        } else 0f

        // 리터당 유류비(fuelPricePerLiter)를 기준으로 사용한 연료비 계산
        val fuelUsed  = if (avgKPL > 0) distanceKm / avgKPL else 0f
        val fuelPrice = (fuelUsed * fuelPricePerLiter).toInt()

        return SessionSummary(
            distance   = distanceKm,
            avgSpeed   = avgSpeed,
            avgKPL     = avgKPL,
            fuelPrice  = fuelPrice,
            accelEvent = accelEvents,
            brakeEvent = brakeEvents
        )
    }

    /**
     * 필요하다면 외부에서 fuelPricePerLiter(리터당 유류비)를 업데이트할 수도 있도록
     * setter 메서드를 추가할 수 있습니다.
     */
    fun updateFuelPrice(newPricePerLiter: Int) {
        fuelPricePerLiter = newPricePerLiter
    }
}