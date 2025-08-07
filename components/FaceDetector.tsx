import React, { useState, useEffect, useCallback } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  NativeModules,
  requireNativeComponent,
  ViewStyle,
  NativeSyntheticEvent,
} from 'react-native';

const { FaceDetectorModule } = NativeModules;
const FaceDetectorView = requireNativeComponent<any>('FaceDetectorView');

interface FaceDetectorProps {
  style?: ViewStyle;
  onFacesDetected?: (event: NativeSyntheticEvent<{ faces: Face[] }>) => void;
}

interface Face {
  id: number;
  x: number;
  y: number;
  width: number;
  height: number;
  left?: number;
  top?: number;
  right?: number;
  bottom?: number;
  trackingId?: number;
}

const FaceDetector: React.FC<FaceDetectorProps> = ({
  style,
  onFacesDetected,
}) => {
  const [isFrontCamera, setIsFrontCamera] = useState(false);
  const [isDetecting, setIsDetecting] = useState(true);
  const [faces, setFaces] = useState<Face[]>([]);

  useEffect(() => {
    // Setup face detector with default options on mount
    FaceDetectorModule.setupFaceDetector({
      performanceMode: 'fast',
      landmarkMode: 'none',
      contourMode: 'none',
      classificationMode: 'none',
      minFaceSize: 0.15,
      enableTracking: true,
    });

    // Cleanup function that runs on component unmount
    return () => {
      // Stop camera and clear faces
      FaceDetectorModule.stopCamera()
      
      // Clear faces state
      setFaces([]);
      
      // Reset detection state
      setIsDetecting(false);
    };
  }, []);

  const handleFacesDetected = useCallback(
    (event: NativeSyntheticEvent<{ faces: Face[] }>) => {
      const detectedFaces = event.nativeEvent.faces;
      setFaces(detectedFaces);
      if (onFacesDetected) {
        onFacesDetected(event);
      }
    },
    [onFacesDetected]
  );

  const toggleCamera = useCallback(() => {
    const newValue = !isFrontCamera;
    setIsFrontCamera(newValue);
  },[])

  return (
    <View style={[styles.container, style]}>
      <FaceDetectorView
        style={styles.preview}
        cameraType={isFrontCamera ? 'front' : 'back'}
        isDetecting={isDetecting}
        onFacesDetected={handleFacesDetected}
      />

      <View style={styles.controls}>
        <TouchableOpacity style={styles.button} onPress={toggleCamera}>
          <Text style={styles.buttonText}>
            {isFrontCamera ? 'Switch to Back' : 'Switch to Front'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.button}
          onPress={() => setIsDetecting(!isDetecting)}
        >
          <Text style={styles.buttonText}>
            {isDetecting ? 'Stop' : 'Start'} Detection
          </Text>
        </TouchableOpacity>
      </View>

      <View style={styles.faceInfo}>
        <Text style={styles.faceText}>Faces detected: {faces.length}</Text>
        {faces.map((face, index) => (
          <View key={face.id || index} style={styles.faceDetails}>
            <Text style={styles.faceText}>Face {(face.id ?? index) + 1}:</Text>
            <Text style={styles.coordinateText}>X: {face.x}</Text>
            <Text style={styles.coordinateText}>Y: {face.y}</Text>
            <Text style={styles.coordinateText}>Width: {face.width}</Text>
            <Text style={styles.coordinateText}>Height: {face.height}</Text>
          </View>
        ))}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  preview: {
    flex: 1,
  },
  controls: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'space-around',
    padding: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  button: {
    padding: 10,
    backgroundColor: '#4285F4',
    borderRadius: 5,
  },
  buttonText: {
    color: 'white',
    fontWeight: 'bold',
  },
  faceInfo: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    padding: 10,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  faceText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  faceDetails: {
    marginTop: 8,
    paddingLeft: 10,
  },
  coordinateText: {
    color: 'white',
    fontSize: 14,
  },
});

export default FaceDetector;
