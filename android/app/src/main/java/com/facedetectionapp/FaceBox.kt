package com.facedetectionapp

import android.graphics.*
import com.google.mlkit.vision.face.Face

class FaceBox(
    overlay: FaceBoxOverlay,
    private val face: Face,
    private val imageRect: Rect,
    private val isFrontCamera: Boolean
) : FaceBoxOverlay.FaceBox(overlay) {

    private val defaultPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6f
        pathEffect = CornerPathEffect(10f) // Rounded corners
    }

    private val successPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        pathEffect = CornerPathEffect(10f) // Rounded corners
    }

    private val mappedRect: RectF = getBoxRect(
        imageRect.width().toFloat(),
        imageRect.height().toFloat(),
        face.boundingBox,
        isFrontCamera
    )

    override fun draw(canvas: Canvas?) {
        canvas?.takeUnless { mappedRect.isEmpty }?.let {
            val paint = if (isFaceInTargetBox(mappedRect)) successPaint else defaultPaint
            it.drawRoundRect(mappedRect, 16f, 16f, paint)
            drawFaceFeatures(it)
        }
    }

    private fun drawFaceFeatures(canvas: Canvas) {
        val featurePaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
        }

        face.allLandmarks.forEach { landmark ->
            val pos = getBoxRect(
                imageRect.width().toFloat(),
                imageRect.height().toFloat(),
                Rect(landmark.position.x.toInt(), landmark.position.y.toInt(),
                    landmark.position.x.toInt(), landmark.position.y.toInt()),
                isFrontCamera
            )
            canvas.drawCircle(pos.centerX(), pos.centerY(), 10f, featurePaint)
        }
    }

    override fun getMappedRect(): RectF = RectF(mappedRect)
}