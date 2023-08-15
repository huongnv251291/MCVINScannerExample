package com.motionscloud.vinscannersdk.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View
import com.google.gson.Gson
import com.motionscloud.vinscannersdk.util.isPortrait
import com.motionscloud.vinscannersdk.util.px
import kotlin.math.min

class ScannerOverlayImpl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ScannerOverlay {

    private var heightDetected: Int =0
    private var tempY: Int = 0
    private var points: List<Point>? = null
    private var detectedRect: RectF? = null
    private var topLeftPoint: Point? = null
    private var topRightPoint: Point? = null
    private var bottomLeftPoint: Point? = null
    private var bottomRightPoint: Point? = null
    private val transparentPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private val strokePaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            strokeWidth = context.px(2f)
            style = Paint.Style.STROKE
        }
    }

    var drawBlueRect: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

//    fun drawGraphicBlocks(graphicBlocks : List<GraphicBlock>) {
//        this.graphicBlocks = graphicBlocks
//        drawBlueRect = true
//    }
//
//    private var graphicBlocks : List<GraphicBlock>? = null

    private val blueColor = Color.parseColor("#1C8DD8")

    init {
        setWillNotDraw(false)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
//        canvas.drawColor(Color.parseColor("#88000000"))

        val radius = context.px(4f)
        val rectF = scanRect
        canvas.drawRoundRect(rectF, radius, radius, transparentPaint)
        strokePaint.color = if (drawBlueRect) blueColor else Color.BLUE
        canvas.drawRoundRect(rectF, radius, radius, strokePaint)
//        detectedRect?.let {
//            val paintFillAlpha = Paint()
//            paintFillAlpha.style = Paint.Style.FILL
//            paintFillAlpha.color = Color.parseColor("#3B85F5")
//            paintFillAlpha.alpha = 50
//            canvas.drawRoundRect(it, radius, radius, strokePaint)
//            canvas.drawRoundRect(it, radius, radius, paintFillAlpha)
//        }
     points?.let {
         val points: List<Point> =it
         val path = Path()
         val paint = Paint()
         val paintFillAlpha = Paint()
         paintFillAlpha.style = Paint.Style.FILL
         paintFillAlpha.color = Color.parseColor("#3B85F5")
         paintFillAlpha.alpha = 50
         paint.color = Color.parseColor("#3B85F5")
         paint.strokeWidth = 5f
         paint.style = Paint.Style.STROKE
         if (points.isEmpty()) {
             return
         }
         val tempx = context.px(24)
         path.moveTo(points[0].x.toFloat()+tempx, points[0].y.toFloat()+tempY)
         path.lineTo(points[3].x.toFloat()+tempx, points[3].y.toFloat()+tempY)
         path.lineTo(points[2].x.toFloat()+tempx*2, points[2].y.toFloat()+tempY)
         path.lineTo(points[1].x.toFloat()+tempx*2, points[1].y.toFloat()+tempY)
         path.lineTo(points[0].x.toFloat()+tempx, points[0].y.toFloat()+tempY)
         canvas.drawPath(path, paint)
         canvas.drawPath(path, paintFillAlpha)
     }

//        graphicBlocks?.forEach { block ->
//            val scaleX = scanRect.width() / block.bitmapSize.width
//            val scaleY = scanRect.height() / block.bitmapSize.height
//
//            canvas.withTranslation(scanRect.left, scanRect.top) {
//                withScale(scaleX, scaleY) {
//                    drawRoundRect(RectF(block.textBlock.boundingBox), radius, radius, strokePaint)
//                }
//            }
//        }
//        graphicBlocks = null
    }

    override fun drawDetectedBox(points: List<Point>) {
        Log.e("drawDetectedBox", "" + Gson().toJson(points))
        try {
            this.points = points
            this.topLeftPoint = points[0]
            this.topRightPoint = points[1]
            this.bottomLeftPoint = points[2]
            this.bottomRightPoint = points[3]
            createRectDetected()
            invalidate()
        } catch (e: Throwable) {
            Log.d(TAG, "convert point fail")
            removeAllPoint()
        }
    }

    private fun createRectDetected() {
        points?.let {
            detectedRect = RectF(
                it.minOf { data -> data.x }.toFloat(),
                (it.minOf { data -> data.y } + tempY).toFloat(),
                it.maxOf { data -> data.x }.toFloat(),
                (it.maxOf { data -> data.y } + tempY).toFloat()
            )
        }

    }


    override fun removeAllPoint() {
        topLeftPoint = null
        topRightPoint = null
        bottomRightPoint = null
        bottomLeftPoint = null
        detectedRect = null
        points = null
    }

    override val size: Size
        get() = Size(width, height)

    override val scanRect: RectF
        get() = if (context.isPortrait()) {
            val rectW = min(width * 1f, MAX_WIDTH_PORTRAIT)
            val l = ((width - rectW) / 2) + (width * 0.15).toFloat()
            val r = width - l
            val t = height * 0.4f
            val b = t + rectW / getIBANOverlayHeightFactor()
            heightDetected = (b-t).toInt();
            RectF(l, t, r, b)
        } else {
            val rectW = min(width * 0.6f, MAX_WIDTH_LANDSCAPE)
            val l = width * 0.05f
            val r = l + rectW
            val t = height * 0.15f
            val b = t + rectW / getIBANOverlayHeightFactor()
            heightDetected = (b-t).toInt();
            RectF(l, t, r, b)
        }

    private fun getIBANOverlayHeightFactor(): Int {
        return 6
    }
    fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun Context.pxToDp(px: Int): Int {
        return (px / resources.displayMetrics.density).toInt()
    }
    fun updateTempY(height: Int, navigationBarHeight: Int) {
        tempY = height - context.dpToPx(24)
    }

//    data class GraphicBlock(val textBlock: BlockWrapper, val bitmapSize: Size)

    companion object {
        val TAG: String = ScannerOverlayImpl::class.java.name
        const val MAX_WIDTH_PORTRAIT = 1200f
        const val MAX_WIDTH_LANDSCAPE = 1600f
    }
}