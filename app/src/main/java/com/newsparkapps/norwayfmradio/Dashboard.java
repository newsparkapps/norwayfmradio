package com.newsparkapps.norwayfmradio;

import static android.view.View.VISIBLE;
import static com.newsparkapps.norwayfmradio.FmConstants.APP_URL;
import static com.newsparkapps.norwayfmradio.FmConstants.FM_JSON_URL;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.newsparkapps.norwayfmradio.util.Shoutcast;
import com.newsparkapps.norwayfmradio.util.ShoutcastHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class Dashboard extends BaseActivity {
    private RecyclerView radioRecyclerView;
    private CustomAdapter adapterMusic;
    private ShimmerFrameLayout shimmerFrameLayout;
    DatabaseHandler db = new DatabaseHandler(this);
    private LinearLayout subPlayer;
    private ImageButton trigger;
    private TextView subPlayerName;
    private NetworkImageView subPlayerImage;
    private RadioManager radioManager;
    private ImageLoader imageLoader;
    private Station pendingStation;
    private final ArrayList<Shoutcast> fmList = new ArrayList<>();
    private boolean isDataLoaded = false;
    private boolean doubleBackToExitPressedOnce = false;
    private static final int PERMISSION_REQUEST_CODE = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed);

        setupToolbar();
        initViews();
        setupRecyclerView();
        setupAds();
        setupClickListeners();

        radioManager = RadioManager.with(getApplicationContext());
        imageLoader = MyApp.getInstance().getImageLoader();

        requestNotificationPermission();
        Utils.setFMAnalytics("Bollywood_FM_", this);
    }

    /* -------------------- UI SETUP -------------------- */

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        View rootView = findViewById(R.id.root_view);
        Utils.enableEdgeToEdge(this, rootView);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);
    }

    private void initViews() {
        shimmerFrameLayout = findViewById(R.id.shimmerFrameLayout);
        radioRecyclerView = findViewById(R.id.recycler_view);
        subPlayer = findViewById(R.id.sub_player);
        trigger = findViewById(R.id.playTrigger);
        subPlayerName = findViewById(R.id.subplayername);
        subPlayerImage = findViewById(R.id.subplayerimage);
    }

    private void setupRecyclerView() {
        int itemWidth = Utils.isTablet(this) ? 180 : 120;
        int columns = calculateNoOfColumns(itemWidth);

        radioRecyclerView.setLayoutManager(new GridLayoutManager(this, columns));
        radioRecyclerView.setItemAnimator(new DefaultItemAnimator());
        radioRecyclerView.addItemDecoration(new GridSpacingItemDecoration(columns, 8, true));
        radioRecyclerView.setHasFixedSize(true);
    }

    private void setupAds() {
        FrameLayout adContainer = findViewById(R.id.ad_view_container);
        setupBanner(adContainer);
    }

    private void setupClickListeners() {

        trigger.setOnClickListener(v -> togglePlayback());

        subPlayer.setOnClickListener(v ->
                startActivity(new Intent(this, MyPlayers.class)));


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    /* -------------------- DATA -------------------- */

    @Override
    protected void onStart() {
        super.onStart();
        RadioManager.with(this).startAndBind();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (db == null) {
                db = new DatabaseHandler(this);
            }

            if (!isDataLoaded) {
                loadRadioStations();
                isDataLoaded = true;
            }

            updateSubPlayer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        RadioManager.with(this).unbind();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /* -------------------- RADIO -------------------- */

    private void togglePlayback() {
        radioManager.toggle();
    }

    private void updateSubPlayer() {
        if (FmConstants.isPlaying) {
            subPlayer.setVisibility(VISIBLE);
            trigger.setImageResource(R.drawable.ic_pause);
        } else {
            trigger.setImageResource(R.drawable.ic_play);
        }

        if (pendingStation != null) {
            subPlayerImage.setImageUrl(pendingStation.image, imageLoader);
        } else {
            subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);
        }

        subPlayerName.setText(pendingStation.name);

        // Set placeholder
        subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);

        // Load new image
        if (pendingStation.image != null && !pendingStation.image.isEmpty()) {
            subPlayerImage.setImageUrl(
                    pendingStation.image,
                    imageLoader
            );
        }
        subPlayer.setVisibility(View.VISIBLE);
    }

    /* -------------------- NETWORK -------------------- */

    private void loadRadioStations() {
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);

        StringRequest request = new StringRequest(Request.Method.GET, FM_JSON_URL,
                this::parseResponse,
                error -> loadBackup());

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MyApp.getInstance().addToRequestQueue(request);
    }

    private void parseResponse(String response) {
        try {
            if (response.startsWith("<!DOC")) {
                loadBackup();
                return;
            }

            JSONArray array = new JSONArray(response);
            fmList.clear();

            for (int i = 1; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Shoutcast s = new Shoutcast();
                s.setName(obj.getString("name"));
                s.setImage(obj.getString("image"));
                s.setUrl(obj.getString("stream"));
                fmList.add(s);
            }

            adapterMusic = new CustomAdapter( this, fmList, "Dashboard");
            radioRecyclerView.setAdapter(adapterMusic);
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        } catch (JSONException e) {
            loadBackup();
        }
    }

    private void loadBackup() {
        adapterMusic = new CustomAdapter(
                this,
                ShoutcastHelper.retrieveShoutcasts(this, "bollywood"),
                "Dashboard"
        );
        radioRecyclerView.setAdapter(adapterMusic);
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
    }

    /* -------------------- EVENT BUS -------------------- */

    @Subscribe
    public void onEvent(String status) {
        if (PlaybackStatus.ERROR.equals(status)) {
            Toast.makeText(this, "Station not available", Toast.LENGTH_SHORT).show();
        }
        updateSubPlayer();
    }

    /* -------------------- HELPERS -------------------- */

    private int calculateNoOfColumns(int itemWidthDp) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float screenWidthDp = dm.widthPixels / dm.density;
        return Math.max(2, (int) (screenWidthDp / itemWidthDp));
    }

    private void handleBackPress() {
        if (doubleBackToExitPressedOnce) {
            startActivity(new Intent(this, Exit.class));
            return;
        }

        doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.backagain, Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper())
                .postDelayed(() -> doubleBackToExitPressedOnce = false, 3000);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    /*@Override
    public void setDataValues() {
        if (FmConstants.isPlaying) {
            trigger.setImageResource(R.drawable.ic_pause);
        } else {
            trigger.setImageResource(R.drawable.ic_play);
        }

        if (pendingStation.image.isEmpty()) {
            subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);
        } else {
            subPlayerImage.setImageUrl(pendingStation.image, imageLoader);
        }
        subPlayerName.setText(pendingStation.name);
        subPlayer.setVisibility(View.VISIBLE);
    }*/

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onStationChanged(StationChangedEvent event) {
        if (event == null || event.station == null) return;
        Log.i("BollywoodFM","onStationChanged event "+event);
        pendingStation = event.station;
        Station station = event.station;
        if (event.isPlaying) {
            trigger.setImageResource(R.drawable.ic_pause);
        } else {
            trigger.setImageResource(R.drawable.ic_play);
        }

        subPlayerName.setText(station.name);

        subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);

        if (station.image != null && !station.image.isEmpty()) {
            subPlayerImage.setImageUrl(
                    station.image,
                    imageLoader
            );
        }

        subPlayer.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.favourites) {
            Intent a = new Intent(this, FavoritesRadio.class);
            startActivity(a);
        } else if (id == R.id.rateus) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(APP_URL));
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
