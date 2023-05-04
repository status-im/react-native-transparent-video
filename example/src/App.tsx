import React, { useEffect, useState } from 'react';

import { StyleSheet, View } from 'react-native';
import TransparentVideo from 'react-native-transparent-video';

const video1 = require('../assets/videos/background.mp4');
const video2 = require('../assets/videos/playdoh-bat.mp4');

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
      <TransparentVideo source={video1} style={styles.video1} />
      <TransparentVideo source={video2} style={styles.video2} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
  },
  video1: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 0,
  },
  video2: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 1,
  },
});
