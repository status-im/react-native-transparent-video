#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(TransparentVideoViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(src, NSDictionary);
RCT_EXPORT_VIEW_PROPERTY(disableLoop, BOOL);

@end
