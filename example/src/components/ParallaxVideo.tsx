import React from 'react';

import { StyleSheet, Platform, Dimensions } from 'react-native';
import TransparentVideo from 'react-native-transparent-video';
import Animated, {
  useAnimatedSensor,
  SensorType,
  useAnimatedStyle,
  interpolate,
  withTiming,
} from 'react-native-reanimated';

const OFFSET = 50;
const PI = Math.PI;

const ParallaxVideo = ({ source, zIndex }: any) => {
  const sensor = useAnimatedSensor(SensorType.ACCELEROMETER, { interval: 10 });

  const layerStyle = useAnimatedStyle(() => {
    let { x, y, z } = sensor.sensor.value;
    const pitch = Math.abs((Math.atan2(y, z) * 180) / PI) * -1;
    const roll = (Math.atan2(-x, Math.sqrt(y * y + z * z)) * 180) / PI;

    const top = withTiming(
      interpolate(pitch, Platform.OS === 'ios' ? [-180, 0] : [0, -180], [
        -OFFSET / zIndex - OFFSET,
        OFFSET / zIndex - OFFSET,
      ]),
      { duration: 100 }
    );
    const left = withTiming(
      interpolate(roll, Platform.OS === 'ios' ? [90, -90] : [-90, 90], [
        (-OFFSET * 2) / zIndex - OFFSET,
        (OFFSET * 2) / zIndex - OFFSET,
      ]),
      { duration: 100 }
    );

    return {
      top,
      left,
    };
  });

  return (
    <Animated.View style={[styles.container, layerStyle]}>
      <TransparentVideo source={source} style={styles.video} />
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    width: Dimensions.get('window').width + OFFSET * 2,
    height: Dimensions.get('window').height + OFFSET * 2,
  },
  video: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
});

export default ParallaxVideo;
