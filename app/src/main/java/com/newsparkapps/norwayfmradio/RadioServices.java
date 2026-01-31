package com.newsparkapps.norwayfmradio;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;

import org.greenrobot.eventbus.EventBus;

public class RadioServices extends Service implements Player.Listener {
    private static final String TAG = "RadioService";
    public static final String ACTION_INIT = "ACTION_INIT";
    public static final int NOTIFICATION_ID = 556;
    public static final String CHANNEL_ID = "radio_channel";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";
    private final IBinder binder = new LocalBinder();
    private ExoPlayer exoPlayer;
    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;
    private WifiManager.WifiLock wifiLock;
    private Station currentStation;
    private Bitmap currentLargeIcon;
    boolean userWantsPlayback = false;

    ImageLoader imageLoader;
    public void pause() {
        exoPlayer.pause();
    }
    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }
    public void toggle() {
         Log.i("TAG","toggle");
         if (exoPlayer.isPlaying()) {
             exoPlayer.pause();
         } else {
             exoPlayer.play();
         }
    }

    /* ===================== BINDER ===================== */

    public class LocalBinder extends Binder {
        public RadioServices getService() {
            return RadioServices.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /* ===================== SERVICE LIFECYCLE ===================== */

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        acquireWifiLock();
        initMediaSession();
        initPlayer();
        // Foreground must start immediately
        try {
            startForeground(
                    NOTIFICATION_ID,
                    buildNotification(false, null)
            );
        }  catch (ForegroundServiceStartNotAllowedException e) {
            e.printStackTrace();
        }
        imageLoader = MyApp.getInstance().getImageLoader();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_PLAY_PAUSE:
                handlePlayPause();
                break;
            case ACTION_STOP:
                stopPlayback();
                break;
        }
        return START_STICKY;
    }

    /* ===================== PLAY / PAUSE ===================== */

    private void handlePlayPause() {
        if (exoPlayer == null) return;
        if (exoPlayer.isPlaying()) {
            userWantsPlayback = false;
            exoPlayer.pause();
        } else if (currentStation != null) {
            userWantsPlayback = true;
            exoPlayer.play();
        }
        updatePlaybackState(exoPlayer.isPlaying());
        updateNotification();
        postStationEvent(); // ✅ keep UI in sync
    }

    /* ===================== PLAYER INIT ===================== */

    private void initPlayer() {
        AudioAttributes attrs =
                new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build();

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setAudioAttributes(attrs, true);
        exoPlayer.setHandleAudioBecomingNoisy(true);
        exoPlayer.addListener(this);
    }

    /* ===================== PLAY STATION ===================== */

    public void play(Station station) {
        try {
            if (station == null || station.url == null || station.url.isEmpty()) {
                Log.e(TAG, "Invalid station");
                return;
            }
            currentStation = station;
            userWantsPlayback = true;
            MediaItem mediaItem =
                    new MediaItem.Builder()
                            .setUri(station.url)
                            .build();

            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
            updatePlaybackState(true);
            updateNotification();
            postStationEvent();
            if (station.image != null && !station.image.isEmpty()) {
                loadStationImageAndUpdateNotification(station.image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===================== EXOPLAYER CALLBACKS ===================== */
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        updatePlaybackState(isPlaying);
        updateNotification();
        postStationEvent();
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        updateNotification();
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        Log.e(TAG, "Player error", error);
    }

    /* ===================== MEDIA SESSION ===================== */
    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                if (exoPlayer != null) {
                    userWantsPlayback = true;
                    exoPlayer.play();
                }
            }

            @Override
            public void onPause() {
                if (exoPlayer != null) {
                    userWantsPlayback = false;
                    exoPlayer.pause();
                }
            }

            @Override
            public void onStop() {
                stopPlayback();
            }
        });
        mediaSession.setActive(true);
    }

    private void updatePlaybackState(boolean isPlaying) {
        PlaybackStateCompat state =
                new PlaybackStateCompat.Builder()
                        .setActions(
                                PlaybackStateCompat.ACTION_PLAY |
                                        PlaybackStateCompat.ACTION_PAUSE |
                                        PlaybackStateCompat.ACTION_STOP
                        )
                        .setState(
                                isPlaying
                                        ? PlaybackStateCompat.STATE_PLAYING
                                        : PlaybackStateCompat.STATE_PAUSED,
                                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                                1f
                        )
                        .build();
        mediaSession.setPlaybackState(state);
    }

    /* ===================== EVENTBUS ===================== */

    private void postStationEvent() {
        if (currentStation == null) return;
        // 🔥 Remove old sticky to avoid stale UI
        EventBus.getDefault()
                .removeStickyEvent(StationChangedEvent.class);
        EventBus.getDefault().postSticky(
                new StationChangedEvent(
                        currentStation,
                        exoPlayer != null && exoPlayer.isPlaying()
                )
        );
        Log.d("EVENTBUS", "Posted station: " + currentStation.name);
    }

    /* ===================== NOTIFICATION ===================== */

    private Notification buildNotification(
            boolean isPlaying,
            Bitmap largeIcon
    ) {

        PendingIntent playPauseIntent =
                PendingIntent.getService(
                        this,
                        0,
                        new Intent(this, RadioServices.class)
                                .setAction(ACTION_PLAY_PAUSE),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        PendingIntent stopIntent =
                PendingIntent.getService(
                        this,
                        1,
                        new Intent(this, RadioServices.class)
                                .setAction(ACTION_STOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        String title =
                currentStation != null
                        ? currentStation.name
                        : "Bollywood FM Radio";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(isPlaying ? "Playing" : "Paused")
                .setSmallIcon(R.drawable.norway_fm_radio_logo)
                .setLargeIcon(
                        largeIcon != null
                                ? largeIcon
                                : BitmapFactory.decodeResource(
                                getResources(),
                                R.drawable.norway_fm_radio_logo
                        )
                )
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                        isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying ? "Pause" : "Play",
                        playPauseIntent
                )
                .addAction(
                        R.drawable.ic_stop,
                        "Stop",
                        stopIntent
                )
                .setStyle(
                        new androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0, 1)
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        try {
            notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                            exoPlayer != null && exoPlayer.isPlaying(),
                            currentLargeIcon
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===================== STATION IMAGE ===================== */

    private void loadStationImageAndUpdateNotification(String imageUrl) {
        imageLoader.get(imageUrl, new ImageLoader.ImageListener() {

            @Override
            public void onResponse(
                    ImageLoader.ImageContainer response,
                    boolean isImmediate
            ) {
                if (response.getBitmap() != null) {
                    currentLargeIcon = response.getBitmap();
                    updateNotification();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Image load failed", error);
            }
        });
    }

    /* ===================== STOP ===================== */

    public void stopPlayback() {

        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
        }

        stopForeground(true);
        stopSelf();
    }

    /* ===================== WIFI LOCK ===================== */

    private void acquireWifiLock() {

        WifiManager wm =
                (WifiManager) getApplicationContext()
                        .getSystemService(WIFI_SERVICE);

        if (wm != null) {
            wifiLock =
                    wm.createWifiLock(
                            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                            "radio_wifi_lock"
                    );
            wifiLock.acquire();
        }
    }

    /* ===================== NOTIFICATION CHANNEL ===================== */

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Radio Playback",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /* ===================== CLEANUP ===================== */

    @Override
    public void onDestroy() {

        if (exoPlayer != null) {
            exoPlayer.release();
        }

        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }

        mediaSession.release();
        super.onDestroy();
    }
}
