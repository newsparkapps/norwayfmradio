package com.newsparkapps.norwayfmradio;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FavoritesRadio extends BaseActivity {
    LinearLayout subPlayer;
    ImageButton trigger;
    private TextView subPlayerName;
    private Station pendingStation;
    private ImageLoader imageLoader;
    private NetworkImageView subPlayerImage;
    CustomAdapter adapterMusic;
    DatabaseHandler db;
    private RadioManager radioManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favoritesfm);

        setupToolbar();
        List<Shoutcast> shoutcasts = new ArrayList<Shoutcast>();
        subPlayer = findViewById(R.id.sub_player);
        trigger = findViewById(R.id.playTrigger);
        subPlayerName = findViewById(R.id.subplayername);
        subPlayerImage = findViewById(R.id.subplayerimage);
        RecyclerView radiorecyclerView =  findViewById(R.id.recycler_view);
        radiorecyclerView.setHasFixedSize(true);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());

        radioManager = RadioManager.with(getApplicationContext());
        imageLoader = MyApp.getInstance().getImageLoader();

        Utils.setFMAnalytics("Bollywood Favourites", this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        radiorecyclerView.setLayoutManager(linearLayoutManager);

        int itemWidth = Utils.isTablet(this) ? 180 : 120;
        int columns = calculateNoOfColumns(itemWidth);

        radiorecyclerView.setLayoutManager(new GridLayoutManager(this, columns));
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());
        radiorecyclerView.addItemDecoration(new GridSpacingItemDecoration(columns, 8, true));
        radiorecyclerView.setHasFixedSize(true);

        setupAds();

        trigger.setOnClickListener(v -> togglePlayback());

        db = new DatabaseHandler(this);
        List<Shoutcast> myFourites = db.getAllFourites();
        for (Shoutcast cn : myFourites)
        {
            Shoutcast items = new Shoutcast(cn.getName(),cn.getUrl(),cn.getImage());
            shoutcasts.add(items);
        }

        adapterMusic = new CustomAdapter(this, shoutcasts,"Detailed");
        radiorecyclerView.setAdapter(adapterMusic);
        radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
        updateSubPlayer();

        subPlayer.setOnClickListener(v ->
                startActivity(new Intent(this, MyPlayers.class)));
    }

    private int calculateNoOfColumns(int itemWidthDp) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float screenWidthDp = dm.widthPixels / dm.density;
        return Math.max(2, (int) (screenWidthDp / itemWidthDp));
    }
    private void togglePlayback() {
        radioManager.toggle();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        View rootView = findViewById(R.id.root_view);
        Utils.enableEdgeToEdge(this, rootView);
        Objects.requireNonNull(getSupportActionBar()).setTitle("My Favourites");
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onStationChanged(StationChangedEvent event) {
        pendingStation = event.station;
        Station station = event.station;
        if (event.isPlaying) {
            trigger.setImageResource(R.drawable.ic_pause);
        } else {
            trigger.setImageResource(R.drawable.ic_play);
        }
        if (pendingStation != null) {
            subPlayerImage.setImageUrl(pendingStation.image, imageLoader);
            if (station.image != null && !station.image.isEmpty()) {
                subPlayerImage.setImageUrl(
                        station.image,
                        MyApp.getInstance().getImageLoader()
                );
            }
            subPlayerName.setText(station.name);
        } else {
            subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);
        }
        subPlayer.setVisibility(View.VISIBLE);
    }



    private void setupAds() {
        FrameLayout adContainer = findViewById(R.id.ad_view_container);
        setupBanner(adContainer);
    }

    @Subscribe
    public void onEvent(String status) {
        Log.i("BollywoodFM","onEvent");
        if (PlaybackStatus.ERROR.equals(status)) {
            Toast.makeText(this, "Station not available", Toast.LENGTH_SHORT).show();
        }
        updateSubPlayer();
    }

    private void updateSubPlayer() {
        if (FmConstants.isPlaying) {
            trigger.setImageResource(R.drawable.ic_pause);
        } else {
            trigger.setImageResource(R.drawable.ic_play);
        }
        if (pendingStation != null) {
            subPlayerName.setText(pendingStation.name);
            subPlayerImage.setImageUrl(pendingStation.image, imageLoader);
        } else {
            subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);
        }
        subPlayer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSubPlayer();
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
