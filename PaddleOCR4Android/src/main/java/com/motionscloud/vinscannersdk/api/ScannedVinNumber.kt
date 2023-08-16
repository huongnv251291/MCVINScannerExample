package com.motionscloud.vinscannersdk.data.api

import com.google.gson.annotations.SerializedName

data class ScannedVinNumber(
    @SerializedName("scanned_vin")
    val scannedVin: String,
    @SerializedName("timestamp")
    val timestamp: String
)