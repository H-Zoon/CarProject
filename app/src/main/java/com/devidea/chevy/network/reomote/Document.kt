package com.devidea.chevy.network.reomote

/**
 * 카카오 장소 검색 결과 문서 모델
 *
 * @property id                   장소 ID
 * @property place_name           장소명, 업체명
 * @property category_name        카테고리 이름
 * @property category_group_code  중요 카테고리만 그룹핑한 카테고리 그룹 코드
 * @property category_group_name  중요 카테고리만 그룹핑한 카테고리 그룹명
 * @property phone                전화번호
 * @property address_name         전체 지번 주소
 * @property road_address_name    전체 도로명 주소
 * @property x                    X 좌표값 (경위도인 경우 longitude, 경도)
 * @property y                    Y 좌표값 (경위도인 경우 latitude, 위도)
 * @property place_url            장소 상세페이지 URL
 * @property distance             중심좌표까지의 거리 (단, x,y 파라미터를 준 경우에만 존재), 단위 meter
 */
data class Document(
    val id: String,
    val place_name: String,
    val category_name: String,
    val category_group_code: String,
    val category_group_name: String,
    val phone: String,
    val address_name: String,
    val road_address_name: String,
    val x: Double,
    val y: Double,
    val place_url: String,
    val distance: String?
)