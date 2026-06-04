package ru.chezzz.denisfacedetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    private var points = emptyList<PointF>()
    private var faceBounds = emptyList<RectF>()
    private var imageWidth = 1f
    private var imageHeight = 1f

    private val pointPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 6f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    fun update(points: List<PointF>, bounds: List<RectF>, imageWidth: Int, imageHeight: Int) {
        this.points = points
        this.faceBounds = bounds
        this.imageWidth = imageWidth.toFloat()
        this.imageHeight = imageHeight.toFloat()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (imageWidth == 0f || imageHeight == 0f) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        // Draw bounding boxes
        faceBounds.forEach { rect ->
            val mappedRect = RectF(
                (imageWidth - rect.right) * scaleX,
                rect.top * scaleY,
                (imageWidth - rect.left) * scaleX,
                rect.bottom * scaleY
            )
            canvas.drawRect(mappedRect, boxPaint)
        }

        // Draw keypoints
        points.forEach { point ->
            canvas.drawCircle((imageWidth - point.x) * scaleX, point.y * scaleY, 8f, pointPaint)
        }
    }
}