package com.devidea.chevy.network

import com.devidea.chevy.network.reomote.KakaoAddressResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun searchAddress(query: String, page: Int, size: Int): KakaoAddressResponse? {
        val response = apiService.searchAddress(query = query, page = page, size = size)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }
}