package com.transparentvideo;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.bridge.ReadableMap;

import java.util.Dictionary;

public class TransparentVideoViewManager extends SimpleViewManager<View> {
  public static final String REACT_CLASS = "TransparentVideoView";

  @Override
  @NonNull
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  @NonNull
  public View createViewInstance(ThemedReactContext reactContext) {
    return new View(reactContext);
  }

  @ReactProp(name = "src")
  public void setSrc(View view, ReadableMap src) {
    view.setBackgroundColor(Color.BLUE);
  }
}
