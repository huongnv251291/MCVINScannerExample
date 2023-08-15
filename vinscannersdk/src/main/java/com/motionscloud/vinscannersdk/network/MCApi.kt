package com.motionscloud.vinscannersdk.network

import com.motionscloud.vinscannersdk.data.api.ScannedVinNumberResponse
import com.motionscloud.vinscannersdk.data.api.ScannedVinNumbersRequest
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MCApi {

    @POST("scanned_vin_numbers")
    suspend fun logScannedVinNumber(
        @Header("User-Access-token") accessToken: String = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjo3MX0.osD_fD_ehdF7rpTVByq9AaVRR9e6yvZr63hquuaLs9k",
        @Header("Accept") accept: String = "*/*",
        @Header("Content-Type") type: String = "application/json",
        @Header("Authorization") token: String = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJpbnN1cmVyX2lkIjo4NCwiZXhwIjoxNjUwOTgwMDQ3fQ.GSjxN78j_cyHmBj7TcV9kVx0vn0HUzy34f9zsdSzes8",
        @Body scannedVinNumbersRequest: ScannedVinNumbersRequest
    ): ScannedVinNumberResponse

}