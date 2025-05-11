package com.devidea.aicar.network.reomote

/**
 * 검색 결과 메타 정보
 *
 * @property total_count 검색어에 검색된 문서 수
 * @property total_count 중 노출 가능 문서 수
 * @property is_end 현재 페이지가 마지막 페이지인지 여부. 값이 false면 다음 요청 시 page 값을 증가시켜 다음 페이지 요청 가능
 * @property same_name 질의어의 지역 및 키워드 분석 정보
 */
data class Meta(
    val total_count: Int,
    val pageable_count: Int,
    val is_end: Boolean,
    val same_name: SameName?
)