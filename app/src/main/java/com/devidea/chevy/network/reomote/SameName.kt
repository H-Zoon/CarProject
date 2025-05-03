package com.devidea.chevy.network.reomote

/**
 * 질의어의 지역 및 키워드 분석 정보
 *
 * @property region 질의어에서 인식된 지역의 리스트 예: '중앙로 맛집' 에서 중앙로에 해당하는 지역 리스트
 * @property keyword 질의어에서 지역 정보를 제외한 키워드 예: '중앙로 맛집' 에서 '맛집'
 * @property selected_region 인식된 지역 리스트 중, 현재 검색에 사용된 지역 정보
 *
 */
data class SameName(
    val region: List<String>,
    val keyword: String,
    val selected_region: String
)