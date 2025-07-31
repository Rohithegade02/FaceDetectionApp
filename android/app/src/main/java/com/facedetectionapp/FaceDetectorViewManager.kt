package com.facedetectionapp

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

@ExperimentalGetImage
class FaceDetectorViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<FaceDetectorView>() {

    private val TAG = "FaceDetectorViewManager"

    override fun getName(): String {
        return "FaceDetectorView"
    }

    override fun createViewInstance(reactContext: ThemedReactContext): FaceDetectorView {
        val view = FaceDetectorView(reactContext)
        
        // Set up face detector module
        try {
            val faceDetectorModule = FaceDetectorModule(this.reactContext)
            view.setFaceDetectorModule(faceDetectorModule)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating FaceDetectorModule", e)
        }
        
        return view
    }
    
    @ReactProp(name = "isDetecting")
    fun setIsDetecting(view: FaceDetectorView, isDetecting: Boolean) {
        view.setIsDetecting(isDetecting)
    }
    
    @ReactProp(name = "cameraType")
    fun setCameraType(view: FaceDetectorView, cameraType: String) {
        view.setCameraType(cameraType)
    }
    
    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return com.facebook.react.common.MapBuilder.builder<String, Any>()
            .put("onFacesDetected", com.facebook.react.common.MapBuilder.of(
                "registrationName", "onFacesDetected"))
            .build()
    }
}