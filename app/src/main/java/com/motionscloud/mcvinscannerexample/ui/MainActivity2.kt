package com.motionscloud.mcvinscannerexample.ui

import android.Manifest
import android.R.attr.text
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.motionscloud.mcvinscannerexample.R
import com.motionscloud.vinscannersdk.analyzer.TextRecognitionAnalyzerForTest
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity2 : AppCompatActivity(), TextRecognitionAnalyzerForTest.DetectionListener {

    private var listBitmapDetected: MutableList<Triple<String, String, Bitmap>> = mutableListOf()
    private var listBitmapDetectFailed: MutableList<Triple<String, String, Bitmap>> =
        mutableListOf()
    private var listResultStr: MutableList<String> = mutableListOf()
    private val listImageAssets = mutableListOf<String>()

    private lateinit var textRecognitionAnalyzerForTest: TextRecognitionAnalyzerForTest

    var bmCount = 0

    private val imageView: ImageView by lazy { findViewById<ImageView>(R.id.imageView) }
    private val btnStart: Button by lazy { findViewById(R.id.btnStart) }

    private val filename = "VinSdkLog.txt"
    private val filepath = "VinSdkStorage"

    var myExternalFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)



        textRecognitionAnalyzerForTest =
            TextRecognitionAnalyzerForTest(this, this) { resultSts, imageBm ->

            }.apply {
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

        btnStart.setOnClickListener {
            getImage(this).map { it ->
                "image/$it"
            }.let {
                bmCount = 0
                listImageAssets.addAll(it)
                loadDataTest()
            }
        }
        readFromFile(this)

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {

        } else {
            val root =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString()
            val myDir = File("$root/$filepath")
            myExternalFile = File(myDir, filename)
        }

    }

    private fun loadDataTest() {
        if (bmCount < listImageAssets.size-2) {
            Log.d("zxcvbnm,", "Run data continue: ${bmCount} - ${listImageAssets.size}")
            startTestRun(listImageAssets[bmCount])
        } else {
            Log.d("qazxcvb", "Run data success: ${listBitmapDetected}")
            Log.d("qazxcvb", "Run data failed: ${listBitmapDetectFailed}")
//            writeToFile("${listBitmapDetected}", this)
            prepareDataToSaveLog()

        }
    }

    @Throws(IOException::class)
    private fun getImage(context: Context): MutableList<String> {
        val assetManager: AssetManager = context.getAssets()
        val files: Array<String> = assetManager.list("image") as Array<String>
        return files.toMutableList()
    }

    private fun startTestRun(path: String) {
        Handler().postDelayed(Runnable {
            val inputStream: InputStream = assets.open(path)
            val bmp = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(bmp)
            textRecognitionAnalyzerForTest.analyze(bmp)
        }, 200)

    }

    override fun detectSuccess(resultStr: String, imgBox: Bitmap) {
        imgBox.let {
            Log.d("zxcvbnm,.", "detect success: $resultStr")
            listBitmapDetected.add(Triple(listImageAssets[bmCount], resultStr, imgBox))
            listResultStr.add(resultStr)
        }
        bmCount++
        loadDataTest()
    }

    override fun detectFailed(simpleText: String, imageFailed: Bitmap) {
        listBitmapDetectFailed.add(Triple(listImageAssets[bmCount], simpleText, imageFailed))
        Log.d("zxcvbnm,.", "detect fail: ${listImageAssets[bmCount]}")
        bmCount++
        loadDataTest()
    }

    private fun readFromFile(context: Context): String? {
        var ret = ""
        try {
            val inputStream: InputStream? = context.openFileInput("config.txt")
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append("\n").append(receiveString)
                }
                inputStream.close()
                ret = stringBuilder.toString()
                Log.d("ReadFromFile,", "ReadFromFile: $ret")
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: " + e.toString())
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: $e")
        }
        return ret
    }


    private fun writeToFile(data: String, context: Context) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    private fun isExternalStorageReadOnly(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
    }

    private fun isExternalStorageAvailable(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == extStorageState
    }

    private fun prepareDataToSaveLog() {

        val detectedStr = StringBuffer();
        listBitmapDetected.map {
            detectedStr.append(
                "Detect success:\nImage file name: ${it.first}\n${it.second}\n" +
                        "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n"
            )
        }
        val failedStr = StringBuffer()
        listBitmapDetectFailed.map {
            failedStr.append(
                "Detect Failed:\nImage file name: ${it.first}\n${it.second}\n" +
                        "------------------------------------------------------------------------------------------\n"
            )
        }
        val timeStamp =
            "${SimpleDateFormat("yyyy/MM/dd - HH:mm:ss").format(Date())}\nSuccess: ${listBitmapDetected.size}\nFailed: ${listBitmapDetectFailed.size}\n\n===\n\n"
        val textSave = StringBuffer().append(timeStamp).append("$detectedStr \n").append(failedStr)

        Log.d("prepareDataToSaveLog,", textSave.toString())
        Log.d(
            "prepareDataToSaveLog,",
            "isExternalStorageAvailable: ${isExternalStorageAvailable()}"
        )
        if (isExternalStorageAvailable()) {
//            val oldLog = StringBuffer(readFileFromExternal())
            saveFileToExternal(textSave.toString())
        } else {
            Log.d(
                "prepareDataToSaveLog,",
                "isExternalStorageAvailable: ${isExternalStorageAvailable()}"
            )
        }
    }

    private fun saveFileToExternal(dataStr: String) {
        try {
            val fos = FileOutputStream(myExternalFile)
            fos.write(dataStr.toByteArray())
            fos.flush()
            fos.close()


//            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "$filename")
//            val fileOutput = FileOutputStream(file)
//            val outputStreamWriter = OutputStreamWriter(fileOutput)
//            outputStreamWriter.write(dataStr)
//            outputStreamWriter.flush()
//            fileOutput.fd.sync()
//            outputStreamWriter.close()
//            writeFile(dataStr)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readFileFromExternal(): String {
        var result = "\n"
        try {
            val fis = FileInputStream(myExternalFile)
            val inputStream = DataInputStream(fis)
            val br = BufferedReader(InputStreamReader(inputStream))
            var strLine: String
            while (br.readLine().also { strLine = it ?: "" } != null) {
                result += "$strLine\n"
            }
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }

    fun isExternalStorageWritable(): Boolean {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            Log.i("State", "Yes it is writable")
            true
        } else {
            Log.i("State", "No it is not writable")
            false
        }
    }

    fun writeFile(text: String) {
        val fileName = "myFile.txt"
        if (isExternalStorageWritable() && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            //File file = new File(path, "/" + fileName);
            val myFile = File(Environment.getExternalStorageDirectory(), fileName)
            try {
                val fos = FileOutputStream(myFile)
                fos.write(text.toByteArray())
                fos.close()
                //fos.write(myText);
                Toast.makeText(this, "File Saved", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Cannot write to external storage", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkPermission(permission: String?): Boolean {
        val check = ContextCompat.checkSelfPermission(this, permission!!)
        return check == PackageManager.PERMISSION_GRANTED
    }
}