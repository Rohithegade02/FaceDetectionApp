import type { ViewProps } from 'ViewPropTypes';
import type { HostComponent } from 'react-native';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';

export interface NativeProps extends ViewProps {
  cameraType?: string;
  isDetecting?: boolean;
}

export default codegenNativeComponent<NativeProps>(
  'RTNMyImageView',
) as HostComponent<NativeProps>;
