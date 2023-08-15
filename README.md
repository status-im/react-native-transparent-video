![](https://github.com/status-im/react-native-transparent-video/assets/18485527/92a5b88f-b152-404e-a4ff-5d7552842cd8)

# react-native-transparent-video

React Native video player with alpha channel (alpha-packing) support. It works on iOS and Android.

Example of a mp4 video with alpha-packing:

https://github.com/status-im/react-native-transparent-video/assets/18485527/69ea988e-0c7d-4123-84a1-1ca46b61994e

### Recommendations

To achieve best compatibility across different Android devices and versions, please check the [video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) from the Android documentation portal.

## Installation

```sh
npm install @status-im/react-native-transparent-video
```

or

```sh
yarn add @status-im/react-native-transparent-video
```

## Usage

```js
import TransparentVideo from '@status-im/react-native-transparent-video';

const video = require('../assets/video.mp4');

function App() {
  return (
    <View>
      <TransparentVideo source={video} style={styles.transparentVideo} />
    </View>
  );
}

const styles = StyleSheet.create({
  transparentVideo: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
});
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## Recognition

Special thanks to:
- Quentin Fasquel for this [Medium article](https://medium.com/@quentinfasquel/ios-transparent-video-with-coreimage-52cfb2544d54)
- Tristan Ferré for this [Medium article](https://medium.com/go-electra/unlock-transparency-in-videos-on-android-5dc43776cc72)
- [@pavelsemak](https://www.github.com/pavelsemak) for creating [this repository](https://github.com/pavelsemak/alpha-movie) and [@nopol10](https://www.github.com/nopol10) for [this fork](https://github.com/nopol10/alpha-movie) which implements the alpha-packing approach that was used to build this React Native library 

## License

MIT
