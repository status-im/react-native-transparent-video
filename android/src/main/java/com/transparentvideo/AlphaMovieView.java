/*
 * Copyright 2017 Pavel Semak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.transparentvideo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashMap;

@SuppressLint("ViewConstructor")
public class AlphaMovieView extends GLTextureView {

    private static final int GL_CONTEXT_VERSION = 2;

    private static final int NOT_DEFINED = -1;
    private static final int NOT_DEFINED_COLOR = 0;
    private static final int TIME_DETECTION_INTERVAL_MS = 100;

    private static final String TAG = "VideoSurfaceView";

    private static final float VIEW_ASPECT_RATIO = 4f / 3f;
    private float videoAspectRatio = VIEW_ASPECT_RATIO;

    VideoRenderer renderer;
    private MediaPlayer mediaPlayer;

    private OnVideoStartedListener onVideoStartedListener;
    private OnVideoEndedListener onVideoEndedListener;

    private boolean isSurfaceCreated;
    private boolean isDataSourceSet;

    private float accuracy;
    private int alphaColor;
    private boolean isPacked;
    // When loopStartMs >= 0 and loopEndMs == -1, the video will jump back to loopStartMs
    // once it reaches the end of the video.
    private long loopStartMs; // -1 means no specific loop points will be set
    private long loopEndMs; //numeros largos
    // This should be populated with a MediaPlayer.SEEK_* constant
    // Only for API 26 and above
    private int loopSeekingMethod = 0; //numeros
    private String shader; //letras y numeros

    private boolean autoPlayAfterResume;//si o no
    private boolean playAfterResume;

    private PlayerState state = PlayerState.NOT_PREPARED;

    final Handler handler = new Handler();
    final Runnable timeDetector = new Runnable() {
        public void run() {
            if (getRootView() == null) {
                return;
            }
            try {
                int currentTimeMs = mediaPlayer.getCurrentPosition();
                if (state == PlayerState.STARTED) {
                    startTimeDetector();
                } else {
                    return;
                }
                if (loopStartMs >= 0 && loopEndMs >= 0 && currentTimeMs >= loopEndMs) {
                    // Handle looping when both loop start and end points are defined
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mediaPlayer.seekTo(loopStartMs, loopSeekingMethod);
                    } else {
                        mediaPlayer.seekTo((int) loopStartMs);
                    }
                }
            } catch (Exception exception) {
                Log.e("AlphaMovieView", "Time detector error. Did you forget to call AlphaMovieView's onPause in the containing fragment/activity? | " + exception.getMessage());
            }
        }
    };

    public AlphaMovieView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            init(attrs);
        }
    }

    private void init(AttributeSet attrs) {
        setEGLContextClientVersion(GL_CONTEXT_VERSION);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        initMediaPlayer();

        renderer = new VideoRenderer();

        obtainRendererOptions(attrs);

        this.addOnSurfacePrepareListener();
        setRenderer(renderer);

        bringToFront();
        setPreserveEGLContextOnPause(true);
        setOpaque(false);
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        setScreenOnWhilePlaying(true);
        setLooping(true);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (onVideoEndedListener != null) {
                    onVideoEndedListener.onVideoEnded();
                }
                if (loopStartMs >= 0 && loopEndMs == -1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mediaPlayer.seekTo(loopStartMs, loopSeekingMethod);
                    } else {
                        mediaPlayer.seekTo((int) loopStartMs);
                    }
                    mediaPlayer.start();
                    return;
                }
                state = PlayerState.PAUSED;
            }
        });
    }

    private void obtainRendererOptions(AttributeSet attrs) {
        //if (attrs != null) {
            TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.AlphaMovieView);
            this.accuracy = arr.getFloat(R.styleable.AlphaMovieView_accuracy, 0.95f);
            this.alphaColor = arr.getColor(R.styleable.AlphaMovieView_alphaColor, Color.argb(1,0,255,0));
            this.autoPlayAfterResume = arr.getBoolean(R.styleable.AlphaMovieView_autoPlayAfterResume, false);
            this.isPacked = arr.getBoolean(R.styleable.AlphaMovieView_packed, false);
            this.loopStartMs = arr.getInteger(R.styleable.AlphaMovieView_loopStartMs, -1);
            this.loopEndMs = arr.getInteger(R.styleable.AlphaMovieView_loopEndMs, -1);
            updateMediaPlayerLoopSetting();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.loopSeekingMethod = arr.getInteger(R.styleable.AlphaMovieView_loopSeekingMethod, MediaPlayer.SEEK_CLOSEST_SYNC);
            } else {
                this.loopSeekingMethod = 0;
            }
            this.shader = arr.getString(R.styleable.AlphaMovieView_shader);
            arr.recycle();
            updateRendererOptions();
       // }
    }

    private void updateRendererOptions() {
        renderer.setPacked(isPacked);
        if (alphaColor != NOT_DEFINED_COLOR) {
            renderer.setAlphaColor(alphaColor);
        }
        if (shader != null) {
            renderer.setCustomShader(shader);
        }
        if (accuracy != NOT_DEFINED) {
            renderer.setAccuracy(accuracy);
        }
    }

    private void addOnSurfacePrepareListener() {
        if (renderer != null) {
            renderer.setOnSurfacePrepareListener(new VideoRenderer.OnSurfacePrepareListener() {
                @Override
                public void surfacePrepared(Surface surface) {
                    isSurfaceCreated = true;
                    mediaPlayer.setSurface(surface);
                    surface.release();
                    if (isDataSourceSet) {
                        prepareAndStartMediaPlayer();
                    }
                }
            });
        }
    }

    private void prepareAndStartMediaPlayer() {
        prepareAsync(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                start();
            }
        });
    }

    private void calculateVideoAspectRatio(int videoWidth, int videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            videoAspectRatio = (float) videoWidth / videoHeight;
        }

        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        double currentAspectRatio = (double) widthSize / heightSize;
        if (currentAspectRatio > videoAspectRatio) {
            widthSize = (int) (heightSize * videoAspectRatio);
        } else {
            heightSize = (int) (widthSize / videoAspectRatio);
        }

        super.onMeasure(View.MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                View.MeasureSpec.makeMeasureSpec(heightSize, heightMode));
    }

    private void onDataSourceSet(MediaMetadataRetriever retriever) {
        int videoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int videoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        if (isPacked) {
            // Packed videos are assumed to be contain the alpha channel on the right side of the
            // original video, so the actual video width is half of the whole video
            videoHeight /= 2.0f;
        }

        calculateVideoAspectRatio(videoWidth, videoHeight);
        isDataSourceSet = true;

        if (isSurfaceCreated) {
            prepareAndStartMediaPlayer();
        }
    }

    public void setAutoPlayAfterResume(boolean autoPlayAfterResume) {
        this.autoPlayAfterResume = autoPlayAfterResume;
    }

    public void setPacked(boolean isPacked) {
        this.isPacked = isPacked;
        renderer.setPacked(isPacked);
        updateRendererOptions();
        renderer.refreshShader();
    }

    private void updateMediaPlayerLoopSetting() {
        if (loopStartMs >= 0 || loopEndMs >= 0) {
            // Disable MediaPlayer's built in looping if manual loop section is specified
            setLooping(false);
        }
    }

    // Sets the start point of a loop. If >= 0, will override any setting set via mediaPlayer.setLooping
    public void setLoopStartMs(long startMs) {
        this.loopStartMs = startMs;
        updateMediaPlayerLoopSetting();
    }

    // Sets the end point of a loop. If >= 0, will override any setting set via mediaPlayer.setLooping
    public void setLoopEndMs(long endMs) {
        this.loopEndMs = endMs;
        updateMediaPlayerLoopSetting();
    }

    public void setVideoFromAssets(String assetsFileName) {
        reset();

        try {
            AssetFileDescriptor assetFileDescriptor = getContext().getAssets().openFd(assetsFileName);
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());

            onDataSourceSet(retriever);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void setVideoFromAssets(String assetsFileName, boolean isPacked) {
        setPacked(isPacked);
        setVideoFromAssets(assetsFileName);
    }

    public void setVideoByUrl(String url) {
        reset();

        try {
            mediaPlayer.setDataSource(url);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(url, new HashMap<String, String>());

            onDataSourceSet(retriever);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

  public void setVideoFromResourceId(Context context, int resId) {
    reset();

    try {
      AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
      if (afd == null) return;

      FileDescriptor fileDescriptor = afd.getFileDescriptor();
      long startOffset = afd.getStartOffset();
      long endOffset = afd.getLength();
      mediaPlayer.setDataSource(fileDescriptor, startOffset, endOffset);

      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      retriever.setDataSource(fileDescriptor, startOffset, endOffset);

      onDataSourceSet(retriever);

    } catch (IOException e) {
      Log.e(TAG + " setVideoFromResourceId", e.getMessage(), e);
    }
  }

    public void setVideoFromFile(FileDescriptor fileDescriptor) {
        reset();

        try {
            mediaPlayer.setDataSource(fileDescriptor);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileDescriptor);

            onDataSourceSet(retriever);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void setVideoFromFile(FileDescriptor fileDescriptor, int startOffset, int endOffset) {
        reset();

        try {
            mediaPlayer.setDataSource(fileDescriptor, startOffset, endOffset);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileDescriptor, startOffset, endOffset);

            onDataSourceSet(retriever);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @TargetApi(23)
    public void setVideoFromMediaDataSource(MediaDataSource mediaDataSource) {
        reset();

        mediaPlayer.setDataSource(mediaDataSource);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mediaDataSource);

        onDataSourceSet(retriever);
    }

    public void setVideoFromUri(Context context, Uri uri) {
        reset();

        try {
            mediaPlayer.setDataSource(context, uri);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, uri);

            onDataSourceSet(retriever);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (autoPlayAfterResume && playAfterResume) {
            playAfterResume = false;
            start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(timeDetector);
        if (isPlaying() && autoPlayAfterResume) {
            playAfterResume = true;
        }
        pause();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
      handler.removeCallbacks(timeDetector);
      TransparentVideoViewManager.destroyView((LinearLayout)this.getParent());
    }

    private void prepareAsync(final MediaPlayer.OnPreparedListener onPreparedListener) {
        if (mediaPlayer != null && state == PlayerState.NOT_PREPARED
                || state == PlayerState.STOPPED) {
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    state = PlayerState.PREPARED;
                    onPreparedListener.onPrepared(mp);
                }
            });
            mediaPlayer.prepareAsync();
        }
    }

    private void startTimeDetector() {
        handler.postDelayed(timeDetector, TIME_DETECTION_INTERVAL_MS);
    }

    public void start() {
        if (mediaPlayer != null) {
            switch (state) {
                case PREPARED:
                    mediaPlayer.start();
                    startTimeDetector();
                    state = PlayerState.STARTED;
                    if (onVideoStartedListener != null) {
                        onVideoStartedListener.onVideoStarted();
                    }
                    break;
                case PAUSED:
                    mediaPlayer.start();
                    startTimeDetector();
                    state = PlayerState.STARTED;
                    break;
                case STOPPED:
                    prepareAsync(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            startTimeDetector();
                            state = PlayerState.STARTED;
                            if (onVideoStartedListener != null) {
                                onVideoStartedListener.onVideoStarted();
                            }
                        }
                    });
                    break;
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null && state == PlayerState.STARTED) {
            mediaPlayer.pause();
            state = PlayerState.PAUSED;
        }
    }

    public void stop() {
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED)) {
            mediaPlayer.stop();
            state = PlayerState.STOPPED;
        }
    }

    public void reset() {
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED ||
                state == PlayerState.STOPPED)) {
            mediaPlayer.reset();
            state = PlayerState.NOT_PREPARED;
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            state = PlayerState.RELEASE;
        }
    }

    public PlayerState getState() {
        return state;
    }

    public boolean isPlaying() {
        return state == PlayerState.STARTED;
    }

    public boolean isPaused() {
        return state == PlayerState.PAUSED;
    }

    public boolean isStopped() {
        return state == PlayerState.STOPPED;
    }

    public boolean isReleased() {
        return state == PlayerState.RELEASE;
    }

    public void seekTo(int msec) {
        mediaPlayer.seekTo(msec);
    }

    public void setLooping(boolean looping) {
        mediaPlayer.setLooping(looping);
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        mediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener){
        mediaPlayer.setOnErrorListener(onErrorListener);
    }

    public void setOnVideoStartedListener(OnVideoStartedListener onVideoStartedListener) {
        this.onVideoStartedListener = onVideoStartedListener;
    }

    public void setOnVideoEndedListener(OnVideoEndedListener onVideoEndedListener) {
        this.onVideoEndedListener = onVideoEndedListener;
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener onSeekCompleteListener) {
        mediaPlayer.setOnSeekCompleteListener(onSeekCompleteListener);
    }

    public void setLoopSeekingMethod(int loopSeekingMethod) {
        this.loopSeekingMethod = loopSeekingMethod;
    }

    public int getLoopSeekingMethod() {
        return this.loopSeekingMethod;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public interface OnVideoStartedListener {
        void onVideoStarted();
    }

    public interface OnVideoEndedListener {
        void onVideoEnded();
    }

    private enum PlayerState {
        NOT_PREPARED, PREPARED, STARTED, PAUSED, STOPPED, RELEASE
    }
}
