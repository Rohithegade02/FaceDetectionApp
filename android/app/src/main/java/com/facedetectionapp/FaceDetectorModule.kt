package com.facedetectionapp

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.facebook.react.bridge.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

class FaceDetectorModule(reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext) {
    
    private val TAG = "FaceDetectorModule"
    private var faceDetectorOptions: FaceDetectorOptions? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var faceDetectionListener: FaceDetectionListener? = null
    
    interface FaceDetectionListener {
        fun onFacesDetected(faces: List<Face>)
    }
    
    override fun getName(): String {
        return "FaceDetectorModule"
    }
    
    @ReactMethod
    fun setupFaceDetector(config: ReadableMap, promise: Promise) {
        try {
            val optionsBuilder = FaceDetectorOptions.Builder()
            
            // Performance mode (default is fast)
            val performanceMode = if (config.hasKey("performanceMode")) {
                config.getString("performanceMode")
            } else "fast"
            
            if (performanceMode == "accurate") {
                optionsBuilder.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            } else {
                optionsBuilder.setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            }
            
            // Landmark detection
            val landmarkMode = if (config.hasKey("landmarkMode")) {
                config.getString("landmarkMode")
            } else "none"
            
            when (landmarkMode) {
                "all" -> optionsBuilder.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                else -> optionsBuilder.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            }
            
            // Contour detection
            val contourMode = if (config.hasKey("contourMode")) {
                config.getString("contourMode")
            } else "none"
            
            when (contourMode) {
                "all" -> optionsBuilder.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                else -> optionsBuilder.setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            }
            
            // Classification mode
            val classificationMode = if (config.hasKey("classificationMode")) {
                config.getString("classificationMode")
            } else "none"
            
            when (classificationMode) {
                "all" -> optionsBuilder.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                else -> optionsBuilder.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            }
            
            // Minimum face size
            if (config.hasKey("minFaceSize")) {
                val minFaceSize = config.getDouble("minFaceSize").toFloat()
                optionsBuilder.setMinFaceSize(minFaceSize)
            }
            
            // Face tracking
            if (config.hasKey("enableTracking") && config.getBoolean("enableTracking")) {
                optionsBuilder.enableTracking()
            }
            
            faceDetectorOptions = optionsBuilder.build()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SETUP_ERROR", "Failed to setup face detector: ${e.message}")
        }
    }
    
    fun setFaceDetectionListener(listener: FaceDetectionListener) {
        this.faceDetectionListener = listener
    }
    
    @ExperimentalGetImage
    fun processImage(imageProxy: ImageProxy) {
        if (faceDetectorOptions == null) {
            // Use default options if not configured
            faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            val detector = FaceDetection.getClient(faceDetectorOptions!!)
            
            detector.process(image)
                .addOnSuccessListener { faces ->
                    faceDetectionListener?.onFacesDetected(faces)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    @ReactMethod
    fun toggleCamera(promise: Promise) {
        // This will be implemented in the ViewManager
        promise.resolve(true)
    }
    
    @ReactMethod
    fun startCamera(promise: Promise) {
        // This will be implemented in the ViewManager
        promise.resolve(true)
    }
    
    @ReactMethod
    fun stopCamera(promise: Promise) {
        // This will be implemented in the ViewManager
        promise.resolve(true)
    }
}