#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(TransparentVideoViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(src, NSDictionary);
RCT_EXPORT_VIEW_PROPERTY(loopFrom, NSInteger);
RCT_EXPORT_VIEW_PROPERTY(autoplay, BOOL);

@end
