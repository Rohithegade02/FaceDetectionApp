package com.facedetectionapp


// Add these imports at the top of your CameraView.kt
import android.view.LayoutInflater
import android.util.TypedValue
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.graphics.RectF
import android.graphics.Color
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.io.File
import android.view.ViewGroup
import android.view.ViewGroup.OnHierarchyChangeListener
import com.facebook.react.uimanager.ThemedReactContext
import com.facedetectionapp.R  // Make sure this import exists for your custom layout



class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val previewView = PreviewView(context)
    private val overlayView = FaceBoxOverlay(context, null)
    private val reactContext = context as? ReactContext

    private var faceDetector: FaceDetector
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraSelector: CameraSelector
    
    private var isFrontCamera = true  // Track camera type
    private var isDetectionEnabled = true
    private var hasStartedCamera = false
    private var receivedCameraType: String? = null
    private var currentToast: Toast? = null // Keep reference to cancel previous toast
    private var lastToastTime = 0L


    init {
        previewView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        previewView.id = View.generateViewId()
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        overlayView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        overlayView.setBackgroundColor(Color.TRANSPARENT)

        setBackgroundColor(Color.BLACK)
        addView(previewView)
        addView(overlayView)
        installHierarchyFitter(previewView)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .enableTracking()
            .build()

        faceDetector = FaceDetection.getClient(options)
    }

    fun startCamera(cameraType: String) {
        Log.d("CameraView", "Starting camera with type: $cameraType")

        hasStartedCamera = true
        receivedCameraType = cameraType

        isFrontCamera = cameraType != "back"
        val lensFacing = if (cameraType == "back") CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                post { bindCamera() }
            } catch (e: Exception) {
                Log.e("CameraView", "Error binding camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setDetectionEnabled(enabled: Boolean) {
        isDetectionEnabled = enabled
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!hasStartedCamera) {
            startCamera(receivedCameraType ?: "front")
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cameraProvider?.unbindAll()
    }

    private fun installHierarchyFitter(view: ViewGroup) {
    if (context is ThemedReactContext) { // only react-native setup
        view.setOnHierarchyChangeListener(object : OnHierarchyChangeListener{
        override fun onChildViewRemoved(parent: View?, child: View?) = Unit
        override fun onChildViewAdded(parent: View?, child: View?) {
            parent?.measure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
            )
            parent?.layout(0, 0, parent.measuredWidth, parent.measuredHeight)
        }
        })
    }
    }


    private fun bindCamera() {
        val lifecycleOwner = reactContext?.currentActivity as? LifecycleOwner
        if (lifecycleOwner == null) {
            Log.e("CameraView", "LifecycleOwner is null")
            return
        }

        cameraProvider?.unbindAll()

        val preview = Preview.Builder()
            .setTargetResolution(Size(1280, 720))
            .build()
            .apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            processImage(imageProxy)
        }

        cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture!!, imageAnalysis)
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (!isDetectionEnabled) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            // In your face detection success listener:
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    overlayView.clear()
                    val targetBoxRect = overlayView.getTargetBoxRect()
                    val facesInFrame = mutableListOf<Face>()
                    val facesInTargetBox = mutableListOf<Face>()
                    val faceArray = Arguments.createArray()
                    currentToast?.cancel() // Clear any existing toast first

                    // First pass: Categorize all detected faces
                    for (face in faces) {
                        val faceBox = FaceBox(overlayView, face, mediaImage.cropRect, isFrontCamera)
                        val faceRect = faceBox.getMappedRect()

                        if (!faceRect.isEmpty) {
                            overlayView.add(faceBox)
                            facesInFrame.add(face)
                            if (targetBoxRect.contains(faceRect)) {
                                facesInTargetBox.add(face)
                            }
                        }
                    }

                    overlayView.invalidate()

                    when {
                        // Case 1: Multiple faces in entire frame
                        facesInFrame.size > 1 -> {
                            showToast("Multiple faces detected in frame", R.color.my_red)
                            sendEmptyArrayToJS()
                        }
                        
                        // Case 2: Exactly one face in frame
                        facesInFrame.size == 1 -> {
                            val face = facesInFrame.first()
                            val faceRect = FaceBox(overlayView, face, mediaImage.cropRect, isFrontCamera)
                                .getMappedRect()
                            
                            when {
                                // Subcase 2a: Face is in target box
                                targetBoxRect.contains(faceRect) -> {
                                    val faceMap = createFaceMap(face, faceRect)
                                    faceArray.pushMap(faceMap)
                                    sendFaceDataToJS(faceArray)
                                    
                                    // Check for smile and show appropriate message
                                    val smilingProbability = face.smilingProbability
                                    if (smilingProbability != null && smilingProbability > 0.5f) {

                                        showToast("You are in a good mood! ��", R.color.my_green)
                                    } else if (smilingProbability != null && smilingProbability < 0.5f) {
                                        showToast("You are not in a good mood! ��", R.color.my_red)
                                    } else {
                                        showToast("Face detected in target box", R.color.my_green)
                                    }
                                }
                                // Subcase 2b: Face not in target box
                                else -> {
                                    showToast("Position your face inside the box", R.color.my_yellow)
                                    sendEmptyArrayToJS()
                                }
                            }
                        }
                        
                        // Case 3: Multiple faces in target box (but might be single in frame)
                        facesInTargetBox.size > 1 -> {
                            showToast("Multiple faces in target box", R.color.my_orange)
                            sendEmptyArrayToJS()
                        }
                        
                        // Case 4: No faces detected
                        else -> {
                            sendEmptyArrayToJS()
                        }
                    }
                }
                .addOnFailureListener { 
                    Log.e("FaceDetection", "Error: ${it.message}")
                    showToast("Face detection failed", R.color.my_red)
                    it.printStackTrace()
                }
                .addOnCompleteListener { 
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }


   // Helper function using proper RectF properties
    private fun createFaceMap(face: Face, faceRect: RectF): WritableMap {
        return Arguments.createMap().apply {
            putInt("x", faceRect.left.toInt())       // left property
            putInt("y", faceRect.top.toInt())        // top property
            putInt("width", faceRect.width().toInt()) // width() method
            putInt("height", faceRect.height().toInt()) // height() method
            putDouble("rollAngle", face.headEulerAngleZ.toDouble())
            putDouble("yawAngle", face.headEulerAngleY.toDouble())
            putDouble("pitchAngle", face.headEulerAngleX.toDouble())
        }
    }

    // -------------show toast----------------------
    private fun showToast(message: String, colorRes: Int) {
        val activity = (context as? ThemedReactContext)?.currentActivity ?: run {
            Log.e("ToastError", "No activity available")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastToastTime < 2000) { // throttle: 1 toast every 2s
            return
        }
        lastToastTime = now

        activity.runOnUiThread {
            try {
                currentToast?.cancel()

                // Inflate layout once
                val inflater = LayoutInflater.from(activity)
                val toastLayout = inflater.inflate(R.layout.custom_toast, null)
                val textView = toastLayout.findViewById<TextView>(R.id.toast_text)

                textView.text = message
                textView.setTextColor(Color.WHITE)

                // Resolve color resource → real color int
                val bgColor = ContextCompat.getColor(activity, colorRes)

                // Rounded background with dynamic color
                val bg = GradientDrawable().apply {
                    cornerRadius = 24f.toPx(activity)
                    setColor(bgColor)
                }
                toastLayout.background = bg
                toastLayout.elevation = 6f.toPx(activity)

                // Create toast
                currentToast = Toast(activity).apply {
                    duration = Toast.LENGTH_SHORT
                    setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
                    view = toastLayout
                    show()
                }
            } catch (e: Exception) {
                Log.e("ToastError", "Toast failed", e)
            }
        }
    }

    // Extension for DP → PX
    private fun Float.toPx(context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            context.resources.displayMetrics
        )
    }


// -------------send data to JS----------------------
    private fun sendFaceDataToJS(data: WritableArray) {
        reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            ?.emit("onSingleFaceInTarget", data)
    }


// -------------send empty array to JS----------------------
    private fun sendEmptyArrayToJS() {
        sendFaceDataToJS(Arguments.createArray())
    }


// -------------take picture----------------------
    fun capturePhoto() {
        val capture = imageCapture ?: return
        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(file)
                    val map = Arguments.createMap()
                    map.putString("uri", uri.toString())

                    reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        ?.emit("onPictureTaken", map)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraView", "Photo capture failed: ${exception.message}")
                    
                    reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        ?.emit("onPictureError", Arguments.createMap().apply {
                            putString("message", exception.message ?: "Unknown error")
                            putString("code", exception.imageCaptureError.toString())
                        })
                }
            }
        )
    }
}