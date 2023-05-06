import React, { useEffect, useState } from 'react';

import { StyleSheet, View } from 'react-native';
import Parallax from './components/Parallax';

const video1 = require('../assets/videos/1.mp4');
const video2 = require('../assets/videos/2.mp4');
const video3 = require('../assets/videos/3.mp4');
const video4 = require('../assets/videos/4.mp4');

export default () => {
  const [background, setBackground] = useState('blue');

  useEffect(() => {
    setTimeout(() => {
      if (background === 'blue') {
        setBackground('green');
      } else if (background === 'green') {
        setBackground('red');
      } else if (background === 'red') {
        setBackground('yellow');
      } else if (background === 'yellow') {
        setBackground('darkblue');
      } else if (background === 'darkblue') {
        setBackground('gray');
      } else if (background === 'gray') {
        setBackground('blue');
      }
    }, 1000);
  }, [background]);

  return (
    <View style={{ ...styles.container, ...{ backgroundColor: background } }}>
      <Parallax layers={[video1, video2, video3, video4]} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
  },
  video: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
});
