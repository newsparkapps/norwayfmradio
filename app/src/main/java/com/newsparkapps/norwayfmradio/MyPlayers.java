package com.newsparkapps.norwayfmradio;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

public class MyPlayers extends BaseActivity {
    private static final String TAG = "MyPlayers";
    private ImageView favorites, bgImage;
    private ImageButton trigger;
    private TextView subPlayerName;
    private NetworkImageView subPlayerImage;
    ImageLoader imageLoader;
    private Bitmap stationImageBitmap;
    private TextView stationName;
    private SeekBar volumeSeekBar;
    private Station currentStation;
    private Circle_image stationIcon;
    private RadioManager radioManager;
    private AudioManager audioManager;
    private DatabaseHandler db;

    private boolean isFavorite;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myplayers);

        radioManager = RadioManager.with(getApplicationContext());
        db = new DatabaseHandler(this);

        setupToolbar();
        initViews();
        setupAudioControls();
        setupAds();
        setupClickListeners();


        imageLoader = MyApp.getInstance().getImageLoader();
    }

    /* -------------------- UI SETUP -------------------- */

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        View rootView = findViewById(R.id.bglayout);
        Utils.enableEdgeToEdge(this, rootView);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);
    }

    private void initViews() {
        trigger = findViewById(R.id.playTrigger);
        favorites = findViewById(R.id.favorites);
        bgImage = findViewById(R.id.bgImage);
        stationIcon = findViewById(R.id.stationimage);
        stationName = findViewById(R.id.stationame);
        volumeSeekBar = findViewById(R.id.seekBar);
        subPlayerName = findViewById(R.id.subplayername);
        subPlayerImage = findViewById(R.id.subplayerimage);
    }

    /* -------------------- AUDIO -------------------- */

    private void setupAudioControls() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        volumeSeekBar.setMax(
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        );
        volumeSeekBar.setProgress(
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        );

        volumeSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC, progress, 0
                            );
                        }
                    }
                }
        );
    }

    /* -------------------- ADS -------------------- */

    private void setupAds() {
        try {
            FrameLayout adContainer = findViewById(R.id.ad_view_container);
            setupBanner(adContainer);

            if ("one".equals(AdmobUtils.getAdOnStatus(this))) {
                AdmobUtils.loadInterstitialAd(this);
                AdmobUtils.setAdOnStatus(this, "zero");
            } else {
                AdmobUtils.setAdOnStatus(this, "one");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStationImage(String imageUrl) {
        try {
            imageLoader.get(imageUrl, new ImageLoader.ImageListener() {

                @Override
                public void onResponse(
                        ImageLoader.ImageContainer response,
                        boolean isImmediate
                ) {
                    if (response.getBitmap() != null) {
                        stationImageBitmap = response.getBitmap();
                        stationIcon.setImageBitmap(stationImageBitmap);
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Image load failed", error);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* -------------------- CLICK HANDLERS -------------------- */

    private void setupClickListeners() {
        trigger.setOnClickListener(v -> togglePlayback());
        favorites.setOnClickListener(v -> toggleFavorite());
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (AdmobUtils.isInterstitialAdLoaded()) {
                            AdmobUtils.showInterstitialAd(MyPlayers.this, Dashboard.class);
                        } else {
                            Intent a  = new Intent(MyPlayers.this, Dashboard.class);
                            startActivity(a);
                        }
                    }
                });
    }

    /* -------------------- PLAYER -------------------- */

    private void togglePlayback() {
        radioManager.toggle();
        updatePlayButton();
    }

    private void updatePlayButton() {
        trigger.setImageResource(
                FmConstants.isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
        );
    }

    /* -------------------- FAVORITES -------------------- */

    private void toggleFavorite() {
        try {
            isFavorite = db.getFavorite(currentStation.name);
            if (isFavorite) {
                db.deleteMessage(
                        new MyFourites(currentStation.name, currentStation.url, currentStation.image)
                );
                favorites.setImageResource(R.drawable.ic_favorites);
                Toast.makeText(this, "Removed from favorite list", Toast.LENGTH_SHORT).show();
            } else {
                Utils.setFMAnalytics("Fav_"+currentStation.name, getApplicationContext());
                db.addShoutcast(
                        new MyFourites(currentStation.name, currentStation.url, currentStation.image)
                );
                favorites.setImageResource(R.drawable.ic_favorites_active);
                Toast.makeText(this, "Added to favorite list", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* -------------------- UI UPDATE -------------------- */

    private void updateUI() {
        updatePlayButton();
        if (currentStation != null) {
            stationName.setText(currentStation.name);
            stationIcon.setImageBitmap(stationImageBitmap);
            bgImage.setImageResource(R.drawable.norway_fm_radio_logo);
            isFavorite = db.getFavorite(currentStation.name);
            Utils.setFMAnalytics("Player_"+currentStation.name, getApplicationContext());
        }
        favorites.setImageResource(
                isFavorite ? R.drawable.ic_favorites_active : R.drawable.ic_favorites
        );
    }

    /* -------------------- LIFECYCLE -------------------- */

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        radioManager.startAndBind();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        radioManager.unbind();
        super.onStop();
    }

    @Subscribe
    public void onEvent(String status) {
        if (PlaybackStatus.ERROR.equals(status)) {
            Toast.makeText(this, "Station not available", Toast.LENGTH_SHORT).show();
        }
        updateUI();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onStationChanged(StationChangedEvent event) {
        Station station = event.station;
        currentStation = event.station;
        stationName.setText(station.name);
        loadStationImage(station.image);
        stationIcon.setImageBitmap(stationImageBitmap);
        bgImage.setImageResource(R.drawable.norway_fm_radio_logo);
        trigger.setImageResource(
                event.isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
        );

        subPlayerName.setText(currentStation.name);

        subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);

        if (station.image != null && !station.image.isEmpty()) {
            subPlayerImage.setImageUrl(
                    currentStation.image,
                    imageLoader
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.home) {
            Intent a = new Intent(this, Dashboard.class);
            startActivity(a);
        }
        return super.onOptionsItemSelected(item);
    }
}
