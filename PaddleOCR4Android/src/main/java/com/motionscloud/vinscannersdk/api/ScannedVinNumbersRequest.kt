package com.motionscloud.vinscannersdk.data.api


import com.google.gson.annotations.SerializedName

data class ScannedVinNumbersRequest(
    @SerializedName("scanned_vin_numbers")
    val scannedVinNumbers: List<ScannedVinNumber>
)