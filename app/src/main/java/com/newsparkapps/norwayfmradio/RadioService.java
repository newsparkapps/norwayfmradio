package com.newsparkapps.norwayfmradio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

import static com.newsparkapps.norwayfmradio.MyApplication.CHANNEL_ID;

public class RadioService extends Service implements Player.EventListener, AudioManager.OnAudioFocusChangeListener{

    public static final String ACTION_PLAY = "com.newsparkapps.norwayfmradio.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.newsparkapps.norwayfmradio.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.newsparkapps.norwayfmradio.ACTION_STOP";

    private final IBinder iBinder = new LocalBinder();
    private Handler handler;
    private final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private SimpleExoPlayer exoPlayer;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private boolean onGoingCall = false;
    private TelephonyManager telephonyManager;
    private WifiManager.WifiLock wifiLock;
    private AudioManager audioManager;
    private MediaNotificationManager notificationManager;
    private String status;
    private String strAppName,img;
    private String strLiveBroadcast;
    private String streamUrl;
    private boolean isPlaying = false;

    public RadioService() {
    }

    public class LocalBinder extends Binder {
        public RadioService getService() {
            return RadioService.this;
        }
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    };

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                if(state == TelephonyManager.CALL_STATE_OFFHOOK
                        || state == TelephonyManager.CALL_STATE_RINGING){
                    if(!isPlaying()) return;
                    onGoingCall = true;
                    stop();
                } else if (state == TelephonyManager.CALL_STATE_IDLE){
                    if(!onGoingCall) return;
                    onGoingCall = false;
                    resume();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    };

    private MediaSessionCompat.Callback mediasSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            resume();
        }

        @Override
        public void onPause() {
            super.onPause();
            pause();
        }

        @Override
        public void onStop() {
            super.onStop();
            stop();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        strAppName = getResources().getString(R.string.app_name);
        strLiveBroadcast = "Start Hindi FM Radio";
        onGoingCall = false;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = new MediaNotificationManager(this);
        wifiLock = ((WifiManager) Objects.requireNonNull(getApplicationContext().getSystemService(Context.WIFI_SERVICE)))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock");

        try {
            mediaSession = new MediaSessionCompat(this, getClass().getSimpleName());
            transportControls = mediaSession.getController().getTransportControls();
            mediaSession.setActive(true);
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, strAppName)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, strAppName)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Now Playing")
                    .build());
            mediaSession.setCallback(mediasSessionCallback);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        try {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            assert telephonyManager != null;
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        handler = new Handler();
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector);
        exoPlayer.addListener(this);
        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        status = AppCons.IDLE;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if(TextUtils.isEmpty(action))
            return START_NOT_STICKY;
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            stop();
            return START_NOT_STICKY;
        }

        if(action.equalsIgnoreCase(ACTION_PLAY)){
            transportControls.play();
            status = AppCons.PLAYING;
            setMedia(FmConstants.fmname,FmConstants.fmimage, true);
            notificationManager.startNotify(status);
        } else if(action.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
            status = AppCons.PAUSED;
            setMedia(FmConstants.fmname,FmConstants.fmimage, true);
            notificationManager.startNotify(status);
        } else if(action.equalsIgnoreCase(ACTION_STOP)){
            transportControls.stop();
            status = AppCons.STOPPED;
        }

        return START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(status.equals(AppCons.IDLE))
            stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    public void setMedia(String name, String image, boolean playing) {
        Log.i("Roney","setMedia "+name+" "+image+" "+playing);
        try {
            strAppName = name;
            strLiveBroadcast = name;
            img = image;
            isPlaying = playing;

            mediaSession = new MediaSessionCompat(this, getClass().getSimpleName());
            transportControls = mediaSession.getController().getTransportControls();
            mediaSession.setActive(true);
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, strAppName)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, strAppName)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Now Playing")
                    .build());
            mediaSession.setCallback(mediasSessionCallback);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        pause();
        exoPlayer.release();
        exoPlayer.removeListener(this);
        if(telephonyManager != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        notificationManager.cancelNotify();
        mediaSession.release();
        unregisterReceiver(becomingNoisyReceiver);
        super.onDestroy();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                exoPlayer.setVolume(0.8f);
                resume();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (isPlaying())
                    exoPlayer.setVolume(0.1f);
                break;
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                status = AppCons.LOADING;
                break;
            case Player.STATE_ENDED:
                status = AppCons.STOPPED;
                break;
            case Player.STATE_IDLE:
                status = AppCons.IDLE;
                break;
            case Player.STATE_READY:
                status = playWhenReady ? AppCons.PLAYING : AppCons.PAUSED;
                break;
            default:
                status = AppCons.IDLE;
                break;
        }

        if (status.equals(AppCons.IDLE)) {
            //notificationManager.cancelNotify();
        } else if(status.equals(AppCons.LOADING)) {
            setMedia(FmConstants.fmname,FmConstants.fmimage, isPlaying);
        } else if(status.equals(AppCons.PLAYING)) {
            //notificationManager.cancelNotify();
            notificationManager.setMedia(FmConstants.fmname,FmConstants.fmimage,isPlaying);
            notificationManager.startNotify(status);
        } else {

        }
        EventBus.getDefault().post(status);

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        EventBus.getDefault().post(AppCons.ERROR);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }


    public void play(String streamUrl) {
        this.streamUrl = streamUrl;
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }

        if(streamUrl.contains("m3u8")) {
            Handler mHandler = new Handler();
            String userAgent = Util.getUserAgent(this, "User Agent");
            DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory(
                    userAgent, null,
                    DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    1800000,
                    true);
            HlsMediaSource mediaSource = new HlsMediaSource(Uri.parse(streamUrl), dataSourceFactory, 1800000,
                    mHandler, null);

            exoPlayer.setPlayWhenReady(true);
            exoPlayer.prepare(mediaSource);
            FmConstants.isPlaying =true;
        } else {
            String userAgent = Util.getUserAgent(this, "All India Radio");
            DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory(
                    userAgent,
                    null /* listener */,
                    DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                    true /* allowCrossProtocolRedirects */
            );


            //DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, getUserAgent(), BANDWIDTH_METER);
            ExtractorMediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(new DefaultExtractorsFactory())
                    .createMediaSource(Uri.parse(streamUrl));

            exoPlayer.setPlayWhenReady(true);
            exoPlayer.prepare(mediaSource);
            FmConstants.isPlaying =true;
        }
    }


    public void cancelNotification() {
        if(notificationManager != null) {
            notificationManager.cancelNotify();
        }
    }

    public void resume() {
        if(streamUrl != null)
            play(streamUrl);
    }

    public void pause() {
        exoPlayer.setPlayWhenReady(false);
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
    }

    public void stop() {
        exoPlayer.stop();
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
        notificationManager.cancelNotify();
    }

    public void playOrPause(String url) {
        notificationManager.setMedia(FmConstants.fmname,FmConstants.fmimage,false);
        if(streamUrl != null && streamUrl.equals(url)){
            if(!isPlaying()){
                play(streamUrl);
            } else {
                pause();
            }
        } else {
            if(isPlaying()){
                pause();
            }
            play(url);
        }
    }

    public String getStatus(){
        return status;
    }

    public MediaSessionCompat getMediaSession(){
        return mediaSession;
    }

    public boolean isPlaying(){
        return this.status.equals(AppCons.PLAYING);
    }

    private void wifiLockRelease(){
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private String getUserAgent(){
        return Util.getUserAgent(this, getClass().getSimpleName());
    }



    private void makeNotification() {

        // Add Play button intent in notification.
        Intent playIntent = new Intent(this, RadioService.class);
        PendingIntent pendingPlayIntent;
        NotificationCompat.Action playAction = null;
        Log.i("Roney",status);
        if (status.equals(AppCons.PAUSED)) {
            playIntent.setAction(ACTION_PLAY);
            pendingPlayIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_MUTABLE);
            playAction = new NotificationCompat.Action(R.drawable.ic_play_white, "Play", pendingPlayIntent);
        } else if (status.equals(AppCons.PLAYING)) {
            playIntent.setAction(ACTION_PAUSE);
            pendingPlayIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_MUTABLE);
            playAction = new NotificationCompat.Action(R.drawable.ic_pause_white, "Pause", pendingPlayIntent);
        } else {
            // DO Nothing
        }

        Intent stopIntent = new Intent(this, RadioService.class);
        stopIntent.setAction(RadioService.ACTION_STOP);
        NotificationCompat.Action stopAction = null;
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_MUTABLE);
        stopAction = new NotificationCompat.Action(R.drawable.ic_stop_white, "Stop", stopPendingIntent);
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.norwayradio_small);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Norway FM Radio")
                .setContentText("Listening "+ FmConstants.fmname)
                .setSmallIcon(R.drawable.music1)
                .setLargeIcon(largeIconBitmap)
                .addAction(playAction)
                .addAction(stopAction)
                .build();

        // Start foreground service.
        try {
            startForeground(1,notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}