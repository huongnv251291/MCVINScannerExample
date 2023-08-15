package com.motionscloud.mcvinscannerexample.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.motionscloud.mcvinscannerexample.R
import com.motionscloud.mcvinscannerexample.databinding.ActivityMainBinding
import com.motionscloud.vinscannersdk.analyzer.TextRecognitionAnalyzer
import com.motionscloud.vinscannersdk.analyzer.TextRecognitionAnalyzer.Companion.TARGET_PREVIEW_HEIGHT
import com.motionscloud.vinscannersdk.analyzer.TextRecognitionAnalyzer.Companion.TARGET_PREVIEW_WIDTH
import com.motionscloud.vinscannersdk.analyzer.TextRecognitionAnalyzer.DetectionListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService
    private var torchOn: Boolean = false

    private var textAnalyzer: TextRecognitionAnalyzer? = null

    private var imageAnalysis: ImageAnalysis? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            textAnalyzer = TextRecognitionAnalyzer(
                this.applicationContext,
                viewBinding.overlayScanner,{ text, bitmap ->
                    Log.e("TextRecognitionAnalyzer", text)
                }) .apply {
                prepareModelConfig(
                    modelPathConfig = "models/ch_PP-OCRv2",
                    classificationModelFileName = "cls.nb",
                    detectionModelFileName = "detection_best_01082023.nb",
                    recognitionModelFileName = "rec.nb",
                    labelPathConfig = "labels/en_dict.txt",
                    isRunDetection = true,
                    isRunClassification = true,
                    isRunRecognition = true
                )
            }

            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        with(viewBinding){
            overlayScanner.setOnClickListener {
                if (layoutScanResult.isVisible) {
                    hideScanResult()
                    textAnalyzer?.detectionFinish()
                    startCamera()
                }
            }
           layoutScanResult.post {
                overlayScanner.updateTempY(layoutScanResult.height,getNavigationBarHeight())
            }

        }


    }
    private fun getNavigationBarHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val usableHeight: Int = metrics.heightPixels
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val realHeight: Int = metrics.heightPixels
            return if (realHeight > usableHeight) realHeight - usableHeight else 0
        }
        return 0
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetResolution(Size(TARGET_PREVIEW_WIDTH, TARGET_PREVIEW_HEIGHT))
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageAnalysis?.let {
                it.setAnalyzer(cameraExecutor, textAnalyzer!!)
            } ?: kotlin.run {
                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(TARGET_PREVIEW_WIDTH, TARGET_PREVIEW_HEIGHT))
                    .build().also {
                        textAnalyzer?.apply {
                            detectionListener = object : DetectionListener {
                                override fun detectSuccess(resultStr: String, imgBox: Bitmap) {
//                                    cameraProvider.unbind(preview)
//                                    it.clearAnalyzer()
                                    viewBinding.fabScannerTorch.isVisible = false
                                    torchOn = false
                                    showScanResult(resultStr, imgBox)

                                }

                                override fun detectFailed(error: String, imageFailed: Bitmap?) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Detect failed: $error",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }?.let { it1 ->
                            it.setAnalyzer(
                                cameraExecutor,
                                it1
                            )
                        }


//                        TextRecognitionAnalyzer(
//                            this.applicationContext,
//                            viewBinding.overlayScanner
//                        ) { text, bitmap ->
//                            cameraProvider.unbind(preview)
//                            it.clearAnalyzer()
//                            viewBinding.fabScannerTorch.isVisible = false
//                            torchOn = false
//                            showScanResult(text, bitmap)
//                        }.apply {
//                            prepareModelConfig(
//                                modelPathConfig = "models/ch_PP-OCRv2",
//                                classificationModelFileName = "cls.nb",
//                                detectionModelFileName = "detection_best_01082023.nb",
//                                recognitionModelFileName = "rec_best_24072023.nb",
//                                labelPathConfig = "labels/ar_dict_fixed.txt",
//                                isRunDetection = true,
//                                isRunClassification = true,
//                                isRunRecognition = true
//                            )
//                        }

                    }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    val camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis
                    )

                    viewBinding.fabScannerTorch.setOnClickListener {
                        torchOn = !torchOn
                        camera.cameraControl.enableTorch(torchOn)
                        setTorchUI()
                    }
                    setTorchUI()

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showScanResult(text: String, bitmap: Bitmap?) {
        with(viewBinding) {
            Log.e("showScanResult", text)
            layoutScanResult.isVisible = true
            tvScanResult.text = text
            imgScanResult.setImageBitmap(bitmap)
        }
    }

    private fun hideScanResult() {
        with(viewBinding) {
            layoutScanResult.isVisible = false
            imgScanResult.setImageBitmap(null)
            tvScanResult.text = ""
            viewBinding.fabScannerTorch.isVisible = true
        }
    }

    private fun setTorchUI() {
        viewBinding.fabScannerTorch.setImageResource(
            if (torchOn)
                R.drawable.ic_baseline_flash_off_24dp_white
            else
                R.drawable.ic_baseline_flash_on_24dp_white
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "MCVINScanner"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).toTypedArray()
    }
}