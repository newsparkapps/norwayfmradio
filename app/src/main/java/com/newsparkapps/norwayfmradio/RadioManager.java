package com.newsparkapps.norwayfmradio;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.EventBus;

public class RadioManager {
    private static RadioManager instance = null;
    private static RadioService service;
    private String TAG = "RadioManager";
    private Context context;
    private boolean serviceBound;

    private RadioManager(Context context) {
        Log.i(TAG,"RadioManager Constructor ");
        this.context = context;
        serviceBound = false;
    }

    public static RadioManager with(Context context) {
        if (instance == null)
            instance = new RadioManager(context);
        return instance;
    }

    public static RadioService getService() {
        return service;
    }

    public void playOrPause(String name, String image, String streamUrl) {
        Log.i(TAG,"playOrPause "+name+ " "+image+" "+streamUrl);
        try {
            //service.cancelNotification();
            service.playOrPause(streamUrl);
            FmConstants.fmimage = image;
            FmConstants.fmname = name;
            FmConstants.fmurl = streamUrl;
            FmConstants.isPlaying = true;
            service.setMedia(FmConstants.fmname, FmConstants.fmurl, false);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying() {
        Log.i(TAG,"isPlaying ");
        return service.isPlaying();
    }

    public void bind() {
        try {
            Log.i(TAG,"bind ");
            Intent intent = new Intent(context, RadioService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            if (service != null)
                EventBus.getDefault().post(service.getStatus());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void pausePlaying() {
        Log.i(TAG,"pausePlaying ");
        try {
            if (service != null) {
                service.pause();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void continuePlaying() {
        Log.i(TAG,"continuePlaying ");
        try {
            if (service != null) {
                service.play(FmConstants.fmurl);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void unbind() {
        Log.i(TAG,"unbind ");
        try {
            context.unbindService(serviceConnection);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.i(TAG,"onServiceConnected ");
            service = ((RadioService.LocalBinder) binder).getService();
            serviceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG,"onServiceDisconnected ");
            serviceBound = false;
        }
    };

}
