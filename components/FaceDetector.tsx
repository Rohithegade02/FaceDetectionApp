import React, {useEffect, useRef, useState} from 'react';
import {
  View,
  PermissionsAndroid,
  Platform,
  Alert,
  NativeModules,
  NativeEventEmitter,
  requireNativeComponent,
  StyleSheet,
  UIManager,
  findNodeHandle,
  Button,
  Image,
} from 'react-native';

const {FaceDetectionModule} = NativeModules;
const NativeCameraView = requireNativeComponent('CameraView');

const FaceDetector = ({
  faces,
  setFaces,
  cameraViewStyle,
  setDetectionEnabled,
  cameraType = 'front',
  detectionEnabled = false,
  setCameraResolution,
}: {
  faces: any;
  setFaces: any;
  cameraViewStyle: any;
  setDetectionEnabled: any;
  cameraType: string;
  detectionEnabled: boolean;
  setCameraResolution: any;
}) => {
  
  const [hasPermission, setHasPermission] = useState(false);
  const [uriValue, setUriValue] = useState('');
  const cameraRef = useRef(null);

  // Request permission
  const requestCameraPermission = async () => {
    try {
      if (Platform.OS === 'android') {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.CAMERA,
        );
        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          setHasPermission(true);
        } else {
          Alert.alert('Camera permission denied');
        }
      } else {
        console.warn('Camera permission error:');
      }
    } catch (e) {
      console.warn('Camera permission error:', e);
    }
  };

  useEffect(() => {
    requestCameraPermission();
  }, []);

  useEffect(() => {
    if (hasPermission) {
      setDetectionEnabled?.(true);
    }
  }, [hasPermission, setDetectionEnabled]);

  useEffect(() => {
    const emitter = new NativeEventEmitter(FaceDetectionModule);

    const faceSub = emitter.addListener('onSingleFaceInTarget', foundFaces => {
      setFaces?.(foundFaces);
    });

    const photoSub = emitter.addListener('onPictureTaken', data => {
      setUriValue(data.uri);
      return Alert.alert('Photo Captured', data.uri);
    });

    const photoErrorSub = emitter.addListener('onPictureError', data => {
      return Alert.alert('Photo Error', data);
    });

    return () => {
      faceSub.remove();
      photoSub.remove();
      photoErrorSub.remove();
    };
  }, [setFaces]);

  const takePicture = () => {
    if (faces.length === 0) {
      return Alert.alert('Face Error', 'No face found.');
    }
    if (faces.length > 1) {
      return Alert.alert('Face Error', 'More than 1 face in frame.');
    }
    const viewId = findNodeHandle(cameraRef.current);
    if (viewId) {
      UIManager.dispatchViewManagerCommand(
        viewId,
        UIManager.getViewManagerConfig('CameraView').Commands.takePicture,
        [],
      );
    }
  };

  return (
    <View style={cameraViewStyle}>
      {hasPermission ? (
        <>
          <NativeCameraView
            ref={cameraRef}
            style={StyleSheet.absoluteFill}
            cameraType={cameraType}
            detectionEnabled={detectionEnabled}
          />
          <View style={styles.captureButtonView}>
            <Button title="Take Picture" onPress={takePicture} />
          </View>
          {uriValue && (
            <View style={styles.previewContainer}>
              <Image
                source={{uri: uriValue}}
                style={styles.previewImage}
                resizeMode="cover"
              />
            </View>
          )}
        </>
      ) : (
        <View style={styles.permissionView}>
          <Button
            title="Grant Camera Permission"
            onPress={requestCameraPermission}
          />
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  captureButtonView: {
    position: 'absolute',
    bottom: 15,
    alignSelf: 'center',
    backgroundColor: '#ffffffcc',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 8,
    elevation: 5,
  },
  previewContainer: {
    position: 'absolute',
    top: 10,
    right: 10,
    borderWidth: 1,
    borderColor: '#ccc',
    backgroundColor: '#fff',
    borderRadius: 8,
    overflow: 'hidden',
  },
  previewImage: {
    width: 100,
    height: 120,
  },
  permissionView: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});

  export default FaceDetector;
