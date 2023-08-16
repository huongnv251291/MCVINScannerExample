package com.motionscloud.vinscannersdk.overlay

import android.graphics.Point
import android.graphics.RectF
import android.util.Size

interface ScannerOverlay {
    fun drawDetectedBox(points: List<Point>)

    val size : Size

    val scanRect : RectF
    fun removeAllPoint()
}