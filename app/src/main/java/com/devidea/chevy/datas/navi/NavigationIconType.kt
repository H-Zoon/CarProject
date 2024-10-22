package com.devidea.chevy.datas.navi

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