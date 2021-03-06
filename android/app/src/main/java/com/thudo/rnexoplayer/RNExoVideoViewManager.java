package com.thudo.rnexoplayer;

import android.app.Activity;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.thudo.rnexoplayer.RNExoVideoView.Events;
import com.thudo.rnexoplayer.player.DemoPlayer;

import java.util.Map;

import javax.annotation.Nullable;

public class RNExoVideoViewManager extends SimpleViewManager<RNExoVideoView>  {

    public static final String REACT_CLASS = "ExoVideo";

    public static final String PROP_SRC = "src";
    public static final String PROP_SRC_URI = "uri";
    public static final String PROP_SRC_RUNONLOAD = "runOnLoad";
    public static final String PROP_SRC_TYPE = "type";
    public static final String PROP_SRC_IS_NETWORK = "isNetwork";
    public static final String PROP_RESIZE_MODE = "resizeMode";
    public static final String PROP_REPEAT = "repeat";
    public static final String PROP_PAUSED = "paused";
    public static final String PROP_MUTED = "muted";
    public static final String PROP_VOLUME = "volume";
    public static final String PROP_SEEK = "seek";
    public static final String PROP_RATE = "rate";

    public static final String PROP_SELECTED_VIDEO_TRACK = "selectedVideoTrack";
    public static final String PROP_SELECTED_AUDIO_TRACK = "selectedAudioTrack";
    public static final String PROP_SELECTED_TEXT_TRACK = "selectedTextTrack";


    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected RNExoVideoView createViewInstance(ThemedReactContext themedReactContext) {
        return new RNExoVideoView(themedReactContext);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @Override
    @Nullable
    public Map getExportedViewConstants() {
        return MapBuilder.of(
                "ScaleNone", Integer.toString(ScalableType.LEFT_TOP.ordinal()),
                "ScaleToFill", Integer.toString(ScalableType.FIT_XY.ordinal()),
                "ScaleAspectFit", Integer.toString(ScalableType.FIT_CENTER.ordinal()),
                "ScaleAspectFill", Integer.toString(ScalableType.CENTER_CROP.ordinal())
        );
    }

    @ReactProp(name = PROP_SRC)
    public void setSrc(final RNExoVideoView videoView, @Nullable ReadableMap src) {
        videoView.setSrc(
                src.getString(PROP_SRC_URI),src.getBoolean(PROP_SRC_RUNONLOAD)
        );
    }

    @ReactProp(name = PROP_RESIZE_MODE)
    public void setResizeMode(final RNExoVideoView videoView, final String resizeModeOrdinalString) {
        videoView.setResizeModeModifier(ScalableType.values()[Integer.parseInt(resizeModeOrdinalString)]);
    }




//    @ReactProp(name = PROP_REPEAT, defaultBoolean = false)
//    public void setRepeat(final RNExoVideoView videoView, final boolean repeat) {
//        videoView.setRepeatModifier(repeat);
//    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(final RNExoVideoView videoView, final boolean paused) {
        FullLog.d("setPaused " + paused);
        videoView.setPausedModifier(paused);
    }

//    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
//    public void setMuted(final RNExoVideoView videoView, final boolean muted) {
//        videoView.setMutedModifier(muted);
//    }

//    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
//    public void setVolume(final RNExoVideoView videoView, final float volume) {
//        videoView.setVolumeModifier(volume);
//    }

    @ReactProp(name = PROP_SEEK)
    public void setSeek(final RNExoVideoView videoView, final float seek) {
        videoView.seekTo(Math.round(seek * 1000.0f));
    }

    @ReactProp(name = PROP_SELECTED_VIDEO_TRACK)
    public void setSelectedVideoTrack(final RNExoVideoView videoView, final int index) {
        if (index < videoView.getNumberTrack(DemoPlayer.TYPE_VIDEO))
            videoView.setSelectedTrack(DemoPlayer.TYPE_VIDEO,index);
    }

    @ReactProp(name = PROP_SELECTED_AUDIO_TRACK)
    public void setSelectedAudioTrack(final RNExoVideoView videoView, final int index) {
        if (index < videoView.getNumberTrack(DemoPlayer.TYPE_AUDIO))
            videoView.setSelectedTrack(DemoPlayer.TYPE_AUDIO,index);
    }

    @ReactProp(name = PROP_SELECTED_TEXT_TRACK)
    public void setSelectedTextTrack(final RNExoVideoView videoView, final int index) {
        if (index < videoView.getNumberTrack(DemoPlayer.TYPE_TEXT))
            videoView.setSelectedTrack(DemoPlayer.TYPE_TEXT,index);
    }

//    @ReactProp(name = PROP_RATE)
//    public void setRate(final RNExoVideoView videoView, final float rate) {
//        videoView.setRateModifier(rate);
//    }
}
