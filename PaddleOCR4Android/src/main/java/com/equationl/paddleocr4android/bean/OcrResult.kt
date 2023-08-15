package com.equationl.paddleocr4android.bean

import android.graphics.Bitmap
import com.equationl.paddleocr4android.Util.paddle.OcrResultModel
import java.util.ArrayList

data class OcrResult(
    val simpleText: String,
    val inferenceTime: Float,
    val imgWithBox: Bitmap,
    val outputRawResult: ArrayList<OcrResultModel>,
)
