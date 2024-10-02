package com.newsparkapps.norwayfmradio;

import static com.newsparkapps.norwayfmradio.FmConstants.APP_NAME;
import static com.newsparkapps.norwayfmradio.MyApplication.CHANNEL_ID;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.Objects;


public class MyForeGroundServices extends Service implements Player.Listener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    private static final String ACTION_PLAY_PAUSE = "com.newsparkapps.norwayfmradio.PLAY_PAUSE";
    private static final String ACTION_STOP = "com.newsparkapps.norwayfmradio.STOP";
    private final IBinder iBinder = new LocalBinders();
    private String status;
    private final String TAG = "MyForeGroundServices";
    private String strAppName;
    private String streamUrl;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private ExoPlayer exoPlayer;
    public static final int NOTIFICATION_ID = 556;
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    private AudioManager audioManager;
    private boolean isPlaying = false;
    private NotificationManagerCompat notificationManager;
    private WifiManager.WifiLock wifiLock;
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_INIT = "ACTION_INIT";

    public MyForeGroundServices() {
    }

    public class LocalBinders extends Binder {
        public MyForeGroundServices getService() {
            return MyForeGroundServices.this;
        }
    }

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "BroadcastReceiver becomingNoisyReceiver");
            pause();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    private final MediaSessionCompat.Callback mediasSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            Log.i(TAG, "MediaSessionCompat.Callback mediasSessionCallback onPlay");
            resume();
        }

        @Override
        public void onPause() {
            super.onPause();
            Log.i(TAG, "MediaSessionCompat.Callback mediasSessionCallback onPause");
            pause();
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.i(TAG, "MediaSessionCompat.Callback mediasSessionCallback onStop");
            stop();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "MyForeGroundServices onCreate");
        notificationManager = NotificationManagerCompat.from(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG_FOREGROUND_SERVICE, "My foreground service onCreate().");
        strAppName = getResources().getString(R.string.app_name);

        wifiLock = ((WifiManager) Objects.requireNonNull(getApplicationContext().getSystemService(Context.WIFI_SERVICE)))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock");
        try {
            startForeground(1, createNotification());
        } catch (ForegroundServiceStartNotAllowedException e) {
            Log.i(TAG, "onCreate ForegroundServiceStartNotAllowedException "+e);
        }

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
            Log.i(TAG, "mediaSession NullPointerException "+e);
        }

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.i(TAG, "exoPlayer onPlaybackStateChanged "+playbackState);
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    isPlaying = false;
                } else if (playbackState == Player.STATE_READY && exoPlayer.getPlayWhenReady()) {
                    isPlaying = true;
                }
            }
        });
        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        status = FmConstants.IDLE;

        mediaSession = new MediaSessionCompat(this, "RadioService");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.i(TAG, "notificationReceiver registerReceiver ");
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG,"notificationReceiver "+action);
            if (ACTION_PLAY_PAUSE.equals(action)) {
                Log.i(TAG,"notificationReceiver ACTION_PLAY_PAUSE");
                pauseOrResumeRadio();
            } else if (ACTION_STOP.equals(action)) {
                Log.i(TAG,"notificationReceiver ACTION_STOP");
                stopRadio();
                stopSelf();
                stop();
            }
        }
    };

    public void pauseOrResumeRadio() {
        Log.i(TAG,"pauseOrResumeRadio");
        if (isPlaying) {
            exoPlayer.pause();
        } else {
            exoPlayer.play();
        }
        isPlaying = !isPlaying;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateNotification();
        }
    }

    public void stopRadio() {
        Log.i(TAG,"stopRadio");
        exoPlayer.stop();
        isPlaying = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateNotification();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    private void updateNotification() {
        Log.i(TAG,"updateNotification");
        NotificationCompat.Builder builder = buildNotification();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
        try {
            Log.i(TAG, "startForeground");
            startForeground(NOTIFICATION_ID, notification);
        } catch (ForegroundServiceStartNotAllowedException e) {
            Log.i(TAG, "updateNotification ForegroundServiceStartNotAllowedException "+e);
        }
    }

    private NotificationCompat.Builder buildNotification() {
        // Play/Pause action
        PendingIntent playPauseIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Stop action
        PendingIntent stopIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openAppIntent = new Intent(this, Detailed.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseText = isPlaying ? "Pause" : "Play";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(APP_NAME)
                .setContentText(isPlaying ? "Streaming" : "Paused")
                .setSmallIcon(R.drawable.norwayradio_small)
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(playPauseIcon, playPauseText, playPauseIntent)
                .addAction(R.drawable.ic_stop, "Stop", stopIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle());
    }

    public String getStatus() {
        Log.i(TAG, "getStatus");
        return status;
    }

    @RequiresApi(api = 31)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand");
        try {
            if (intent != null) {
                String action = intent.getAction();
                switch (Objects.requireNonNull(action)) {
                    case ACTION_START_FOREGROUND_SERVICE:
                        Log.i(TAG,"onStartCommand ACTION_START_FOREGROUND_SERVICE");
                        break;
                    case ACTION_STOP_FOREGROUND_SERVICE:
                        Log.i(TAG,"onStartCommand ACTION_STOP_FOREGROUND_SERVICE");
                        stopForegroundService();
                        break;
                    case ACTION_PLAY:
                        Log.i(TAG,"onStartCommand ACTION_PLAY");
                        transportControls.play();
                        status = FmConstants.PLAYING;
                        updateNotification();
                        playOrPause(FmConstants.fmurl);
                        break;
                    case ACTION_PAUSE:
                        Log.i(TAG,"onStartCommand ACTION_PAUSE");
                        transportControls.pause();
                        updateNotification();
                        status = FmConstants.PAUSED;
                        break;
                    case ACTION_STOP:
                        Log.i(TAG,"onStartCommand ACTION_STOP");
                        Log.i(TAG, "getStatus");
                        stopForegroundService();
                        transportControls.stop();
                        status = FmConstants.STOPPED;
                        break;
                    case ACTION_INIT:
                        Log.i(TAG,"onStartCommand ACTION_INIT");
                        break;
                }
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "onStartCommand NullPointerException "+e);
        }
        return START_STICKY;
    }

    public void stopForegroundService() {
        Log.i(TAG,"stopForegroundService");
        stopForeground(true);
        stopSelf();
    }

    public void playOrPause(String url) {
        Log.i(TAG,"playOrPause "+url);
        setMedia(FmConstants.fmname, FmConstants.fmimage, false);
        if (streamUrl != null && streamUrl.equals(url)) {
            if (!isPlaying()) {
                play(streamUrl);
            } else {
                pause();
            }
        } else {
            if (isPlaying()) {
                pause();
            }
            play(url);
        }
    }

    public void play(String streamUrl) {
        Log.i(TAG,"play "+streamUrl);
        this.streamUrl = streamUrl;

        if (streamUrl.contains("m3u8")) {
            DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this,
                    Util.getUserAgent(this, "radioApp"));
            HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streamUrl));

            exoPlayer.setPlayWhenReady(true);
            exoPlayer.prepare(hlsMediaSource);
            exoPlayer.play();
            isPlaying = true;
            FmConstants.isPlaying = true;
        } else {

            MediaItem mediaItem = MediaItem.fromUri(streamUrl);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
            isPlaying = true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateNotification();
        }
    }


    public boolean isPlaying() {
        return this.status.equals(FmConstants.PLAYING);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.i(TAG,"onAudioFocusChange "+focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                exoPlayer.setVolume(0.8f);
                resume();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                exoPlayer.setVolume(0.1f);
                break;
        }
    }

    public void setMedia(String name, String image, boolean playing) {
        Log.i(TAG,"setMedia "+name+" "+playing);
        try {
            strAppName = name;
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
            Log.i(TAG, "setMedia NullPointerException "+e);
        }
    }

    public void resume() {
        Log.i(TAG,"resume ");
        if(streamUrl != null)
            play(streamUrl);
    }

    public void pause() {
        Log.i(TAG,"pause");
        exoPlayer.setPlayWhenReady(false);
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
    }

    public void stop() {
        Log.i(TAG,"stop");
        exoPlayer.stop();
        audioManager.abandonAudioFocus(this);
        wifiLockRelease();
        stopForegroundService();
    }

    private void wifiLockRelease() {
        Log.i(TAG, "wifiLockRelease");
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private Notification createNotification() {
        // Create a notification channel (required for Android 8.0 Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Radio Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Online FM Radio")
                .setAutoCancel(true)
                .setOngoing(true);

        return builder.build();
    }
}