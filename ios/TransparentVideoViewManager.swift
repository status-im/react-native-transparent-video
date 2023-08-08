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
  private var videoAutoplay: Bool?

  @objc var src: NSDictionary = NSDictionary() {
    didSet {
      self.source = VideoSource(src)
    }
  }

  @objc var autoplay: Bool = false {
    didSet {
      self.videoAutoplay = autoplay
      let itemUrl = URL(string: self.source!.uri!)!
      loadVideoPlayer(itemUrl: itemUrl)
    }
  }

  @objc var loop: Bool = Bool() {
    didSet {
      // Setup looping on our video
      self.playerView?.isLoopingEnabled = loop
      let player = self.playerView?.player
      if (loop && (player?.rate == 0 || player?.error != nil)) {
        player?.play()
      }
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
      NotificationCenter.default.addObserver(self, selector: #selector(appEnteredBackgound), name: UIApplication.didEnterBackgroundNotification, object: nil)
      NotificationCenter.default.addObserver(self, selector: #selector(appEnteredForeground), name: UIApplication.willEnterForegroundNotification, object: nil)

      self.playerView = playerView
    }

    // Load our player item
    loadItem(url: itemUrl)
  }

  deinit {
    playerView?.player?.pause()
    playerView?.player?.replaceCurrentItem(with: nil)
    playerView?.removeFromSuperview()
    playerView = nil
  }

  // MARK: - Player Item Configuration

  private func loadItem(url: URL) {
    setUpAsset(with: url) { [weak self] (asset: AVAsset) in
      self?.setUpPlayerItem(with: asset)
    }
  }

  private func setUpAsset(with url: URL, completion: ((_ asset: AVAsset) -> Void)?) {
    let asset = AVAsset(url: url)
    asset.loadValuesAsynchronously(forKeys: ["metadata"]) {
      var error: NSError? = nil
      let status = asset.statusOfValue(forKey: "metadata", error: &error)
      switch status {
      case .loaded:
        completion?(asset)
      case .failed:
        print(".failed")
      case .cancelled:
        print(".cancelled")
      default:
              print("default")
          }
      }
  }

  private func setUpPlayerItem(with asset: AVAsset) {
    DispatchQueue.main.async { [weak self] in
      let playerItem = AVPlayerItem(asset: asset)
      playerItem.seekingWaitsForVideoCompositionRendering = true
      // Apply a video composition (which applies our custom filter)
      playerItem.videoComposition = self?.createVideoComposition(for: asset)

      self?.playerView!.loadPlayerItem(playerItem) { result in
        switch result {
        case .failure(let error):
          return print("Something went wrong when loading our video", error)

        case .success(let player) where self?.videoAutoplay == true:
          // Finally, we can start playing
          player.play()

        case .success(let player):
          // Finally, we can start playing
          player.pause()
        }
      }
    }
  }

  override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
    if keyPath == #keyPath(AVPlayerItem.status) {
      let status: AVPlayerItem.Status
      if let statusNumber = change?[.newKey] as? NSNumber {
        status = AVPlayerItem.Status(rawValue: statusNumber.intValue)!
      } else {
        status = .unknown
      }
      // Switch over status value
      switch status {
      case .readyToPlay:
        print(".readyToPlay")
      case .failed:
        print(".failed")
      case .unknown:
        print(".unknown")
      @unknown default:
        print("@unknown default")
          }
      }
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
