package com.motionscloud.vinscannersdk.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.bean.OcrResult
import com.equationl.paddleocr4android.callback.OcrInitCallback
import com.equationl.paddleocr4android.callback.OcrRunCallback
import com.motionscloud.vinscannersdk.overlay.ScannerOverlay
import com.motionscloud.vinscannersdk.trackevent.LogScannedVinNumberRepository
import com.motionscloud.vinscannersdk.util.BitmapUtil
import com.motionscloud.vinscannersdk.util.FrameMetadata
import com.motionscloud.vinscannersdk.util.YuvNV21Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class TextRecognitionAnalyzerForTest(
    private val context: Context,
    private val detectionListener: DetectionListener,
    private val onRecognition: (String, Bitmap?) -> Unit,
) {

    companion object {
        const val TARGET_PREVIEW_WIDTH = 1080
        const val TARGET_PREVIEW_HEIGHT = 1920
    }

//    private val vinRegex = Pattern.compile("^[A-HJ-NPR-Za-hj-npr-z\\d]{17}$")

    private val vinRegex = Pattern.compile("^[a-zA-Z\\d]{17}$")

    private val logScannedVinNumberRepository = LogScannedVinNumberRepository(context)

    private lateinit var ocr: OCR

    fun prepareModelConfig(
        modelPathConfig: String,
        classificationModelFileName: String,
        detectionModelFileName: String,
        recognitionModelFileName: String,
        labelPathConfig: String,
        isRunDetection: Boolean,
        isRunClassification: Boolean,
        isRunRecognition: Boolean,
    ) {
        initPaddle(
            modelPathConfig,
            classificationModelFileName,
            detectionModelFileName,
            recognitionModelFileName,
            labelPathConfig,
            isRunDetection,
            isRunClassification,
            isRunRecognition
        )
    }

    fun analyze(bitmap: Bitmap) {
        if (this::ocr.isInitialized) {
            ocr.run(bitmap, object : OcrRunCallback {
                override fun onSuccess(result: OcrResult) {
                    val simpleText = result.simpleText.trim()
                    val imgWithBox = result.imgWithBox
                    val inferenceTime = result.inferenceTime
                    val outputRawResult = result.outputRawResult

                    VINTextPreProcessor.preProcess(simpleText).let { text ->

                        if (vinRegex.matcher(text).matches()) {
                            val text1 =
                                "Result: $text\nTime Detect = $inferenceTime ms\nLength: ${text.length}"
                            detectionListener.detectSuccess(text1, imgWithBox)
                            onRecognition.invoke(text1, imgWithBox)

                        } else {
                            val text1 =
                                "$text - ${text.length}\n$inferenceTime ms"
                            detectionListener.detectFailed(text1, bitmap)
                        }
                    }
                }

                override fun onFail(e: Throwable) {
                    detectionListener.detectFailed("null", bitmap)
                    Log.e("PaddleDetect", "onFail: ${e.message}")
                    onRecognition.invoke("error = " + e.message, null)

                }

            })
        }
    }


    private fun initPaddle(
        modelPathConfig: String,
        classificationModelFileName: String,
        detectionModelFileName: String,
        recognitionModelFileName: String,
        labelPathConfig: String,
        isRunDetection: Boolean,
        isRunClassification: Boolean,
        isRunRecognition: Boolean,
    ) {
        ocr = OCR(context)
        OcrConfig().apply {
            modelPath = modelPathConfig
            clsModelFilename = classificationModelFileName
            detModelFilename = detectionModelFileName
            recModelFilename = recognitionModelFileName
            labelPath = labelPathConfig
            isRunDet = isRunDetection
            isRunCls = isRunClassification
            isRunRec = isRunRecognition

            cpuPowerMode = CpuPowerMode.LITE_POWER_HIGH
            isDrwwTextPositionBox = true

            ocr.initModel(this, object : OcrInitCallback {
                override fun onSuccess() {
                    Log.i("PaddleInit", "onSuccess: paddle init")
                }

                override fun onFail(e: Throwable) {
                    Log.e("PaddleInit", "onFail: paddle init ${e.message}")
                }

            })
        }
    }


    interface DetectionListener {
        fun detectSuccess(resultStr: String, imgBox: Bitmap)

        fun detectFailed(simpleText: String, imageFailed: Bitmap)

    }
}