package com.devidea.aicar.k10s.navi

import com.kakaomobility.knsdk.guidance.knguidance.safetyguide.objects.KNSafety

/**
 * public static final int	BICYCLE	6
 * public static final int	BREAKRULE	3
 * public static final int	BUSWAY	4
 * public static final int	EMERGENCY	5
 * public static final int	INTERVALVELOCITYEND	9
 * public static final int	INTERVALVELOCITYSTART	8
 * public static final int	SPEED	0
 * public static final int	SURVEILLANCE	1
 * public static final int	TRAFFICLIGHT	2
 * https://a.amap.com/lbs/static/unzip/Android_Navi_Doc/index.html?index-files/index-7.html
 */
enum class CameraType(val code: Int) {
    MOBILE_SPEED_CAMERA(81),            // 이동식 과속 단속 카메라
    FIXED_SPEED_CAMERA(82),             // 고정식 과속 단속 카메라
    BUS_LANE_CAMERA(84),                // 버스 전용 차로 위반 단속 카메라
    BUS_LANE_AND_SIGNAL_CAMERA(89),     // 버스 전용 차로 및 신호 위반 단속 카메라
    SIGNAL_AND_SPEED_CAMERA(86),        // 신호 및 과속 단속 카메라
    SECTION_START_CAMERA(92),           // 구간 단속 시점 카메라
    SECTION_END_CAMERA(93),             // 구간 단속 종점 카메라
    SHOULDER_LANE_CAMERA(94),           // 갓길 단속 카메라
    SECTION_MONITORING_ZONE(96),        // 구간 단속 구간
    DESIGNATED_LANE_CAMERA(97),         // 지정 차로 단속 카메라
    LANE_CHANGE_START_CAMERA(98),       // 차로 변경 구간 단속 시점 카메라
    LANE_CHANGE_END_CAMERA(99),         // 차로 변경 구간 단속 종점 카메라
    BOX_SPEED_CAMERA(100),              // 박스형 과속 단속 카메라
    SEATBELT_CAMERA(101),               // 안전벨트 미착용 단속 카메라
    REAR_SPEED_CAMERA(102),             // 후면 과속 단속 카메라
    REAR_SIGNAL_AND_SPEED_CAMERA(103)   // 후면 신호 및 과속 단속 카메라
}

fun isCameraType(code: Int): Boolean {
    return CameraType.values().any { it.code == code }
}

// 확장 함수 또는 유틸리티 함수 (필요에 따라 구현)
fun KNSafety.isCameraType(): Boolean {
    return when (this.code.value) {
        81, 82, 86, 100, 102, 103 -> true
        else -> false
    }
}