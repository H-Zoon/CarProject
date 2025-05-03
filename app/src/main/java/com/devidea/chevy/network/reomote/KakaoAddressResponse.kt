package com.devidea.chevy.network.reomote

/**
 * 카카오 주소 검색 응답 모델
 *
 * @property meta 응답 관련 정보
 * @property documents 응답 결과
 */
data class KakaoAddressResponse(
    val meta: Meta? = null,
    val documents: List<Document>? = null
)