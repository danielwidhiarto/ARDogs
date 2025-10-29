package com.example.ardogs

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        style = Paint.Style.FILL
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var detections = listOf<Detection>()
    private var sourceImageWidth = 0
    private var sourceImageHeight = 0

    // Color palette untuk berbagai ras anjing
    private val colors = listOf(
        Color.rgb(255, 87, 34),   // Deep Orange
        Color.rgb(76, 175, 80),   // Green
        Color.rgb(33, 150, 243),  // Blue
        Color.rgb(255, 193, 7),   // Amber
        Color.rgb(156, 39, 176),  // Purple
        Color.rgb(0, 188, 212),   // Cyan
        Color.rgb(255, 152, 0),   // Orange
        Color.rgb(139, 195, 74),  // Light Green
        Color.rgb(244, 67, 54),   // Red
        Color.rgb(103, 58, 183)   // Deep Purple
    )

    fun setDetections(newDetections: List<Detection>) {
        detections = newDetections
        invalidate()
    }

    fun setDetections(newDetections: List<Detection>, srcWidth: Int, srcHeight: Int) {
        detections = newDetections
        sourceImageWidth = srcWidth
        sourceImageHeight = srcHeight
        android.util.Log.d("BoundingBoxOverlay", "Set detections: ${newDetections.size}, source: ${srcWidth}x${srcHeight}")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (sourceImageWidth == 0 || sourceImageHeight == 0) {
            // No source dimensions set, draw boxes as-is (might be incorrect)
            drawDetections(canvas, detections)
            return
        }

        // Calculate scaling factors from source image to view
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scaleX = viewWidth / sourceImageWidth
        val scaleY = viewHeight / sourceImageHeight

        android.util.Log.d("BoundingBoxOverlay", "View: ${viewWidth}x${viewHeight}, Source: ${sourceImageWidth}x${sourceImageHeight}, Scale: ${scaleX}x${scaleY}")

        // Scale detections to view coordinates
        val scaledDetections = detections.map { detection ->
            val scaledBbox = RectF(
                detection.bbox.left * scaleX,
                detection.bbox.top * scaleY,
                detection.bbox.right * scaleX,
                detection.bbox.bottom * scaleY
            )
            Detection(scaledBbox, detection.confidence, detection.classId, detection.className)
        }

        drawDetections(canvas, scaledDetections)
    }

    private fun drawDetections(canvas: Canvas, detectionsToDraw: List<Detection>) {
        detectionsToDraw.forEach { detection ->
            val color = colors[detection.classId % colors.size]
            boxPaint.color = color
            fillPaint.color = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color))

            // Draw semi-transparent filled box
            canvas.drawRect(detection.bbox, fillPaint)

            // Draw bounding box border
            canvas.drawRect(detection.bbox, boxPaint)

            // Prepare label text
            val confidence = (detection.confidence * 100).toInt()
            val label = "${detection.className.replace("_", " ")} $confidence%"

            // Measure text
            val textBounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)

            // Calculate label background position
            val labelLeft = detection.bbox.left
            val labelTop = detection.bbox.top - textBounds.height() - 20f
            val labelRight = labelLeft + textBounds.width() + 20f
            val labelBottom = detection.bbox.top

            // Draw label background
            backgroundPaint.color = color
            val labelRect = RectF(labelLeft, labelTop, labelRight, labelBottom)
            canvas.drawRoundRect(labelRect, 8f, 8f, backgroundPaint)

            // Draw label text
            canvas.drawText(
                label,
                labelLeft + 10f,
                labelBottom - 10f,
                textPaint
            )

            // Draw corner accents for better visibility
            drawCornerAccents(canvas, detection.bbox, color)
        }
    }

    private fun drawCornerAccents(canvas: Canvas, rect: RectF, color: Int) {
        val accentPaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val cornerLength = 40f

        // Top-left corner
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLength, rect.top, accentPaint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLength, accentPaint)

        // Top-right corner
        canvas.drawLine(rect.right, rect.top, rect.right - cornerLength, rect.top, accentPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLength, accentPaint)

        // Bottom-left corner
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLength, rect.bottom, accentPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - cornerLength, accentPaint)

        // Bottom-right corner
        canvas.drawLine(rect.right, rect.bottom, rect.right - cornerLength, rect.bottom, accentPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLength, accentPaint)
    }
}

