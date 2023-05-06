import React from 'react';

import { StyleSheet } from 'react-native';
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
const HALF_PI = PI / 2;

const ParallaxVideo = ({ source, zIndex }: any) => {
  const sensor = useAnimatedSensor(SensorType.ROTATION, { interval: 10 });

  const layerStyle = useAnimatedStyle(() => {
    const { yaw, pitch, roll } = sensor.sensor.value;
    console.log(yaw, pitch, roll, 99);

    const top = withTiming(
      interpolate(
        pitch,
        [-HALF_PI, HALF_PI],
        [-OFFSET / zIndex - OFFSET, OFFSET / zIndex - OFFSET]
      ),
      { duration: 100 }
    );
    const left = withTiming(
      interpolate(
        roll,
        [-PI, PI],
        [(-OFFSET * 2) / zIndex - OFFSET, (OFFSET * 2) / zIndex - OFFSET]
      ),
      { duration: 100 }
    );
    const right = withTiming(
      interpolate(
        roll,
        [-PI, PI],
        [(OFFSET * 2) / zIndex - OFFSET, (-OFFSET * 2) / zIndex - OFFSET]
      ),
      { duration: 100 }
    );
    const bottom = withTiming(
      interpolate(
        pitch,
        [-HALF_PI, HALF_PI],
        [OFFSET / zIndex - OFFSET, -OFFSET / zIndex - OFFSET]
      ),
      { duration: 10 }
    );
    return {
      top,
      left,
      right,
      bottom,
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
