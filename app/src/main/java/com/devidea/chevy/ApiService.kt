package com.devidea.chevy

import com.devidea.chevy.response.KakaoAddressResponse
import dagger.Provides
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


interface ApiService {
    @GET("v2/local/search/keyword.json")
    suspend fun searchAddress(
        @Query("query") query: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<KakaoAddressResponse>
}