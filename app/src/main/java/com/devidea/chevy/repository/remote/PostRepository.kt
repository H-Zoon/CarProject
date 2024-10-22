package com.devidea.chevy.repository.remote

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