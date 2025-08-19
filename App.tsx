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
  ActivityIndicator,
  } from 'react-native';
import FaceDetector from './components/FaceDetector';

function App() {
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(false);
  const [faceFound, setFaceFound] = useState([]);
  const [inProcess, setInProcess] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [attendanceDone, setAttendanceDone] = useState(false);
  const [cameraResolution, setCameraResolution] = useState({});
  const [faceInBoxNormal, setFaceInBoxNormal] = useState(false);
  const [detectionEnabled, setDetectionEnabled] = useState(false);

  const cameraType = 'front';


  // initial rendering
  useEffect(() => {
    // (async () => {
    // })();

    const intervalId = setInterval(updateTime, 1000);

    // Clear the interval when the component is unmounted
    return () => {
      clearInterval(intervalId);
      setInProcess(false);
      setAttendanceDone(false);
      setFaceInBoxNormal(false);
    };
  }, []);

  // no face
  useEffect(() => {
    if (faceFound.length === 0) {
      setInProcess(false);
      setAttendanceDone(false);
      setFaceInBoxNormal(false);
    }
  }, [faceFound]);



  // --------------------fetch current position----------------------


  const updateTime = () => {
    setCurrentTime(new Date());
  };

  


  // if loading
  if (loading) {
    return <ActivityIndicator size="large" color="#0000ff" />;
  }


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
      
        <FaceDetector
         cameraType={cameraType}
         faces={faceFound}
         setFaces={setFaceFound}
         detectionEnabled={detectionEnabled}
         setDetectionEnabled={setDetectionEnabled}
         setCameraResolution={setCameraResolution}
         cameraViewStyle={styles.cameraView}
      />
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
  cameraView: {
    elevation: 5,
    width: '100%',
    height: '100%',
    borderWidth: 1,
    borderRadius: 22,
    shadowRadius: 3.84,
    shadowOpacity: 0.25,
    shadowColor: 'black',
    backgroundColor: 'transparent',
    shadowOffset: {
      width: 0,
      height: 2,
    },
  },
});

export default App;
