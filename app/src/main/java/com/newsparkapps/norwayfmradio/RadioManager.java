package com.newsparkapps.norwayfmradio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class RadioManager {

    private static final String TAG = "RadioManager";
    public static final String ACTION_INIT = "ACTION_INIT";
    private static RadioManager instance;

    private final Context appContext;
    private RadioServices service;
    private Station currentStation;
    private boolean isBound = false;

    // 🔹 Single source of truth (pending until service is ready)
    private Station pendingStation;

    /* -------------------- SINGLETON -------------------- */

    private RadioManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized RadioManager with(Context context) {
        if (instance == null) {
            instance = new RadioManager(context);
        }
        return instance;
    }

    /* -------------------- SERVICE CONNECTION -------------------- */

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((RadioServices.LocalBinder) binder).getService();
            isBound = true;
            Log.d(TAG, "Service connected");

            // ▶️ Play pending station after bind (cold start fix)
            if (pendingStation != null) {
                service.play(pendingStation);
                pendingStation = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            service = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    /* -------------------- LIFECYCLE -------------------- */

    public void startAndBind() {
        Intent intent = new Intent(appContext, RadioServices.class);
        intent.setAction(RadioServices.ACTION_INIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(appContext, intent);
        } else {
            appContext.startService(intent);
        }

        if (!isBound) {
            appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbind() {
        if (isBound) {
            try {
                appContext.unbindService(serviceConnection);
            } catch (IllegalArgumentException ignored) {
            }
            isBound = false;
        }
    }

    /* -------------------- PLAYBACK API -------------------- */

    public void play(String name, String image, String url) {
        Station station = new Station(name, image, url);

        if (service != null) {
            service.play(station);
        } else {
            pendingStation = station;
            startAndBind();
        }
    }

    public void pause() {
        if (service != null) {
            FmConstants.isPlaying = false;
            service.pause();
        }
    }

    public void stop() {
        if (service != null) {
            service.stopPlayback();
        }
    }

    public boolean isPlaying() {
        return service != null && service.isPlaying();
    }

    /* -------------------- TOGGLE (FIXED) -------------------- */

    public void toggle() {
        try {
            if (service == null) {
                if (pendingStation != null) {
                    startAndBind();
                } else {
                    service.play(pendingStation);
                }
                return;
            }
            service.toggle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Station getCurrentStation() {
        return currentStation;
    }
}
