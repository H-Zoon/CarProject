package com.devidea.chevy.datas.navi

import com.kakaomobility.knsdk.KNRGCode

enum class NavigationIconType(val value: Int) {
    ARRIVED_DESTINATION(15),  // 목적지 도착 아이콘
    ARRIVED_SERVICE_AREA(13), // 서비스 구역 도착 아이콘
    ARRIVED_TOLLGATE(14),     // 톨게이트 도착 아이콘
    ARRIVED_TUNNEL(16),       // 터널 도착 아이콘
    ARRIVED_WAYPOINT(10),     // 경유지 도착 아이콘
    DEFAULT(1),               // 자차 아이콘 (사용자 정의 전환 아이콘 배열의 첫 요소)
    ENTER_ROUNDABOUT(11),     // 로터리 진입 아이콘
    LEFT(2),                  // 좌회전 아이콘
    LEFT_BACK(6),             // 좌후방 아이콘
    LEFT_FRONT(4),            // 좌전방 아이콘
    TURN_AROUND(8),           // U턴 아이콘
    NONE(0),                  // 정의되지 않음 (사용자 정의 전환 아이콘 배열의 첫 요소)
    OUT_ROUNDABOUT(12),       // 로터리 나가기 아이콘
    RIGHT(3),                 // 우회전 아이콘
    RIGHT_BACK(7),            // 우후방 아이콘
    RIGHT_FRONT(5),           // 우전방 아이콘
    STRAIGHT(9)               // 직진 아이콘
}

fun findGuideAsset(code: KNRGCode): NavigationIconType {
    return when (code) {
        KNRGCode.KNRGCode_Start -> NavigationIconType.NONE
        KNRGCode.KNRGCode_Goal -> NavigationIconType.ARRIVED_DESTINATION
        KNRGCode.KNRGCode_Via -> NavigationIconType.ARRIVED_WAYPOINT
        KNRGCode.KNRGCode_Straight -> NavigationIconType.STRAIGHT

        // 좌회전 관련 방향
        KNRGCode.KNRGCode_LeftTurn -> NavigationIconType.LEFT

        KNRGCode.KNRGCode_LeftDirection,
        KNRGCode.KNRGCode_LeftOutHighway,
        KNRGCode.KNRGCode_LeftInHighway,
        KNRGCode.KNRGCode_LeftOutCityway,
        KNRGCode.KNRGCode_LeftInCityway,
        KNRGCode.KNRGCode_LeftStraight,
        KNRGCode.KNRGCode_ChangeLeftHighway -> NavigationIconType.LEFT_FRONT

        // 우회전 관련 방향
        KNRGCode.KNRGCode_RightTurn -> NavigationIconType.RIGHT

        KNRGCode.KNRGCode_RightDirection,
        KNRGCode.KNRGCode_RightOutHighway,
        KNRGCode.KNRGCode_RightInHighway,
        KNRGCode.KNRGCode_RightOutCityway,
        KNRGCode.KNRGCode_RightInCityway,
        KNRGCode.KNRGCode_RightStraight,
        KNRGCode.KNRGCode_ChangeRightHighway -> NavigationIconType.RIGHT_FRONT

        // 후방 방향
        KNRGCode.KNRGCode_Direction_7 -> NavigationIconType.LEFT_BACK
        KNRGCode.KNRGCode_Direction_5 -> NavigationIconType.RIGHT_BACK

        // U턴
        KNRGCode.KNRGCode_UTurn -> NavigationIconType.TURN_AROUND

        /*
        // 고속도로 출입구
        KNRGCode.KNRGCode_OutHighway -> NavigationIconType.OUT_ROUNDABOUT
        KNRGCode.KNRGCode_InHighway -> NavigationIconType.ENTER_ROUNDABOUT

        // 페리 항로
        KNRGCode.KNRGCode_InFerry -> NavigationIconType.ENTER_ROUNDABOUT
        KNRGCode.KNRGCode_OutFerry -> NavigationIconType.OUT_ROUNDABOUT
         */

        // 터널 및 톨게이트
        KNRGCode.KNRGCode_OverPath -> NavigationIconType.ARRIVED_TUNNEL
        KNRGCode.KNRGCode_Tollgate -> NavigationIconType.ARRIVED_TOLLGATE
        KNRGCode.KNRGCode_NonstopTollgate -> NavigationIconType.ARRIVED_TOLLGATE
        KNRGCode.KNRGCode_JoinAfterBranch -> NavigationIconType.ARRIVED_SERVICE_AREA

        // 로터리 방향
        KNRGCode.KNRGCode_RotaryDirection_1,
        KNRGCode.KNRGCode_RotaryDirection_2,
        KNRGCode.KNRGCode_RotaryDirection_3,
        KNRGCode.KNRGCode_RotaryDirection_4,
        KNRGCode.KNRGCode_RotaryDirection_5,
        KNRGCode.KNRGCode_RotaryDirection_6,
        KNRGCode.KNRGCode_RotaryDirection_7,
        KNRGCode.KNRGCode_RotaryDirection_8,
        KNRGCode.KNRGCode_RotaryDirection_9,
        KNRGCode.KNRGCode_RotaryDirection_10,
        KNRGCode.KNRGCode_RotaryDirection_11,
        KNRGCode.KNRGCode_RotaryDirection_12 -> NavigationIconType.ENTER_ROUNDABOUT

        // 기본적으로 매핑되지 않은 경우 null 반환
        else -> NavigationIconType.NONE
    }
}