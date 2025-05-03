package com.devidea.chevy.network

import com.devidea.chevy.network.reomote.KakaoAddressResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    /**
     * 키워드 기반 주소 검색을 수행합니다.
     *
     * @param query            검색을 원하는 질의어. (필수)
     * @param categoryGroupCode 카테고리 그룹 코드로 결과를 필터링할 때 사용합니다. (선택)
     * @param x                중심 좌표의 X 혹은 경도(longitude) 값. radius와 함께 사용 시 지역 검색 필터링을 수행합니다. (선택)
     * @param y                중심 좌표의 Y 혹은 위도(latitude) 값. radius와 함께 사용 시 지역 검색 필터링을 수행합니다. (선택)
     * @param radius           중심 좌표부터의 반경거리(m). x, y와 함께 사용하여 반경 검색을 수행합니다. 최소 0, 최대 20000. (선택)
     * @param rect             사각형 범위 검색을 위한 좌표. `"minX,minY,maxX,maxY"` 형식으로 전달합니다. (선택)
     * @param page             결과 페이지 번호 (1~45). 기본값 1. (선택)
     * @param size             한 페이지에 보여질 문서 개수 (1~15). 기본값 15. (선택)
     * @param sort             결과 정렬 순서. `"accuracy"`(기본) 또는 `"distance"`. (선택)
     * @return [Response] wrapping [KakaoAddressResponse] containing 검색 결과
     */
    @GET("v2/local/search/keyword.json")
    suspend fun searchAddress(
        @Query("query") query: String,
        @Query("category_group_code") categoryGroupCode: String? = null,
        @Query("x") x: String? = null,
        @Query("y") y: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("rect") rect: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 15,
        @Query("sort") sort: String = "accuracy"
    ): Response<KakaoAddressResponse>
}