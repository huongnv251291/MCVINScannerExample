package com.motionscloud.vinscannersdk.analyzer

import java.util.*

object VINTextPreProcessor {

    fun preProcess(raw : String) : String {
        return raw
            .replace("×","")
            .replace(" ", "")
            .replace("-", "")
            .replace("I", "1")
            .replace("i", "1")
            .replace("O", "0")
            .replace("o", "0")
            .replace("Q", "0")
            .replace("q", "9")
            .replace("$", "S")
            .replace("\n", "")
            .toUpperCase(Locale.ENGLISH)
    }
}