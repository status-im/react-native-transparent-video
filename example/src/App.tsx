import React from 'react';

import { StyleSheet, View } from 'react-native';
import Parallax from './components/Parallax';

const video1 = require('../assets/videos/1.mp4');
const video2 = require('../assets/videos/2.mp4');
const video3 = require('../assets/videos/3.mp4');
const video4 = require('../assets/videos/4.mp4');
const video5 = require('../assets/videos/5.mp4');
const video6 = require('../assets/videos/6.mp4');

export default () => {
  return (
    <View style={styles.container}>
      <Parallax layers={[video1, video2, video3, video4, video5, video6]} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: 'darkblue',
  },
  video: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
});
