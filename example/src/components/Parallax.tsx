import React from 'react';

import ParallaxVideo from './ParallaxVideo';

const Parallax = ({ layers }: any) => {
  return (
    <>
      {layers.map((layer: any, index: number) => (
        <ParallaxVideo key={'layer' + index} source={layer} zIndex={++index} />
      ))}
    </>
  );
};

export default Parallax;
