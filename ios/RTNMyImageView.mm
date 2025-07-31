#import "RTNMyImageView.h"

#import <react/renderer/components/RTNMyImageViewSpecs/ComponentDescriptors.h>
#import <react/renderer/components/RTNMyImageViewSpecs/EventEmitters.h>
#import <react/renderer/components/RTNMyImageViewSpecs/Props.h>
#import <react/renderer/components/RTNMyImageViewSpecs/RCTComponentViewHelpers.h>
#import "FaceDetectionApp-Swift.h"

using namespace facebook::react;

@interface RTNMyImageView () <RCTRTNMyImageViewViewProtocol>
@end

@implementation RTNMyImageView {
  MyImageView *_myImageView;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<RTNMyImageViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const RTNMyImageViewProps>();
    _props = defaultProps;

    _myImageView = [[MyImageView alloc] initWithFrame:frame];
    _myImageView.backgroundColor = [UIColor blackColor];
    
    // Set up face detection callback
    __weak typeof(self) weakSelf = self;
    _myImageView.onFacesDetected = ^(NSDictionary *eventData) {
      if (weakSelf && weakSelf->_eventEmitter) {
        auto emitter = std::static_pointer_cast<const RTNMyImageViewEventEmitter>(weakSelf->_eventEmitter);
        RTNMyImageViewEventEmitterOnFacesDetected event = {
          .faces = facebook::react::convertIdToFollyDynamic(eventData[@"faces"])
        };
        emitter->onFacesDetected(event);
      }
    };

    self.contentView = _myImageView;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<RTNMyImageViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<RTNMyImageViewProps const>(props);

  if (oldViewProps.cameraType != newViewProps.cameraType) {
    NSString *cameraType = [[NSString alloc] initWithCString:newViewProps.cameraType.c_str() encoding:NSUTF8StringEncoding];
    [_myImageView setCameraType:cameraType];
  }
  
  if (oldViewProps.isDetecting != newViewProps.isDetecting) {
    [_myImageView setIsDetecting:newViewProps.isDetecting];
  }

  [super updateProps:props oldProps:oldProps];
}

@end

Class<RCTComponentViewProtocol> RTNMyImageViewCls(void)
{
  return RTNMyImageView.class;
}