package com.newsparkapps.norwayfmradio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class MediaNotificationManager {

    public static final int NOTIFICATION_ID = 555;
    private final String PRIMARY_CHANNEL = "PRIMARY_CHANNEL_ID";
    private final String PRIMARY_CHANNEL_NAME = "PRIMARY";
    private RadioService service;
    private String strAppName,img;
    Bitmap bmp = null;
    private Resources resources;
    boolean isPlaying = false;
    private NotificationManagerCompat notificationManager;

    public MediaNotificationManager(RadioService service) {
        this.service = service;
        this.resources = service.getResources();
        strAppName = resources.getString(R.string.app_name);
        notificationManager = NotificationManagerCompat.from(service);

    }

    public void startNotify(String playbackStatus) {
        Log.i("Roney","startNotify "+playbackStatus);
        int icon = R.drawable.ic_pause_white;



        Intent playbackAction = new Intent(service, RadioService.class);
        PendingIntent action;
        if(playbackStatus.equals(AppCons.PAUSED)){
            icon = R.drawable.ic_play_white;
            playbackAction.setAction(RadioService.ACTION_PLAY);
            action = PendingIntent.getService(service, 2, playbackAction, PendingIntent.FLAG_MUTABLE);
        } else {
            playbackAction.setAction(RadioService.ACTION_PAUSE);
            action = PendingIntent.getService(service, 1, playbackAction, PendingIntent.FLAG_MUTABLE);
        }

        Intent stopIntent = new Intent(service, RadioService.class);
        stopIntent.setAction(RadioService.ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(service, 3, stopIntent, PendingIntent.FLAG_MUTABLE);

        Intent intent = new Intent(service, Detailed.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_MUTABLE);
        notificationManager.cancel(NOTIFICATION_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(PRIMARY_CHANNEL, PRIMARY_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            assert manager != null;
            manager.createNotificationChannel(channel);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        /*try {
            bmp = getBitmapFromURL(img);
        } catch (NetworkOnMainThreadException e) {
            e.printStackTrace();
            bmp = BitmapFactory.decodeResource(resources, R.drawable.fmradioindialogo_small);
        }*/
        bmp = BitmapFactory.decodeResource(resources, R.drawable.norwayradio_small);

        /*Bitmap bmp = null;
        try {
            bmp = Glide
                    .with(context)
                    .asBitmap()
                    .load(img)
                    .submit()
                    .get();
        } catch (NetworkOnMainThreadException e) {
            e.printStackTrace();
            bmp = BitmapFactory.decodeResource(resources, R.drawable.fmradioindialogo_small);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }*/


        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, PRIMARY_CHANNEL)
                .setAutoCancel(false)
                .setContentTitle("Norway FM Radio")
                .setContentText("Listening "+ FmConstants.fmname)
                .setLargeIcon(bmp)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.music2)
                .addAction(icon, "pause", action)
                .addAction(R.drawable.ic_stop_white, "stop", stopAction)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setStyle(new  androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)
                        .setShowCancelButton(true)
                        .setMediaSession(service.getMediaSession().getSessionToken())
                        .setCancelButtonIntent(stopAction));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            service.startForeground(NOTIFICATION_ID, builder.build());
        }


    }

    public void cancelNotify() {
        service.stopForeground(true);
    }

    public void setMedia(String name, String image,boolean playing) {
        strAppName = name;
        img = image;
        isPlaying = playing;
    }

    public Bitmap getBitmapFromURL(String strURL) {
        Bitmap myBitmap = null;
        try {
            URL url = new URL(strURL);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-length", "0");
            httpConnection.setUseCaches(false);
            httpConnection.setAllowUserInteraction(false);
            httpConnection.setConnectTimeout(100000);
            httpConnection.setReadTimeout(100000);
            httpConnection.setDoInput(true);
            httpConnection.connect();
            int responseCode = httpConnection.getResponseCode();
            Log.i("responseCode",responseCode+"");
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream input = httpConnection.getInputStream();
                myBitmap = BitmapFactory.decodeStream(input);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return myBitmap;
    }

}