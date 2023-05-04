import AVFoundation
import os.log

@objc(TransparentVideoViewManager)
class TransparentVideoViewManager: RCTViewManager {

  override func view() -> (TransparentVideoView) {
    return TransparentVideoView()
  }

  @objc override static func requiresMainQueueSetup() -> Bool {
    return false
  }
}

class TransparentVideoView : UIView {

  private var source: VideoSource?
  private var playerView: AVPlayerView?

  @objc var src: NSDictionary = NSDictionary() {
    didSet {
      self.source = VideoSource(src)
      let itemUrl = URL(string: self.source!.uri!)!
      loadVideoPlayer(itemUrl: itemUrl)
    }
  }
  
  func loadVideoPlayer(itemUrl: URL) {
    if (self.playerView == nil) {
      let playerView = AVPlayerView(frame: CGRect(origin: .zero, size: .zero))
      addSubview(playerView)
     
      // Use Auto Layout anchors to center our playerView
      playerView.translatesAutoresizingMaskIntoConstraints = false
      NSLayoutConstraint.activate([
        playerView.topAnchor.constraint(equalTo: self.topAnchor),
        playerView.bottomAnchor.constraint(equalTo: self.bottomAnchor),
        playerView.leadingAnchor.constraint(equalTo: self.leadingAnchor),
        playerView.trailingAnchor.constraint(equalTo: self.trailingAnchor)
      ])
      
      // Setup our playerLayer to hold a pixel buffer format with "alpha"
      let playerLayer: AVPlayerLayer = playerView.playerLayer
      playerLayer.pixelBufferAttributes = [
          (kCVPixelBufferPixelFormatTypeKey as String): kCVPixelFormatType_32BGRA]

      // Setup looping on our video
      playerView.isLoopingEnabled = true
      
      NotificationCenter.default.addObserver(self, selector: #selector(appEnteredBackgound), name: UIApplication.didEnterBackgroundNotification, object: nil)
      NotificationCenter.default.addObserver(self, selector: #selector(appEnteredForeground), name: UIApplication.willEnterForegroundNotification, object: nil)

      self.playerView = playerView
    }
    
    // Load our player item
    let playerItem = createTransparentItem(url: itemUrl)
    
    self.playerView!.loadPlayerItem(playerItem) { result in
      switch result {
      case .failure(let error):
        return print("Something went wrong when loading our video", error)

      case .success(let player):
        // Finally, we can start playing
        player.play()
      }
    }
  }
  
  // MARK: - Player Item Configuration
  
  func createTransparentItem(url: URL) -> AVPlayerItem {
    let asset = AVAsset(url: url)
    let playerItem = AVPlayerItem(asset: asset)
    // Set the video so that seeking also renders with transparency
    playerItem.seekingWaitsForVideoCompositionRendering = true
    // Apply a video composition (which applies our custom filter)
    playerItem.videoComposition = createVideoComposition(for: asset)
    return playerItem
  }
  
  func createVideoComposition(for asset: AVAsset) -> AVVideoComposition {
    let filter = AlphaFrameFilter(renderingMode: .builtInFilter)
    let composition = AVMutableVideoComposition(asset: asset, applyingCIFiltersWithHandler: { request in
      do {
        let (inputImage, maskImage) = request.sourceImage.verticalSplit()
        let outputImage = try filter.process(inputImage, mask: maskImage)
        return request.finish(with: outputImage, context: nil)
      } catch {
        os_log("Video composition error: %s", String(describing: error))
        return request.finish(with: error)
      }
    })

    composition.renderSize = asset.videoSize.applying(CGAffineTransform(scaleX: 1.0, y: 0.5))
    return composition
  }
  
  // MARK: - Lifecycle callbacks
  
  @objc func appEnteredBackgound() {
    if let tracks = self.playerView?.player?.currentItem?.tracks {
      for track in tracks {
        if (track.assetTrack?.hasMediaCharacteristic(AVMediaCharacteristic.visual))! {
          track.isEnabled = false
        }
      }
    }
  }

  @objc func appEnteredForeground() {
    if let tracks = self.playerView?.player?.currentItem?.tracks {
      for track in tracks {
        if (track.assetTrack?.hasMediaCharacteristic(AVMediaCharacteristic.visual))! {
          track.isEnabled = true
        }
      }
    }
  }
}
