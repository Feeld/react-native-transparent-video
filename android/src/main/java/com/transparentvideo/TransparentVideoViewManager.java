package com.transparentvideo;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TransparentVideoViewManager extends SimpleViewManager<LinearLayout> {

  private static List<LinearLayout> sInstances = new ArrayList<>();
  private static Map<LinearLayout, Boolean> sLoopSettings = new HashMap<>();

  public static final String REACT_CLASS = "TransparentVideoView";
  private static final String TAG = "TransparentVideoViewManager";

  ReactApplicationContext reactContext;

  public TransparentVideoViewManager(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  @NonNull
  public LinearLayout createViewInstance(ThemedReactContext reactContext) {
    LinearLayout view = new LinearLayout(this.reactContext);
    sInstances.add(view);
    return view;
  }

  public static void destroyView(LinearLayout view) {
    sInstances.remove(view);
    sLoopSettings.remove(view);
  }

  @Override
  @Nullable
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.<String, Object>builder()
      .put("onLoad", MapBuilder.of("registrationName", "onLoad"))
      .build();
  }

  @ReactProp(name = "src")
  public void setSrc(LinearLayout view, ReadableMap src) {
    AlphaMovieView alphaMovieView = (AlphaMovieView)view.getChildAt(0);
    
    if (alphaMovieView == null) {
      alphaMovieView = new AlphaMovieView(reactContext, null);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
      lp.gravity = Gravity.CENTER;
      alphaMovieView.setLayoutParams(lp);
      alphaMovieView.setAutoPlayAfterResume(true);
      view.addView(alphaMovieView);
      
      // Apply saved loop setting
      Boolean loopSetting = sLoopSettings.get(view);
      alphaMovieView.setLooping(loopSetting != null ? loopSetting : true);
      
      // Send onLoad event when video starts
      final int viewId = view.getId();
      alphaMovieView.setOnVideoStartedListener(new AlphaMovieView.OnVideoStartedListener() {
        @Override
        public void onVideoStarted() {
          WritableMap event = Arguments.createMap();
          reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(viewId, "onLoad", event);
        }
      });
    }
    alphaMovieView.setPacked(true);
    String file = src.getString("uri").toLowerCase();
    Log.d(TAG + " setSrc", "file: " + file);

    try {
      Integer rawResourceId = Utils.getRawResourceId(reactContext, file);
      Log.d(TAG + " setSrc", "ResourceID: " + rawResourceId);

      alphaMovieView.setVideoFromResourceId(reactContext, rawResourceId);
    } catch (RuntimeException e) {
      Log.e(TAG + " setSrc", e.getMessage(), e);
      alphaMovieView.setVideoByUrl(file);
    }
  }

  @ReactProp(name = "loop", defaultBoolean = true)
  public void setLoop(LinearLayout view, boolean loop) {
    sLoopSettings.put(view, loop);
    
    AlphaMovieView alphaMovieView = (AlphaMovieView)view.getChildAt(0);
    if (alphaMovieView != null) {
      alphaMovieView.setLooping(loop);
    }
  }
}
