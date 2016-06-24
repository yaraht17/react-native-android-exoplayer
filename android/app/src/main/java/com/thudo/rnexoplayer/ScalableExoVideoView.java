/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2013 YIXIA.COM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thudo.rnexoplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.id3.ApicFrame;
import com.google.android.exoplayer.metadata.id3.GeobFrame;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.PrivFrame;
import com.google.android.exoplayer.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer.metadata.id3.TxxxFrame;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.Util;
import com.thudo.rnexoplayer.player.DashRendererBuilder;
import com.thudo.rnexoplayer.player.DemoPlayer;
import com.thudo.rnexoplayer.player.ExtractorRendererBuilder;
import com.thudo.rnexoplayer.player.HlsRendererBuilder;
import com.thudo.rnexoplayer.player.SmoothStreamingRendererBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScalableExoVideoView extends TextureView implements TextureView.SurfaceTextureListener,
        DemoPlayer.Listener, DemoPlayer.CaptionListener, DemoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener{

    public interface Listener {
        void onCompletionListener();
        void onPreparedListener();
        void onErrorListener(String errorString);
//        void onSeekCompleteListener();
//        void onTimedTextListener();
//        void onInfoListener();
//        void onBufferingUpdateListener();
    }

    public static final String TAG = "ScalableVitamioVideo";

    Context mContext;
    protected Activity _activity;

    protected DemoPlayer mMediaPlayer;
    protected boolean playerNeedsPrepare;
    protected SubtitleLayout subtitleLayout;

    private MediaController mediaController;

    private int mCurrentState = DemoPlayer.STATE_IDLE;
//    private int mTargetState = STATE_IDLE;

    private Uri mUri;
    private int contentType;
    private String contentId;
    private String provider;

    protected long playerPosition;

    private Listener mListener;

    public static final int VIDEO_LAYOUT_ORIGIN = 0;
    public static final int VIDEO_LAYOUT_SCALE = 1;
    public static final int VIDEO_LAYOUT_STRETCH = 2;
    public static final int VIDEO_LAYOUT_ZOOM = 3;
    public static final int VIDEO_LAYOUT_FIT_PARENT = 4;

    public ScalableExoVideoView(Context context,Activity activity) {
        super(context);
        mContext = context;
        _activity = activity;
        initVideoView(context);
    }

    public ScalableExoVideoView(Context context, AttributeSet attrs,Activity activity) {

        this(context, attrs, 0,activity);
        mContext = context;
        _activity = activity;
        initVideoView(context);
    }

    public ScalableExoVideoView(Context context, AttributeSet attrs, int defStyle,Activity activity) {

        super(context, attrs, defStyle);
        mContext = context;
        _activity = activity;
        initVideoView(context);
    }


    @Override
    public void onId3Metadata(List<Id3Frame> id3Frames) {
        for (Id3Frame id3Frame : id3Frames) {
            if (id3Frame instanceof TxxxFrame) {
                TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
                Log.i("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
                        txxxFrame.description, txxxFrame.value);
            } else if (id3Frame instanceof PrivFrame) {
                PrivFrame privFrame = (PrivFrame) id3Frame;
                Log.i("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner);
            } else if (id3Frame instanceof GeobFrame) {
                GeobFrame geobFrame = (GeobFrame) id3Frame;
                Log.i("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description);
            } else if (id3Frame instanceof ApicFrame) {
                ApicFrame apicFrame = (ApicFrame) id3Frame;
                Log.i("ID3 TimedMetadata %s: mimeType=%s, description=%s",
                        apicFrame.id, apicFrame.mimeType, apicFrame.description);
            } else if (id3Frame instanceof TextInformationFrame) {
                TextInformationFrame textInformationFrame = (TextInformationFrame) id3Frame;
                Log.i("ID3 TimedMetadata %s: description=%s", textInformationFrame.id,
                        textInformationFrame.description);
            } else {
                Log.i("ID3 TimedMetadata %s", id3Frame.id);
            }
        }
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
//            showControls();
        }
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                mCurrentState=DemoPlayer.STATE_BUFFERING;
                break;
            case ExoPlayer.STATE_ENDED:
                mCurrentState=DemoPlayer.STATE_ENDED;
                onCompletion();
                break;
            case ExoPlayer.STATE_IDLE:
                mCurrentState=DemoPlayer.STATE_IDLE;
                break;
            case ExoPlayer.STATE_PREPARING:
                mCurrentState=DemoPlayer.STATE_PREPARING;
                break;
            case ExoPlayer.STATE_READY:
                mCurrentState=DemoPlayer.STATE_READY;
                onPrepared();
                break;
            default:
                break;
        }
//        playerStateTextView.setText(text);
//        updateButtonVisibilities();
    }

    @Override
    public void onError(Exception e) {
        String errorString = null;
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            errorString = mContext.getString(Util.SDK_INT < 18 ? R.string.error_drm_not_supported
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
        } else if (e instanceof ExoPlaybackException
                && e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
            // Special case for decoder initialization failures.
            MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
                    (MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
            if (decoderInitializationException.decoderName == null) {
                if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                    errorString = mContext.getString(R.string.error_querying_decoders);
                } else if (decoderInitializationException.secureDecoderRequired) {
                    errorString = mContext.getString(R.string.error_no_secure_decoder,
                            decoderInitializationException.mimeType);
                } else {
                    errorString = mContext.getString(R.string.error_no_decoder,
                            decoderInitializationException.mimeType);
                }
            } else {
                errorString = mContext.getString(R.string.error_instantiating_decoder,
                        decoderInitializationException.decoderName);
            }
        }
        if (errorString != null) {
            Toast.makeText(mContext, errorString, Toast.LENGTH_LONG).show();
        }
        playerNeedsPrepare = true;
//        updateButtonVisibilities();
//        showControls();

        mListener.onErrorListener((errorString != null) ? errorString : e.toString());
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (mMediaPlayer == null) {
            return;
        }
        boolean backgrounded = mMediaPlayer.getBackgrounded();
        boolean playWhenReady = mMediaPlayer.getPlayWhenReady();
        releasePlayer();
        preparePlayer(playWhenReady);
        mMediaPlayer.setBackgrounded(backgrounded);
    }

    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.setCues(cues);
    }

    public void setListener(Listener listener){
        mListener = listener;
    }
    //////////////////

    private void releasePlayer() {
        if (mMediaPlayer != null) {
//            debugViewHelper.stop();
//            debugViewHelper = null;
            playerPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.release();
            mMediaPlayer = null;
//            eventLogger.endSession();
//            eventLogger = null;
        }
    }

    private void preparePlayer(boolean playWhenReady) {
        if (mMediaPlayer == null) {
            mMediaPlayer = new DemoPlayer(getRendererBuilder());
            mMediaPlayer.addListener(this);
            mMediaPlayer.setCaptionListener(this);
            mMediaPlayer.setMetadataListener(this);
            mMediaPlayer.seekTo(playerPosition);
            playerNeedsPrepare = true;
//            mediaController.setMediaPlayer(player.getPlayerControl());
//            mediaController.setEnabled(true);
//            eventLogger = new EventLogger();
//            eventLogger.startSession();
//            player.addListener(eventLogger);
//            player.setInfoListener(eventLogger);
//            player.setInternalErrorListener(eventLogger);
//            debugViewHelper = new DebugTextViewHelper(player, debugTextView);
//            debugViewHelper.start();
        }
        if (playerNeedsPrepare) {
            mMediaPlayer.prepare();
            playerNeedsPrepare = false;
//            updateButtonVisibilities();
        }
        mMediaPlayer.setSurface(mSurface);
        mMediaPlayer.setPlayWhenReady(playWhenReady);

    }
    private DemoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(_activity, "ExoPlayerDemo");
        switch (contentType) {
            case Util.TYPE_SS:
                return new SmoothStreamingRendererBuilder(_activity, userAgent, mUri.toString(),
                        new SmoothStreamingTestMediaDrmCallback());
            case Util.TYPE_DASH:
                return new DashRendererBuilder(_activity, userAgent, mUri.toString(),
                        new WidevineTestMediaDrmCallback(contentId, provider));
            case Util.TYPE_HLS:
                return new HlsRendererBuilder(_activity, userAgent, mUri.toString());
            case Util.TYPE_OTHER:
                return new ExtractorRendererBuilder(_activity, userAgent, mUri);
            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthAspectRatio) {
//        shutterView.setVisibility(View.GONE);
        Log.d("onVideoSizeChanged: (%dx%d)", width, height);
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoAspectRatio = ( height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
        if (mVideoWidth != 0 && mVideoHeight != 0)
            setVideoLayout(mVideoLayout, ( height == 0 ? 1 : (width * pixelWidthAspectRatio) / height));
    }





    ////////////////////

    public void onPrepared() {
        Log.d("onPrepared");

        // Get the capabilities of the player for this stream
        //TODO mCanPause

        mListener.onPreparedListener();

        if (mMediaController != null)
            mMediaController.setEnabled(true);
//        mVideoWidth = mp.getVideoWidth();
//        mVideoHeight = mp.getVideoHeight();
//        mVideoAspectRatio = mp.getVideoAspectRatio();

        long seekToPosition = mSeekWhenPrepared;
        if (seekToPosition != 0)
            seekTo(seekToPosition);

//        if (mVideoWidth != 0 && mVideoHeight != 0) {
//            setVideoLayout(mVideoLayout, mAspectRatio);
//            if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
//                if (mTargetState == STATE_PLAYING) {
//                    start();
//                    if (mMediaController != null)
//                        mMediaController.show();
//                } else if (!isPlaying() && (seekToPosition != 0 || getCurrentPosition() > 0)) {
//                    if (mMediaController != null)
//                        mMediaController.show(0);
//                }
//            }
//        } else if (mTargetState == STATE_PLAYING) {
            start();
//        }
    };

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

        Surface surface = new Surface(surfaceTexture);
        mSurface = surface;

        class SetSurface implements Runnable {
            Surface mSurface;
            SetSurface(Surface surface){mSurface = surface;}
            @Override
            public void run() {
                try {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.setSurface(mSurface);
                        resume();
                    } else {
                        openVideo();
                    }
                }
                catch(Exception ex){
                    android.util.Log.e("ReactVideoView","onDetachedFromWindow err");
                }
            }
        }

        new Thread(new SetSurface(surface)).start();

    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
//        boolean isValidState = (mTargetState == STATE_PLAYING);
        boolean hasValidSize = (mVideoWidth == width && mVideoHeight == height);
        if (mMediaPlayer != null && hasValidSize) {
            if (mSeekWhenPrepared != 0)
                seekTo(mSeekWhenPrepared);
            start();
            if (mMediaController != null) {
                if (mMediaController.isShowing())
                    mMediaController.hide();
                mMediaController.show();
            }
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurface = null;
        if (mMediaController != null) mMediaController.hide();
        release(true);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        if (mMediaPlayer == null) {
//            return;
//        }
//
//        if (isPlaying()) {
//            stop();
//        }
        release(true);
        mMediaPlayer = null;
    }

    //
    private static String buildResolutionString(MediaFormat format) {
        return format.width == MediaFormat.NO_VALUE || format.height == MediaFormat.NO_VALUE
                ? "" : format.width + "x" + format.height;
    }

    private static String buildAudioPropertyString(MediaFormat format) {
        return format.channelCount == MediaFormat.NO_VALUE || format.sampleRate == MediaFormat.NO_VALUE
                ? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
    }

    private static String buildLanguageString(MediaFormat format) {
        return TextUtils.isEmpty(format.language) || "und".equals(format.language) ? ""
                : format.language;
    }

    private static String buildBitrateString(MediaFormat format) {
        return format.bitrate == MediaFormat.NO_VALUE ? ""
                : String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
    }

    private static String joinWithSeparator(String first, String second) {
        return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
    }

    private static String buildTrackIdString(MediaFormat format) {
        return format.trackId == null ? "" : " (" + format.trackId + ")";
    }
    private void configureSubtitleView() {
        CaptionStyleCompat style;
        float fontScale;
        if (Util.SDK_INT >= 19) {
            style = getUserCaptionStyleV19();
            fontScale = getUserCaptionFontScaleV19();
        } else {
            style = CaptionStyleCompat.DEFAULT;
            fontScale = 1.0f;
        }
        subtitleLayout.setStyle(style);
        subtitleLayout.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }

    /**
     * Makes a best guess to infer the type from a media {@link Uri} and an optional overriding file
     * extension.
     *
     * @param uri The {@link Uri} of the media.
     * @param fileExtension An overriding file extension.
     * @return The inferred type.
     */
    private static int inferContentType(Uri uri, String fileExtension) {
        String lastPathSegment = !TextUtils.isEmpty(fileExtension) ? "." + fileExtension
                : uri.getLastPathSegment();
        return Util.inferContentType(lastPathSegment);
    }


    ///////////////////
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    public void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
//            mCurrentState = STATE_IDLE;
//            mTargetState = STATE_IDLE;
        }
    }

    private void openVideo() {
        android.util.Log.d(TAG, "openVideo: "+mUri+":"+mSurface+":");
        if (mUri == null || mSurface == null )
            return;

        preparePlayer(true);
        _activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }


    /////////////////////
    private long mDuration;

    private float mAspectRatio = 0;
    private int mVideoLayout = VIDEO_LAYOUT_SCALE;
//    private SurfaceHolder mSurfaceHolder = null;
    private Surface mSurface = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private float mVideoAspectRatio;

    private boolean mHardwareDecoder = false;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private float mVolumeLeft;
    private float mVolumeRight;
    private boolean mMuted;
    private MediaController mMediaController;
    private View mMediaBufferingIndicator;

    private int mCurrentBufferPercentage;
    private long mSeekWhenPrepared; // recording the seek position while preparing
    private Map<String, String> mHeaders;
    private int mBufSize;

    public void onCompletion() {
        Log.d("onCompletion");
//            mCurrentState = STATE_PLAYBACK_COMPLETED;
//            mTargetState = STATE_PLAYBACK_COMPLETED;
        if (mMediaController != null)
            mMediaController.hide();
        mListener.onCompletionListener();

    }


//    private OnInfoListener mInfoListener = new OnInfoListener() {
//        @Override
//        public boolean onInfo(MediaPlayer mp, int what, int extra) {
//            Log.d("onInfo: (%d, %d)", what, extra);
//            if (mOnInfoListener != null) {
//                mOnInfoListener.onInfo(mp, what, extra);
//            } else if (mMediaPlayer != null) {
//                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
//                    mMediaPlayer.pause();
//                    if (mMediaBufferingIndicator != null)
//                        mMediaBufferingIndicator.setVisibility(View.VISIBLE);
//                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
//                    mMediaPlayer.start();
//                    if (mMediaBufferingIndicator != null)
//                        mMediaBufferingIndicator.setVisibility(View.GONE);
//                }
//            }
//            return true;
//        }
//    };


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    /**
     * Set the display options
     *
     * @param layout      <ul>
     *                    <li>{@link #VIDEO_LAYOUT_ORIGIN}
     *                    <li>{@link #VIDEO_LAYOUT_SCALE}
     *                    <li>{@link #VIDEO_LAYOUT_STRETCH}
     *                    <li>{@link #VIDEO_LAYOUT_ZOOM}
     *                    <li>{@link #VIDEO_LAYOUT_FIT_PARENT}
     *                    </ul>
     * @param aspectRatio video aspect ratio, will audo detect if 0.
     */
    public void setVideoLayout(int layout, float aspectRatio) {
        LayoutParams lp = getLayoutParams();
        Pair<Integer, Integer> res = ScreenResolution.getResolution(mContext);
        int windowWidth = res.first.intValue(), windowHeight = res.second.intValue();
        float windowRatio = windowWidth / (float) windowHeight;
        float videoRatio = aspectRatio <= 0.01f ? mVideoAspectRatio : aspectRatio;
        mSurfaceHeight = mVideoHeight;
        mSurfaceWidth = mVideoWidth;
        if (VIDEO_LAYOUT_ORIGIN == layout && mSurfaceWidth < windowWidth && mSurfaceHeight < windowHeight) {
            lp.width = (int) (mSurfaceHeight * videoRatio);
            lp.height = mSurfaceHeight;
        } else if (layout == VIDEO_LAYOUT_ZOOM) {
            lp.width = windowRatio > videoRatio ? windowWidth : (int) (videoRatio * windowHeight);
            lp.height = windowRatio < videoRatio ? windowHeight : (int) (windowWidth / videoRatio);
        } else if (layout == VIDEO_LAYOUT_FIT_PARENT) {
            ViewGroup parent = (ViewGroup) getParent();
            float parentRatio = ((float) parent.getWidth()) / ((float) parent.getHeight());
            lp.width = (parentRatio < videoRatio) ? parent.getWidth() : Math.round(((float) parent.getHeight()) * videoRatio);
            lp.height = (parentRatio > videoRatio) ? parent.getHeight() : Math.round(((float) parent.getWidth()) / videoRatio);
        } else {
            boolean full = layout == VIDEO_LAYOUT_STRETCH;
            lp.width = (full || windowRatio < videoRatio) ? windowWidth : (int) (videoRatio * windowHeight);
            lp.height = (full || windowRatio > videoRatio) ? windowHeight : (int) (windowWidth / videoRatio);
        }
        setLayoutParams(lp);
//        getSurfaceTexture().setFixedSize(mSurfaceWidth, mSurfaceHeight);
        Log.d("VIDEO: %dx%dx%f, Surface: %dx%d, LP: %dx%d, Window: %dx%dx%f", mVideoWidth, mVideoHeight, mVideoAspectRatio, mSurfaceWidth, mSurfaceHeight, lp.width, lp.height, windowWidth, windowHeight, windowRatio);
        mVideoLayout = layout;
        mAspectRatio = aspectRatio;
    }

    @SuppressWarnings("deprecation")
    private void initVideoView(Context ctx) {
        mContext = ctx;
        mVideoWidth = 0;
        mVideoHeight = 0;
        android.util.Log.d(TAG, "initVideoView: init");

        setSurfaceTextureListener(this);
//        getSurfaceTexture().setFormat(PixelFormat.RGBA_8888); // PixelFormat.RGB_565
//        getSurfaceTexture().addCallback(mSHCallback);
        // this value only use Hardware decoder before Android 2.3
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && mHardwareDecoder) {
//            getSurfaceTexture().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        }
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
//        mCurrentState = STATE_IDLE;
//        mTargetState = STATE_IDLE;
        if (ctx instanceof Activity)
            ((Activity) ctx).setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    public boolean isValid() {
        return (mSurface != null && mSurface.isValid());
    }



//    public void setMediaController(MediaController controller) {
//        if (mMediaController != null)
//            mMediaController.hide();
//        mMediaController = controller;
//        attachMediaController();
//    }

//    public void setMediaBufferingIndicator(View mediaBufferingIndicator) {
//        if (mMediaBufferingIndicator != null)
//            mMediaBufferingIndicator.setVisibility(View.GONE);
//        mMediaBufferingIndicator = mediaBufferingIndicator;
//    }

//    private void attachMediaController() {
//        if (mMediaPlayer != null && mMediaController != null) {
//            mMediaController.setMediaPlayer(this);
//            View anchorView = this.getParent() instanceof View ? (View) this.getParent() : this;
//            mMediaController.setAnchorView(anchorView);
//            mMediaController.setEnabled(isInPlaybackState());
//
//            if (mUri != null) {
//                List<String> paths = mUri.getPathSegments();
//                String name = paths == null || paths.isEmpty() ? "null" : paths.get(paths.size() - 1);
//                mMediaController.setFileName(name);
//            }
//        }
//    }


    private void release(boolean cleartargetstate) {

        releasePlayer();

        _activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }


    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    public void start() {
        _activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        openVideo();
//        if (isInPlaybackState()) {
//            mMediaPlayer.start();
//            mCurrentState = STATE_PLAYING;
//        }
//        mTargetState = STATE_PLAYING;
    }

    public void pause() {
        if (isInPlaybackState()) {
//            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
//                mCurrentState = STATE_PAUSED;
//            }
        }
        _activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        if (isInPlaybackState()) {
            release(false);
//            mCurrentState = STATE_SUSPEND_UNSUPPORTED;
            Log.d("Unable to suspend video. Release MediaPlayer.");
        }
    }

    public void resume() {
        _activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        if (mSurface == null && mCurrentState == STATE_SUSPEND) {
//            mTargetState = STATE_RESUME;
//        } else if (mCurrentState == STATE_SUSPEND_UNSUPPORTED) {
            openVideo();
//        }
//        else{
//
//        }
    }

    public long getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0)
                return mDuration;
            mDuration = mMediaPlayer.getDuration() ;
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    public long getCurrentPosition() {
        if (isInPlaybackState())
            return mMediaPlayer.getCurrentPosition();
        return 0;
    }

    public void seekTo(long msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

//    public boolean isPlaying() {
//        return isInPlaybackState() && mMediaPlayer.isPlaying();
//    }

    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            mCurrentBufferPercentage = mMediaPlayer.getBufferedPercentage();
            return mCurrentBufferPercentage;
        }
        return 0;
    }

//    public void setVolume(float leftVolume, float rightVolume) {
//        mVolumeLeft = leftVolume;
//        mVolumeRight = rightVolume;
//        if (mMediaPlayer != null)
//            mMediaPlayer.setVolume(leftVolume, rightVolume);
//    }

//    public void setMuted(final boolean muted) {
//        mMuted = muted;
//        if (mMediaPlayer != null){
//            if (mMuted) {
//                setVolume(0, 0);
//            } else {
//                setVolume(mVolumeLeft, mVolumeRight);
//            }
//        }
//    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public float getVideoAspectRatio() {
        return mVideoAspectRatio;
    }

//    /**
//     * Must set before {@link #setVideoURI}
//     * @param chroma
//     */
//    public void setVideoChroma(int chroma) {
////        getSurfaceTexture().setFormat(chroma == MediaPlayer.VIDEOCHROMA_RGB565 ? PixelFormat.RGB_565 : PixelFormat.RGBA_8888); // PixelFormat.RGB_565
//        mVideoChroma = chroma;
//    }
//
//    public void setHardwareDecoder(boolean hardware) {
//        mHardwareDecoder= hardware;
//    }

//    public void setVideoQuality(int quality) {
//        if (mMediaPlayer != null)
//            mMediaPlayer.setVideoQuality(quality);
//    }

    public void setBufferSize(int bufSize) {
        mBufSize = bufSize;
    }

//    public void setRate(final float speed) {
//        if (mMediaPlayer != null)
//            mMediaPlayer.setPlaybackSpeed(speed);
//    }

    public boolean isBuffering() {
        if (mMediaPlayer != null)
            return mCurrentState == DemoPlayer.STATE_BUFFERING;
        return false;
    }

//    public String getMetaEncoding() {
//        if (mMediaPlayer != null)
//            return mMediaPlayer.getMetaEncoding();
//        return null;
//    }

//    public void setMetaEncoding(String encoding) {
//        if (mMediaPlayer != null)
//            mMediaPlayer.setMetaEncoding(encoding);
//    }

//    public SparseArray<MediaFormat> getAudioTrackMap(String encoding) {
//        if (mMediaPlayer != null)
//            return mMediaPlayer.findTrackFromTrackInfo(TrackInfo.MEDIA_TRACK_TYPE_AUDIO, mMediaPlayer.getTrackInfo(encoding));
//        return null;
//    }

    protected boolean isInPlaybackState() {
        return (mMediaPlayer != null && mCurrentState != DemoPlayer.STATE_IDLE && mCurrentState != DemoPlayer.STATE_PREPARING);
    }
}