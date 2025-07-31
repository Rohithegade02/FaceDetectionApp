package com.facedetectionapp

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.ReactContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage
class SimpleCameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "SimpleCameraView"
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isFrontCamera = false
    private val previewView: PreviewView

    init {
        Log.d(TAG, "Initializing SimpleCameraView")
        
        // Create executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Create and configure preview view
        previewView = PreviewView(context)
        previewView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        // Set implementation mode to PERFORMANCE for better performance
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        
        // Add preview view to this frame layout
        addView(previewView)
        
        // Start camera when view is ready
        post {
            Log.d(TAG, "Posted startCamera")
            startCamera()
        }
    }

    fun setCameraType(cameraType: String) {
        val newIsFrontCamera = cameraType == "front"
        if (isFrontCamera != newIsFrontCamera) {
            Log.d(TAG, "Switching camera to: ${if (newIsFrontCamera) "FRONT" else "BACK"}")
            isFrontCamera = newIsFrontCamera
            cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            
            // Restart camera with new selector
            startCamera()
        }
    }

    private fun startCamera() {
        Log.d(TAG, "Starting camera with selector: ${if (isFrontCamera) "FRONT" else "BACK"}")
        
        val reactContext = context as? ReactContext
        if (reactContext == null) {
            Log.e(TAG, "Context is not ReactContext")
            return
        }
        
        val activity = reactContext.currentActivity
        if (activity == null) {
            Log.e(TAG, "Activity is null")
            return
        }
        
        if (activity !is LifecycleOwner) {
            Log.e(TAG, "Activity is not a LifecycleOwner")
            return
        }
        
        val lifecycleOwner = activity as LifecycleOwner
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                Log.d(TAG, "Camera provider future listener called")
                
                // Get camera provider
                cameraProvider = cameraProviderFuture.get()
                if (cameraProvider == null) {
                    Log.e(TAG, "Camera provider is null")
                    return@addListener
                }
                
                Log.d(TAG, "Camera provider obtained successfully")
                
                // Unbind any existing use cases
                cameraProvider?.unbindAll()
                
                // Create preview use case
                preview = Preview.Builder()
                    .build()
                
                Log.d(TAG, "Preview use case created")
                
                try {
                    // Bind use cases to camera
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    
                    Log.d(TAG, "Camera bound to lifecycle")
                    
                    // Connect preview to preview view
                    preview?.setSurfaceProvider(previewView.surfaceProvider)
                    
                    Log.d(TAG, "Camera started successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Use case binding failed", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera provider initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "Detached from window")
        
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