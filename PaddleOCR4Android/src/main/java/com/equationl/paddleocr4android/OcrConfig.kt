package com.equationl.paddleocr4android

data class OcrConfig(
    /**
     * model path
     * from assests
     * */
    var modelPath: String = "models/ocr_v2_for_cpu",
    /**
     * label path
     * */
    var labelPath: String? = "labels/ar_dict_fixed.txt",
    /**
     * cpu threads
     * */
    var cpuThreadNum: Int = 8,
    /**
     * cpu power model
     * */
    var cpuPowerMode: CpuPowerMode = CpuPowerMode.LITE_POWER_HIGH,
    /**
     * Score Threshold
     * */
    var scoreThreshold: Float = 0.1f,

    var detLongSize: Int = 960,

    /**
     * detect file name
     * */
    var detModelFilename: String = "",

    /**
     * recognize file name
     * */
    var recModelFilename: String = "",

    /**
     * Classification file name
     * */
    var clsModelFilename: String = "",

    /**
     * is run detection model
     * */
    var isRunDet: Boolean = true,

    /**
     * is run classification model
     * */
    var isRunCls: Boolean = true,

    /**
     * is run Recognition model
     * */
    var isRunRec: Boolean = true,

    var isUseOpencl: Boolean = false,

    /**
     *
     *  bitmap with box
     * */
    var isDrwwTextPositionBox: Boolean = false
)

enum class CpuPowerMode {
    /**
     * HIGH(only big cores)
     * */
    LITE_POWER_HIGH,

    /**
     * LOW(only LITTLE cores)
     * */
    LITE_POWER_LOW,

    /**
     * FULL(all cores)
     * */
    LITE_POWER_FULL,

    /**
     * NO_BIND(depends on system)
     * */
    LITE_POWER_NO_BIND,

    /**
     * RAND_HIGH
     * */
    LITE_POWER_RAND_HIGH,

    /**
     * RAND_LOW
     * */
    LITE_POWER_RAND_LOW
}