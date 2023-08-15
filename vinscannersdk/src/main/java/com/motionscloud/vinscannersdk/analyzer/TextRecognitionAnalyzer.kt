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
import java.util.UUID
import java.util.regex.Pattern

class TextRecognitionAnalyzer(
    private val context: Context,
    private val scannerOverlay: ScannerOverlay,
    private val onRecognition: (String, Bitmap?) -> Unit,
    private var onDrawDetectedBox: ((OcrResult) -> Unit)? =null,
) :
    ImageAnalysis.Analyzer {

    companion object {
        const val TARGET_PREVIEW_WIDTH = 1080
        const val TARGET_PREVIEW_HEIGHT = 1920
    }

    private val vinRegex = Pattern.compile("^[A-HJ-NPR-Za-hj-npr-z\\d]{17}$")

    private val logScannedVinNumberRepository = LogScannedVinNumberRepository(context)

    private lateinit var ocr: OCR

    var detectionListener: DetectionListener? = null

    var uuid: String = UUID.randomUUID().toString()

    var vinCodeDetected: String = ""

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

    override fun analyze(imageProxy: ImageProxy) {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val scannerRect =
            getScannerRectToPreviewViewRelation(Size(imageProxy.width, imageProxy.height), rotation)

        val image = imageProxy.image!!
        val cropRect = image.getCropRectAccordingToRotation(scannerRect, rotation)
        image.cropRect = cropRect
        val byteArray = YuvNV21Util.yuv420toNV21(image)

        val bitmap = BitmapUtil.getBitmap(
            byteArray,
            FrameMetadata(cropRect.width(), cropRect.height(), rotation)
        )

        if (this::ocr.isInitialized) {
            ocr.run(BitmapUtil.getBitmap(
                byteArray,
                FrameMetadata(cropRect.width(), cropRect.height(), rotation)
            ), object : OcrRunCallback {
                override fun onSuccess(result: OcrResult) {
                    val simpleText = result.simpleText.trim()
                    val imgWithBox = result.imgWithBox
                    val inferenceTime = result.inferenceTime
                    val outputRawResult = result.outputRawResult

                    VINTextPreProcessor.preProcess(simpleText).let { text ->
                        if (vinRegex.matcher(text).matches()) {
                            val text1 =
                                "Result: \n$text\nTime Detect=$inferenceTime ms\nLength: ${text.length}"
                            Log.d("PaddleDetect", "onSuccess: $text1")
                            vinCodeDetected = text
                            detectionListener?.detectSuccess(text, imgWithBox)
                            onRecognition.invoke(simpleText, imgWithBox)
                            if(result.outputRawResult.size>0){
                                scannerOverlay.drawDetectedBox(result.outputRawResult[0].points)
                            }
                            onDrawDetectedBox?.invoke(result)
                        }
                    }
                    imageProxy.close()
                }

                override fun onFail(e: Throwable) {
                    Log.e("PaddleDetect", "onFail: ${e.message}")
                    detectionListener?.detectFailed("error = " + e.message, null)
                    onRecognition.invoke("error = " + e.message, null)
                }

            })
        }
    }

    private fun logScannedVinNumber(resultText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            logScannedVinNumberRepository.logScannedVinNumber(resultText)
        }
    }

    private fun getScannerRectToPreviewViewRelation(
        proxySize: Size,
        rotation: Int
    ): ScannerRectToPreviewViewRelation {
        return when (rotation) {
            0, 180 -> {
                val size = scannerOverlay.size
                val width = size.width
                val height = size.height
                val previewHeight = width / (proxySize.width.toFloat() / proxySize.height)
                val heightDeltaTop = (previewHeight - height) / 2

                val scannerRect = scannerOverlay.scanRect
                val rectStartX = scannerRect.left
                val rectStartY = heightDeltaTop + scannerRect.top

                ScannerRectToPreviewViewRelation(
                    rectStartX / width,
                    rectStartY / previewHeight,
                    scannerRect.width() / width,
                    scannerRect.height() / previewHeight
                )
            }
            90, 270 -> {
                val size = scannerOverlay.size
                val width = size.width
                val height = size.height
                val previewWidth = height / (proxySize.width.toFloat() / proxySize.height)
                val widthDeltaLeft = (previewWidth - width) / 2

                val scannerRect = scannerOverlay.scanRect
                val rectStartX = widthDeltaLeft + scannerRect.left
                val rectStartY = scannerRect.top

                ScannerRectToPreviewViewRelation(
                    rectStartX / previewWidth,
                    rectStartY / height,
                    scannerRect.width() / previewWidth,
                    scannerRect.height() / height
                )
            }
            else -> throw IllegalArgumentException("Rotation degree ($rotation) not supported!")
        }
    }

    data class ScannerRectToPreviewViewRelation(
        val relativePosX: Float,
        val relativePosY: Float,
        val relativeWidth: Float,
        val relativeHeight: Float
    )

    private fun Image.getCropRectAccordingToRotation(
        scannerRect: ScannerRectToPreviewViewRelation,
        rotation: Int
    ): Rect {
        return when (rotation) {
            0 -> {
                val startX = (scannerRect.relativePosX * this.width).toInt()
                val numberPixelW = (scannerRect.relativeWidth * this.width).toInt()
                val startY = (scannerRect.relativePosY * this.height).toInt()
                val numberPixelH = (scannerRect.relativeHeight * this.height).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            90 -> {
                val startX = (scannerRect.relativePosY * this.width).toInt()
                val numberPixelW = (scannerRect.relativeHeight * this.width).toInt()
                val numberPixelH = (scannerRect.relativeWidth * this.height).toInt()
                val startY =
                    height - (scannerRect.relativePosX * this.height).toInt() - numberPixelH
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            180 -> {
                val numberPixelW = (scannerRect.relativeWidth * this.width).toInt()
                val startX =
                    (this.width - scannerRect.relativePosX * this.width - numberPixelW).toInt()
                val numberPixelH = (scannerRect.relativeHeight * this.height).toInt()
                val startY =
                    (height - scannerRect.relativePosY * this.height - numberPixelH).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            270 -> {
                val numberPixelW = (scannerRect.relativeHeight * this.width).toInt()
                val numberPixelH = (scannerRect.relativeWidth * this.height).toInt()
                val startX =
                    (this.width - scannerRect.relativePosY * this.width - numberPixelW).toInt()
                val startY = (scannerRect.relativePosX * this.height).toInt()
                Rect(startX, startY, startX + numberPixelW, startY + numberPixelH)
            }
            else -> throw IllegalArgumentException("Rotation degree ($rotation) not supported!")
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

    fun detectionFinish() {
        if (vinCodeDetected.isNotEmpty())
            Log.d("zxcvbnm,.", "$vinCodeDetected\n$uuid")
    }

    interface DetectionListener {
        fun detectSuccess(resultStr: String, imgBox: Bitmap)

        fun detectFailed(error: String, imageFailed: Bitmap?)

    }
}