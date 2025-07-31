package com.facedetectionapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var faces: List<Face> = emptyList()
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var widthScaleFactor = 1.0f
    private var heightScaleFactor = 1.0f
    private var isFrontCamera = false

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8.0f
    }

    fun updateFaces(faces: List<Face>, previewWidth: Int, previewHeight: Int, isFrontCamera: Boolean) {
        this.faces = faces
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        this.isFrontCamera = isFrontCamera
        
        if (width > 0 && height > 0 && previewWidth > 0 && previewHeight > 0) {
            widthScaleFactor = width.toFloat() / previewWidth.toFloat()
            heightScaleFactor = height.toFloat() / previewHeight.toFloat()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for (face in faces) {
            val bounds = face.boundingBox
            
            // Calculate scaled bounds
            val scaledLeft = bounds.left * widthScaleFactor
            val scaledTop = bounds.top * heightScaleFactor
            val scaledRight = bounds.right * widthScaleFactor
            val scaledBottom = bounds.bottom * heightScaleFactor
            
            // If using front camera, flip the bounding box horizontally
            val finalBounds = if (isFrontCamera) {
                android.graphics.RectF(
                    width - scaledRight,
                    scaledTop,
                    width - scaledLeft,
                    scaledBottom
                )
            } else {
                android.graphics.RectF(
                    scaledLeft,
                    scaledTop,
                    scaledRight,
                    scaledBottom
                )
            }
            
            // Draw bounding box
            canvas.drawRect(finalBounds, boxPaint)
        }
    }
}