package com.motionscloud.vinscannersdk.trackevent

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.motionscloud.vinscannersdk.data.api.ScannedVinNumber
import com.motionscloud.vinscannersdk.data.api.ScannedVinNumbersRequest
import com.motionscloud.vinscannersdk.network.MCApi
import com.motionscloud.vinscannersdk.network.RetrofitHelper
import java.util.concurrent.TimeUnit


class LogScannedVinNumberRepository(private val context: Context) {

    companion object {
        const val SCANNED_VIN_NUMBER_PREF = "scanned_vin_number"
        private const val KEY_SCANNED_VIN_NUMBERS = "SCANNED_VIN_NUMBERS"
    }

    suspend fun logScannedVinNumber(vinNumber: String) {
        try {
            val mcApi = RetrofitHelper.getInstance().create(MCApi::class.java)
            val sharePref =
                context.getSharedPreferences(SCANNED_VIN_NUMBER_PREF, Context.MODE_PRIVATE)
            val editor = sharePref.edit()

            val newScannedVinNumber = ScannedVinNumber(
                vinNumber,
                TimeUnit.MICROSECONDS.toSeconds(System.currentTimeMillis()).toString()
            )

            val scannedVinNumbersJson = sharePref.getString(KEY_SCANNED_VIN_NUMBERS, "[]") ?: "[]"
            val gson = Gson()
            val type = object : TypeToken<List<ScannedVinNumber?>?>() {}.type

            val scannedVinNumbers = mutableListOf<ScannedVinNumber>()
            val jsonList = gson.fromJson<List<ScannedVinNumber>>(scannedVinNumbersJson, type)
            scannedVinNumbers.addAll(jsonList)

            scannedVinNumbers.add(newScannedVinNumber)

            try {
                mcApi.logScannedVinNumber(
                    scannedVinNumbersRequest = ScannedVinNumbersRequest(
                        scannedVinNumbers = scannedVinNumbers
                    )
                )
                // clear all unlog vin numbers cache
                editor.putString(KEY_SCANNED_VIN_NUMBERS, "[]")
                editor.apply()
            } catch (ex: Exception) {
                // save unlog scanned vin numbers
                editor.putString(KEY_SCANNED_VIN_NUMBERS, gson.toJson(scannedVinNumbers))
                editor.apply()
            }
        } catch (ex: Exception) {
            Log.e("LogScannedVinNumber", "error logging",ex)
        }


    }
}