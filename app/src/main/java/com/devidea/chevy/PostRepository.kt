package com.devidea.chevy

import com.devidea.chevy.response.KakaoAddressResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun searchAddress(query: String?, page: Int, size: Int): KakaoAddressResponse? {
        val response = apiService.searchAddress(query, page, size)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }
}