package com.motionscloud.vinscannersdk.data.api


import com.google.gson.annotations.SerializedName

data class ScannedVinNumberResponse(
    @SerializedName("message")
    val message: String
)