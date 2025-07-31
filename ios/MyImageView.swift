import UIKit
import AVFoundation
import Vision

@objc(MyImageView)
class MyImageView: UIView {
    
    private var captureSession: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var videoOutput: AVCaptureVideoDataOutput?
    private var currentCamera: AVCaptureDevice.Position = .back
    private var isDetecting = true
    
    // Face detection properties
    private var faceOverlayLayer = CALayer()
    private let outputQueue = DispatchQueue(label: "VideoOutputQueue")
    
    // Event callback
    var onFacesDetected: (([String: Any]) -> Void)?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupCamera()
        setupFaceOverlay()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupCamera()
        setupFaceOverlay()
    }
    
    private func setupFaceOverlay() {
        faceOverlayLayer.bounds = bounds
        faceOverlayLayer.position = CGPoint(x: bounds.midX, y: bounds.midY)
        layer.addSublayer(faceOverlayLayer)
    }
    
    private func setupCamera() {
        captureSession = AVCaptureSession()
        captureSession?.sessionPreset = .high
        
        guard let captureSession = captureSession else { return }
        
        // Get camera device
        guard let camera = getCamera(for: currentCamera) else {
            print("Failed to get camera")
            return
        }
        
        do {
            // Camera input
            let cameraInput = try AVCaptureDeviceInput(device: camera)
            if captureSession.canAddInput(cameraInput) {
                captureSession.addInput(cameraInput)
            }
            
            // Video output for face detection
            videoOutput = AVCaptureVideoDataOutput()
            videoOutput?.setSampleBufferDelegate(self, queue: outputQueue)
            videoOutput?.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
            
            if captureSession.canAddOutput(videoOutput!) {
                captureSession.addOutput(videoOutput!)
            }
            
            // Preview layer
            previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
            previewLayer?.videoGravity = .resizeAspectFill
            previewLayer?.frame = bounds
            
            if let previewLayer = previewLayer {
                layer.insertSublayer(previewLayer, at: 0)
            }
            
            // Start session
            DispatchQueue.global(qos: .background).async {
                captureSession.startRunning()
            }
            
        } catch {
            print("Error setting up camera: \(error)")
        }
    }
    
    private func getCamera(for position: AVCaptureDevice.Position) -> AVCaptureDevice? {
        let deviceTypes: [AVCaptureDevice.DeviceType] = [
            .builtInWideAngleCamera,
            .builtInTelephotoCamera,
            .builtInDualCamera,
            .builtInTrueDepthCamera
        ]
        
        let discoverySession = AVCaptureDeviceDiscoverySession(
            deviceTypes: deviceTypes,
            mediaType: .video,
            position: position
        )
        
        return discoverySession.devices.first
    }
    
    @objc func setCameraType(_ cameraType: String) {
        let newPosition: AVCaptureDevice.Position = cameraType == "front" ? .front : .back
        
        if newPosition != currentCamera {
            currentCamera = newPosition
            updateCamera()
        }
    }
    
    @objc func setIsDetecting(_ detecting: Bool) {
        isDetecting = detecting
    }
    
    private func updateCamera() {
        guard let captureSession = captureSession else { return }
        
        captureSession.beginConfiguration()
        
        // Remove existing inputs
        captureSession.inputs.forEach { input in
            captureSession.removeInput(input)
        }
        
        // Add new camera input
        guard let camera = getCamera(for: currentCamera) else {
            captureSession.commitConfiguration()
            return
        }
        
        do {
            let cameraInput = try AVCaptureDeviceInput(device: camera)
            if captureSession.canAddInput(cameraInput) {
                captureSession.addInput(cameraInput)
            }
        } catch {
            print("Error updating camera: \(error)")
        }
        
        captureSession.commitConfiguration()
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
        faceOverlayLayer.bounds = bounds
        faceOverlayLayer.position = CGPoint(x: bounds.midX, y: bounds.midY)
    }
    
    private func drawFaceRectangles(_ faces: [VNFaceObservation]) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Clear previous face rectangles
            self.faceOverlayLayer.sublayers?.removeAll()
            
            for face in faces {
                let boundingBox = self.convertRect(face.boundingBox, from: self.previewLayer!)
                
                let faceRectangle = CALayer()
                faceRectangle.frame = boundingBox
                faceRectangle.borderColor = UIColor.red.cgColor
                faceRectangle.borderWidth = 2.0
                faceRectangle.backgroundColor = UIColor.clear.cgColor
                
                self.faceOverlayLayer.addSublayer(faceRectangle)
            }
        }
    }
    
    private func convertRect(_ rect: CGRect, from layer: AVCaptureVideoPreviewLayer) -> CGRect {
        let layerRect = layer.layerRectConverted(fromMetadataOutputRect: rect)
        return layerRect
    }
    
    private func sendFacesDetectedEvent(_ faces: [VNFaceObservation]) {
        let faceData = faces.enumerated().map { (index, face) in
            let boundingBox = convertRect(face.boundingBox, from: previewLayer!)
            return [
                "id": index,
                "x": Int(boundingBox.origin.x),
                "y": Int(boundingBox.origin.y),
                "width": Int(boundingBox.size.width),
                "height": Int(boundingBox.size.height),
                "confidence": face.confidence
            ]
        }
        
        onFacesDetected?(["faces": faceData])
    }
}

// MARK: - AVCaptureVideoDataOutputSampleBufferDelegate
extension MyImageView: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard isDetecting else { return }
        
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        
        let imageRequestHandler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:])
        
        let faceDetectionRequest = VNDetectFaceRectanglesRequest { [weak self] request, error in
            guard let self = self else { return }
            
            if let error = error {
                print("Face detection error: \(error)")
                return
            }
            
            guard let results = request.results as? [VNFaceObservation] else { return }
            
            self.drawFaceRectangles(results)
            self.sendFacesDetectedEvent(results)
        }
        
        do {
            try imageRequestHandler.perform([faceDetectionRequest])
        } catch {
            print("Failed to perform face detection: \(error)")
        }
    }
}