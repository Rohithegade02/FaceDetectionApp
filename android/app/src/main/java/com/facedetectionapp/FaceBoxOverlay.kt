package com.facedetectionapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val lock = Any()
    private val faceBoxes = mutableListOf<FaceBox>()
    
    private val targetBoxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = CornerPathEffect(20f) // Rounded corners
    }
    
    private val targetBoxRect = RectF()
    private val targetBoxRatio = 0.85f
    
    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boxWidth = w * targetBoxRatio
        val boxHeight = h * targetBoxRatio
        val left = (w - boxWidth) / 2f
        val top = (h - boxHeight) / 2f
        targetBoxRect.set(left, top, left + boxWidth, top + boxHeight)
    }

    abstract class FaceBox(protected val overlay: FaceBoxOverlay) {
        abstract fun draw(canvas: Canvas?)
        abstract fun getMappedRect(): RectF

        fun isFaceInTargetBox(faceRect: RectF): Boolean {
            return overlay.targetBoxRect.contains(faceRect)
        }

        protected fun getBoxRect(
            imageRectWidth: Float,
            imageRectHeight: Float,
            faceBoundingBox: Rect,
            isFrontCamera: Boolean
        ): RectF {
            val scaleX = overlay.width.toFloat() / imageRectHeight
            val scaleY = overlay.height.toFloat() / imageRectWidth
            val scale = maxOf(scaleX, scaleY)

            val offsetX = (overlay.width - imageRectHeight * scale) / 2f
            val offsetY = (overlay.height - imageRectWidth * scale) / 2f

            return RectF().apply {
                left = faceBoundingBox.left * scale + offsetX
                top = faceBoundingBox.top * scale + offsetY
                right = faceBoundingBox.right * scale + offsetX
                bottom = faceBoundingBox.bottom * scale + offsetY
                
                if (isFrontCamera) {
                    val centerX = overlay.width / 2f
                    val tmp = centerX - (right - centerX)
                    right = centerX + (centerX - left)
                    left = tmp
                }
            }
        }
    }

    fun clear() {
        synchronized(lock) { faceBoxes.clear() }
        postInvalidate()
    }

    fun add(faceBox: FaceBox) {
        synchronized(lock) { faceBoxes.add(faceBox) }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw target box with shadow
        canvas.drawRoundRect(targetBoxRect, 20f, 20f, targetBoxPaint)
        
        // Draw instruction text
        // Paint().apply {
        //     color = Color.GREEN
        //     textSize = 48f
        //     textAlign = Paint.Align.CENTER
        //     typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        // }.also { paint ->
        //     canvas.drawText("Align face here", width/2f, targetBoxRect.top - 40f, paint)
        // }
        
        // Draw face boxes
        synchronized(lock) {
            faceBoxes.forEach { it.draw(canvas) }
        }
    }

    fun getTargetBoxRect(): RectF = RectF(targetBoxRect)
}