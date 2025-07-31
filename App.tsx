/**
 * Face Detection App
 * Using ML Kit for Android and Vision Framework for iOS
 *
 * @format
 */

import React, { useState, useEffect } from 'react';
import {
  StatusBar,
  StyleSheet,
  View,
  Platform,
  PermissionsAndroid,
  Text,
} from 'react-native';
import FaceDetector from './components/FaceDetector';
import RTNMyImageViewNativeComponent from './RTNMyImageView/js/RTNMyImageViewNativeComponent';

function App() {
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);

  useEffect(() => {
    async function requestCameraPermission() {
      if (Platform.OS === 'android') {
        try {
          const granted = await PermissionsAndroid.requestMultiple([
            PermissionsAndroid.PERMISSIONS.CAMERA,
            PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
          ]);

          const cameraGranted =
            granted[PermissionsAndroid.PERMISSIONS.CAMERA] ===
            PermissionsAndroid.RESULTS.GRANTED;
          const audioGranted =
            granted[PermissionsAndroid.PERMISSIONS.RECORD_AUDIO] ===
            PermissionsAndroid.RESULTS.GRANTED;

          setHasPermission(cameraGranted && audioGranted);
        } catch (err) {
          console.warn(err);
          setHasPermission(false);
        }
      } else {
        // For iOS, permissions are handled in the native module
        setHasPermission(true);
      }
    }

    requestCameraPermission();
  }, []);

  if (hasPermission === null) {
    return <View style={styles.container} />;
  }

  if (hasPermission === false) {
    return (
      <View style={styles.container}>
        <StatusBar barStyle="dark-content" />
        <View style={styles.permissionContainer}>
          <Text style={styles.permissionText}>
            Camera permission is required to use this app
          </Text>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000000" />
      {Platform.OS === 'ios' ? (
        <RTNMyImageViewNativeComponent />
      ) : (
        <FaceDetector
          style={styles.faceDetector}
          onFacesDetected={event => {
            console.log(`Faces detected: ${event.nativeEvent.faces.length}`);
          }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  permissionContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  permissionText: {
    fontSize: 18,
    textAlign: 'center',
    color: '#fff',
  },
  faceDetector: {
    flex: 1,
  },
});

export default App;
