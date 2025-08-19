package com.facedetectionapp;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableArray;


public class FaceDetectionModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;

    public FaceDetectionModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @NonNull
    @Override
    public String getName() {
        return "FaceDetectionModule";  // Module name used in React Native
    }

    // @ReactMethod
    // public void startFaceDetection(String cameraType, boolean shouldStartDetection) {
    //     Activity activity = getCurrentActivity();
    //     if (activity != null && shouldStartDetection) {
    //         Intent intent = new Intent(activity, FaceDetectionActivity.class);
    //         intent.putExtra("cameraType", cameraType); // either "front" or "back"
    //         activity.startActivity(intent);
    //     }
    // }

    // @ReactMethod
    // public void stopFaceDetection() {
    //     Activity activity = getCurrentActivity();
    //     if (activity != null && activity.getClass().getSimpleName().equals("FaceDetectionActivity")) {
    //         activity.finish();
    //     }
    // }




    // Required methods for NativeEventEmitter compatibility
    @ReactMethod
    public void addListener(String eventName) {
        // No-op: Required by RN for NativeEventEmitter
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // No-op: Required by RN for NativeEventEmitter
    }


    // Method to send face data to JS
    public static void sendEvent(String eventName, @Nullable WritableArray params) {
        if (reactContext != null) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        }
    }
}