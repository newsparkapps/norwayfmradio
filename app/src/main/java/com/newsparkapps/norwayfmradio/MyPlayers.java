package com.newsparkapps.norwayfmradio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MyPlayers extends AppCompatActivity {
    ImageView favorites, bgImage;
    RadioManager radioManager;
    private static final String TAG = "MyPlayers";
    ImageButton trigger;
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private SeekBar mediaVlmSeekBar = null;
    AudioManager audiomanager;
    Circle_image stationIcon;
    private DisplayImageOptions options;
    TextView stationName;
    RelativeLayout bglayout;
    DatabaseHandler db;
    String language;
    boolean addedornot;
    SharedPreferences.Editor editor;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myplayers);

        radioManager = RadioManager.with(this);
        stationName = findViewById(R.id.stationame);
        trigger = findViewById(R.id.playTrigger);
        stationIcon = findViewById(R.id.stationimage);
        bglayout = findViewById(R.id.bglayout);
        favorites = findViewById(R.id.favorites);
        bgImage = findViewById(R.id.bgImage);
        db = new DatabaseHandler(this);

        Intent a = getIntent();
        language = a.getStringExtra("language");


        startAdRequesting();

        try {
            if (AdmobUtils.getAdOnStatus(this).equals("zero")) {
                AdmobUtils.setAdOnStatus(this,"one");
            } else {
                AdmobUtils.loadInterstitialAd(this);
                AdmobUtils.setAdOnStatus(this,"zero");
            }
        } catch (NullPointerException e) {
            Log.i(TAG,"Exception "+e);
        }

        addedornot = db.getFavorite(FmConstants.fmname);
        if (addedornot) {
            favorites.setImageResource(R.drawable.ic_favorites_active);
        } else {
            favorites.setImageResource(R.drawable.ic_favorites);
        }


        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        initControls();

        try {
            mediaVlmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onStopTrackingTouch(SeekBar arg0) {
                }

                public void onStartTrackingTouch(SeekBar arg0) {
                }

                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            });
        } catch (Exception e) {
            Log.i(TAG,"Exception "+e);
        }

        setDatavalues();

        trigger.setOnClickListener(view -> {
            if (FmConstants.isPlaying) {
                radioManager.pausePlaying();
                FmConstants.isPlaying = false;
                setDatavalues();
            } else {
                radioManager.continuePlaying();
                FmConstants.isPlaying = true;
                setDatavalues();
            }
        });

        favorites.setOnClickListener(v -> {
            try {
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    addedornot = db.getFavorite(FmConstants.fmname);
                    if (addedornot) {
                        db.deleteMessage(new MyFourites(FmConstants.fmname, FmConstants.fmurl, FmConstants.fmimage));
                        favorites.setImageResource(R.drawable.ic_favorites);
                        Toast.makeText(getApplicationContext(), "Removed from favorite list", Toast.LENGTH_SHORT).show();
                    } else {
                        Utils.setFavoritesAnalytics(FmConstants.fmname, getApplicationContext());
                        db.addShoutcast(new MyFourites(FmConstants.fmname, FmConstants.fmurl, FmConstants.fmimage));
                        favorites.setImageResource(R.drawable.ic_favorites_active);
                        Toast.makeText(getApplicationContext(), "Added to favorite list", Toast.LENGTH_SHORT).show();
                    }
                }, 500);
            } catch (RuntimeException e) {
                Log.i(TAG,"Exception "+e);
            }
        });

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(getResources().getDrawable(R.drawable.norwayradio_small))
                .showImageForEmptyUri(getResources().getDrawable(R.drawable.norwayradio_small))
                .showImageOnFail(getResources().getDrawable(R.drawable.norwayradio_small))
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .build();

        imageLoader.displayImage(FmConstants.fmimage, stationIcon, options, null);
        stationName.setText(FmConstants.fmname);
        imageLoader.displayImage(FmConstants.fmimage, bgImage, options, null);
        Utils.setPlayerAnalytics(FmConstants.fmname, getApplicationContext());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle oldInstanceState) {
        super.onSaveInstanceState(oldInstanceState);
        oldInstanceState.clear();
    }

    private void startAdRequesting() {
        FrameLayout adContainer = findViewById(R.id.ad_view_container);
        AdView adView1 = AdmobUtils.createAdView(this);
        adContainer.addView(adView1);
        AdmobUtils.loadBannerAd(this, adView1);


        FrameLayout adSquareContainer1 = findViewById(R.id.ad_view_container2);
        AdView adView2 = AdmobUtils.createSmallSquareAdView(this);
        adSquareContainer1.addView(adView2);
        AdmobUtils.loadBannerAd(this, adView2);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Intent a = getIntent();
        language = a.getStringExtra("language");
        setDatavalues();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }



    @Override
    public void onResume() {
        super.onResume();
    }

    @Subscribe
    public void onEvent(String status) {
        switch (status) {
            case PlaybackStatus.LOADING:
                // Do Nothing
                break;

            case PlaybackStatus.ERROR:
                Toast.makeText(getApplicationContext(), "Station not available", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void initControls() {
        audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaVlmSeekBar = findViewById(R.id.seekBar);
        mediaVlmSeekBar.setMax(audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        mediaVlmSeekBar.setProgress(audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (AdmobUtils.isInterstitialAdLoaded()) {
                    AdmobUtils.showInterstitialAd(this,Detailed.class);
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setDatavalues() {
        if (FmConstants.isPlaying) {
            trigger.setImageResource(R.drawable.ic_pause);
        } else {
            trigger.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * Called when leaving the activity
     */
    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        imageLoader = null;
        radioManager = null;
        trigger = null;
        favorites = null;
        bgImage = null;
        mediaVlmSeekBar = null;
        audiomanager = null;
        stationIcon = null;
        options = null;
        stationName = null;
        bglayout = null;
        db = null;
        language = null;
        addedornot = false;
        editor = null;
    }
}
