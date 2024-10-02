package com.newsparkapps.norwayfmradio;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

public class RadioManager {
    @SuppressLint("StaticFieldLeak")
    private static RadioManager instance = null;
    private static MyForeGroundServices myForeGroundServices;
    private final String TAG = "RadioManager";
    private final Context context;
    boolean serviceBound;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.i(TAG, "onServiceConnected ");
            try {
                myForeGroundServices = ((MyForeGroundServices.LocalBinders) binder).getService();
                serviceBound = true;
            } catch (ClassCastException e) {
                Log.i(TAG, "onServiceConnected ClassCastException "+e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "onServiceDisconnected ");
            serviceBound = false;
        }
    };

    public static RadioManager with(Context context) {
        if (instance == null)
            instance = new RadioManager(context);
        return instance;
    }

    public static MyForeGroundServices getService() {
        return myForeGroundServices;
    }


    public void playOrPause(String name, String image, String streamUrl) {
        Log.i(TAG,"playOrPause "+name+ " "+image+" "+streamUrl);
        try {
            myForeGroundServices.playOrPause(streamUrl);
            FmConstants.fmimage = image;
            FmConstants.fmname = name;
            FmConstants.fmurl = streamUrl;
            FmConstants.isPlaying = true;
            myForeGroundServices.setMedia(FmConstants.fmname, FmConstants.fmurl, false);
        } catch (NullPointerException e) {
            Log.i(TAG, "Exception "+e);
        }
    }

    public boolean isPlaying() {
        Log.i(TAG, "isPlaying ");
        return myForeGroundServices.isPlaying();
    }

    private RadioManager(Context context) {
        Log.i(TAG, "RadioManager Constructor ");
        this.context = context;
        serviceBound = false;
    }

    public void pausePlaying() {
        Log.i(TAG, "pausePlaying ");
        try {
            if (myForeGroundServices != null) {
                myForeGroundServices.pause();
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "Exception "+e);
        }
    }

    public void stopPlaying() {
        Log.i(TAG, "stopPlaying ");
        try {
            if (myForeGroundServices != null) {
                myForeGroundServices.stop();
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "Exception "+e);
        }
    }

    public void bind() {
        try {
            Log.i(TAG, "bind ");
            Intent serviceIntent = new Intent(context, MyForeGroundServices.class);
            serviceIntent.setAction(MyForeGroundServices.ACTION_INIT);
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            if (myForeGroundServices != null)
                EventBus.getDefault().post(myForeGroundServices.getStatus());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

        } catch (NullPointerException e) {
            Log.i(TAG, "Exception "+e);
        }
    }

    public void continuePlaying() {
        Log.i(TAG, "continuePlaying ");
        try {
            if (myForeGroundServices != null) {
                myForeGroundServices.play(FmConstants.fmurl);
            }
        } catch (NullPointerException e) {
            Log.i(TAG, "Exception "+e);
        }
    }

    public void unbind() {
        Log.i(TAG, "unbind ");
        try {
            stopPlaying();
            context.unbindService(serviceConnection);
            myForeGroundServices.stopForegroundService();
        } catch (RuntimeException e) {
            Log.i(TAG, "Exception "+e);
        }
    }

}
