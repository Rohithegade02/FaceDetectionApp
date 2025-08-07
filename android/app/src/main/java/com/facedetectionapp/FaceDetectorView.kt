package com.facedetectionapp

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.google.mlkit.vision.face.Face
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
// Main camera implementation using CameraX with face detection integration
@ExperimentalGetImage
class FaceDetectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), FaceDetectorModule.FaceDetectionListener, TextureView.SurfaceTextureListener {

    private val TAG = "FaceDetectorView"
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val textureView: TextureView
    private val faceOverlayView: FaceOverlayView
    private var isFrontCamera = false
    private var isDetecting = true
    private var previewWidth = 0
    private var previewHeight = 0
    private var surfaceProvider: Preview.SurfaceProvider? = null
    
    private var faceDetectorModule: FaceDetectorModule? = null
    private val reactContext = context as? ReactContext

    init {
        // Create executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Create and configure texture view
        textureView = TextureView(context)
        textureView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        textureView.surfaceTextureListener = this
        addView(textureView)
        
        // Create face overlay view
        faceOverlayView = FaceOverlayView(context)
        faceOverlayView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        addView(faceOverlayView)
    }
    
    fun setCameraType(cameraType: String) {
        val newIsFrontCamera = cameraType == "front"
        if (isFrontCamera != newIsFrontCamera) {
            isFrontCamera = newIsFrontCamera
            cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
    }
    
    fun setIsDetecting(detecting: Boolean) {
        isDetecting = detecting
    }
    
    fun setFaceDetectorModule(module: FaceDetectorModule) {
        faceDetectorModule = module
        faceDetectorModule?.setFaceDetectionListener(this)
    }

    private fun startCamera() {
        val activity = reactContext?.currentActivity as? LifecycleOwner ?: return
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.unbindAll()
                
                // Create preview use case
                preview = Preview.Builder().build()
                
                // Create image analyzer use case for face detection
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (isDetecting) {
                                previewWidth = imageProxy.width
                                previewHeight = imageProxy.height
                                faceDetectorModule?.processImage(imageProxy) ?: imageProxy.close()
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    activity,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                
                // Connect preview to texture view surface provider
                surfaceProvider?.let { preview?.setSurfaceProvider(it) }
                
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    override fun onFacesDetected(faces: List<Face>) {
        // Send face coordinates to React Native
        val faceCoordinates = faces.mapIndexed { index, face ->
            val bounds = face.boundingBox
            mapOf(
                "id" to index,
                "x" to bounds.left,
                "y" to bounds.top,
                "width" to bounds.width(),
                "height" to bounds.height()
            )
        }
        
        // Send event to React Native
        reactContext?.let { context ->
            val eventData = com.facebook.react.bridge.Arguments.createMap().apply {
                putArray("faces", com.facebook.react.bridge.Arguments.createArray().apply {
                    faceCoordinates.forEach { face ->
                        pushMap(com.facebook.react.bridge.Arguments.createMap().apply {
                            putInt("id", face["id"] as Int)
                            putInt("x", face["x"] as Int)
                            putInt("y", face["y"] as Int)
                            putInt("width", face["width"] as Int)
                            putInt("height", face["height"] as Int)
                        })
                    }
                })
            }
            
            context.getJSModule(com.facebook.react.uimanager.events.RCTEventEmitter::class.java)
                ?.receiveEvent(id, "onFacesDetected", eventData)
        }
        
        post {
            faceOverlayView.updateFaces(faces, previewWidth, previewHeight, isFrontCamera)
        }
    }
    
    // TextureView.SurfaceTextureListener methods
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceProvider = Preview.SurfaceProvider { request ->
            val texture = surface.apply {
                setDefaultBufferSize(request.resolution.width, request.resolution.height)
            }
            val outputSurface = Surface(texture)
            request.provideSurface(outputSurface, cameraExecutor) { }
        }
        startCamera()
    }
    
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
    
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            cameraProvider?.unbindAll()
            if (::cameraExecutor.isInitialized) {
                cameraExecutor.shutdown()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera", e)
        }
    }
}