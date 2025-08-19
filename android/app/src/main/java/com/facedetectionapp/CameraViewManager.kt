package com.facedetectionapp

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.uimanager.ViewManagerDelegate

class CameraViewManager : SimpleViewManager<CameraView>() {
    override fun getName(): String = "CameraView"

    override fun createViewInstance(reactContext: ThemedReactContext): CameraView {
        return CameraView(reactContext)
    }

    @ReactProp(name = "cameraType")
    fun setCameraType(view: CameraView, type: String) {
        view.startCamera(type)
    }

    @ReactProp(name = "detectionEnabled")
    fun setDetectionEnabled(view: CameraView, enabled: Boolean) {
        view.setDetectionEnabled(enabled)
    }

    override fun getCommandsMap(): Map<String, Int> {
        return mapOf("takePicture" to 1)
    }

    override fun receiveCommand(view: CameraView, commandId: Int, args: ReadableArray?) {
        when (commandId) {
            1 -> view.capturePhoto()
        }
    }
}