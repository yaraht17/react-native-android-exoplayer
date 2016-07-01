package com.thudo.rnexoplayer;

import android.app.Activity;
import android.os.Handler;
import android.view.WindowManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.thudo.rnexoplayer.player.DemoPlayer;

/**
 * @author phuongtq
 * @file ....java
 * @brief ....java source file.
 * <p/>
 * File/module comments.
 * @mobile 01684499886
 * @note No Note at the moment
 * @bug No known bugs.
 * <p/>
 * <pre>
 * MODIFICATION HISTORY:
 *
 * Ver   Who  	  Date       Changes
 * ----- --------- ---------- -----------------------------------------------
 * 1.00  phuongtq   3/3/2016 First release
 *
 *
 * </pre>
 */


public class RNExoVideoView extends ScalableExoVideoView implements ScalableExoVideoView.Listener {
    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_SEEK("onVideoSeek"),
        EVENT_END("onVideoEnd");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static final String EVENT_PROP_FAST_FORWARD = "canPlayFastForward";
    public static final String EVENT_PROP_SLOW_FORWARD = "canPlaySlowForward";
    public static final String EVENT_PROP_SLOW_REVERSE = "canPlaySlowReverse";
    public static final String EVENT_PROP_REVERSE = "canPlayReverse";
    public static final String EVENT_PROP_STEP_FORWARD = "canStepForward";
    public static final String EVENT_PROP_STEP_BACKWARD = "canStepBackward";

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_PLAYABLE_DURATION = "playableDuration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";
    public static final String EVENT_PROP_BUFFER_PERCENT = "bufferPercent";
    public static final String EVENT_PROP_BUFFER_POSITION = "bufferPosition";
    public static final String EVENT_PROP_SEEK_TIME = "seekTime";

    public static final String EVENT_PROP_VIDEO_TRACKS = "videoTracks";
    public static final String EVENT_PROP_AUDIO_TRACKS = "audioTracks";
    public static final String EVENT_PROP_TEXT_TRACKS = "textTracks";

    public static final String EVENT_PROP_SELECTED_VIDEO_TRACK = "selectedVideoTrack";
    public static final String EVENT_PROP_SELECTED_AUDIO_TRACK = "selectedAudioTrack";
    public static final String EVENT_PROP_SELECTED_TEXT_TRACK = "selectedTextTracks";


    public static final String EVENT_PROP_ERROR = "error";
    public static final String EVENT_PROP_WHAT = "what";
    public static final String EVENT_PROP_EXTRA = "extra";

    Activity mActivity;
    private ThemedReactContext mThemedReactContext;
    private RCTEventEmitter mEventEmitter;

    private Handler mProgressUpdateHandler = new Handler();
    private Runnable mProgressUpdateRunnable = null;

    private boolean mRepeat = false;
    private boolean mPaused = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private float mRate = 1.0f;
    private ScalableType mResizeMode = ScalableType.CENTER;

    private boolean mMediaPlayerValid = false; // True if mMediaPlayer is in prepared, started, or paused state.
    private long mVideoDuration = 0;
    private long mVideoBufferedDuration = 0;

    private String mSrcUriString = null;
    private boolean mRunOnLoad = true;
    private String mSrcType = "mp4";
    private boolean mSrcIsNetwork = false;

    private boolean mStarted = false;


    RNExoVideoView(ThemedReactContext themedReactContext ,Activity activity){
        super(themedReactContext,activity);
        FullLog.d("RNExoVideoView");
        mThemedReactContext = themedReactContext;
        mActivity = activity;
        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
        setListener(this);
        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                //
//                FullLog.d("mProgressUpdateRunnable " + isValid());

                if (mStarted) {
                    if (!isValid()) {
                        mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);
                        return;
                    }

                    if (mMediaPlayerValid && (mCurrentState!=DemoPlayer.STATE_ENDED)) {

                        WritableMap event = Arguments.createMap();
                        mVideoDuration = getDuration();
                        event.putDouble(EVENT_PROP_CURRENT_TIME, getCurrentPosition() / 1000.0);
                        event.putDouble(EVENT_PROP_BUFFER_POSITION, getBufferedPosition() / 1000.0);
                        event.putDouble(EVENT_PROP_BUFFER_PERCENT, getBufferPercentage());
                        event.putDouble(EVENT_PROP_PLAYABLE_DURATION, mVideoDuration / 1000.0); //TODO:mBufferUpdateRunnable
                        mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
                    }
                }

                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 1000);
            }
        };
        mProgressUpdateHandler.post(mProgressUpdateRunnable);
    }


    public void setSrc(String src,boolean runOnLoad) {
        FullLog.d("setSrc: " + src);

        mSrcUriString = src;
        mRunOnLoad = runOnLoad;
        mMediaPlayerValid = false;
        mVideoDuration = -1;
        mVideoBufferedDuration = 0;

        WritableMap srcRet = Arguments.createMap();
        srcRet.putString(RNExoVideoViewManager.PROP_SRC_URI, src);
        WritableMap event = Arguments.createMap();
        event.putMap(RNExoVideoViewManager.PROP_SRC, srcRet);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD_START.toString(), event);
        release();
        setRunOnLoad(runOnLoad);
        setVideoPath(src);

//        setMediaController(new MediaController(mThemedReactContext));
        requestFocus();

    }

    public void setResizeModeModifier(final ScalableType resizeMode) {
        FullLog.d("setResizeModeModifier: " + resizeMode);
        mResizeMode = resizeMode;

        if (mMediaPlayerValid) {
            setScalableType(resizeMode);
            invalidate();
        }
    }

    public void setPausedModifier(final boolean pause) {
        FullLog.d("setPausedModifier: " + pause);
        mPaused = pause;

        if (mMediaPlayerValid) {
            if (pause) {
                pause();
            }
            else{
                start();
            }
        }
    }

    @Override
    public void seekTo(long msec) {

        if (mMediaPlayerValid) {
            WritableMap event = Arguments.createMap();
            event.putDouble(EVENT_PROP_CURRENT_TIME, getCurrentPosition() / 1000.0);
            event.putDouble(EVENT_PROP_SEEK_TIME, msec / 1000.0);
            mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), event);

            super.seekTo(msec);
        }
    }

    @Override
    public void onPreparedListener() {
        FullLog.d("onPreparedListener");
        mMediaPlayerValid = true;
        mVideoDuration = mMediaPlayer.getDuration();
        mStarted = true;

        WritableMap event = Arguments.createMap();
        event.putDouble(EVENT_PROP_DURATION, mVideoDuration / 1000.0);
        event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getCurrentPosition() / 1000.0);
        // TODO: Actually check if you can.
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_FORWARD, true);
        event.putBoolean(EVENT_PROP_SLOW_REVERSE, true);
        event.putBoolean(EVENT_PROP_REVERSE, true);
        event.putBoolean(EVENT_PROP_FAST_FORWARD, true);
        event.putBoolean(EVENT_PROP_STEP_BACKWARD, true);
        event.putBoolean(EVENT_PROP_STEP_FORWARD, true);

        event.putInt(EVENT_PROP_SELECTED_VIDEO_TRACK, getSelectedTrack(DemoPlayer.TYPE_VIDEO));
        event.putInt(EVENT_PROP_SELECTED_AUDIO_TRACK, getSelectedTrack(DemoPlayer.TYPE_AUDIO));
        event.putInt(EVENT_PROP_SELECTED_TEXT_TRACK, getSelectedTrack(DemoPlayer.TYPE_TEXT));

        WritableArray videoTracks = Arguments.createArray();
        String[] trackNameArray= getTrackNameArray(DemoPlayer.TYPE_VIDEO);
        for (String trackName :
                trackNameArray) {
            videoTracks.pushString(trackName);
        }
        event.putArray(EVENT_PROP_VIDEO_TRACKS, videoTracks);

        WritableArray audioTracks = Arguments.createArray();
        trackNameArray= getTrackNameArray(DemoPlayer.TYPE_AUDIO);
        for (String trackName :
                trackNameArray) {
            audioTracks.pushString(trackName);
        }
        event.putArray(EVENT_PROP_AUDIO_TRACKS, audioTracks);

        WritableArray textTracks = Arguments.createArray();
        trackNameArray= getTrackNameArray(DemoPlayer.TYPE_TEXT);
        for (String trackName :
                trackNameArray) {
            textTracks.pushString(trackName);
        }
        event.putArray(EVENT_PROP_TEXT_TRACKS, textTracks);




        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), event);

        _activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        applyModifiers();
        setResizeModeModifier(mResizeMode);

//        class StartVideo implements Runnable {
//            @Override
//            public void run() {
//                start();
//            }
//        }
//        new Thread(new StartVideo()).start();
    }

    @Override
    public void onErrorListener(String errorString) {

        WritableMap event = Arguments.createMap();
        event.putString(EVENT_PROP_ERROR, errorString);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
    }

    @Override
    public void onCompletionListener() {
//        mMediaPlayerValid = false;
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), null);
        _activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDetachedFromWindow() {
//        mVideoValid = false;
        mMediaPlayerValid = false;
        _activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        release();

        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        try {
            super.onAttachedToWindow();
            setSrc(mSrcUriString,mRunOnLoad);
        }
        catch(Exception ex){
            FullLog.e(ex.toString());
        }

    }
}
