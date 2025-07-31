package com.facedetectionapp

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

@ExperimentalGetImage
class SimpleCameraViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<SimpleCameraView>() {

    private val TAG = "SimpleCameraViewManager"

    override fun getName(): String {
        return "SimpleCameraView"
    }

    override fun createViewInstance(reactContext: ThemedReactContext): SimpleCameraView {
        Log.d(TAG, "Creating SimpleCameraView instance")
        return SimpleCameraView(reactContext)
    }
    
    @ReactProp(name = "cameraType")
    fun setCameraType(view: SimpleCameraView, cameraType: String) {
        Log.d(TAG, "Setting cameraType: $cameraType")
        view.setCameraType(cameraType)
    }
}