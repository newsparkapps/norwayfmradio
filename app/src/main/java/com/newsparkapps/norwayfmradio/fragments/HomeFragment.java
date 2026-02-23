package com.newsparkapps.norwayfmradio.fragments;

import static com.newsparkapps.norwayfmradio.FmConstants.APP_URL;
import static com.newsparkapps.norwayfmradio.FmConstants.FM_JSON_URL;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.newsparkapps.norwayfmradio.GridSpacingItemDecoration;
import com.newsparkapps.norwayfmradio.MyApp;
import com.newsparkapps.norwayfmradio.PlaybackStatus;
import com.newsparkapps.norwayfmradio.R;
import com.newsparkapps.norwayfmradio.RadioManager;
import com.newsparkapps.norwayfmradio.Station;
import com.newsparkapps.norwayfmradio.StationChangedEvent;
import com.newsparkapps.norwayfmradio.Utils;
import com.newsparkapps.norwayfmradio.activities.Exit;
import com.newsparkapps.norwayfmradio.activities.Home;
import com.newsparkapps.norwayfmradio.activities.StreamValidatorActivity;
import com.newsparkapps.norwayfmradio.adapters.CustomAdapter;
import com.newsparkapps.norwayfmradio.db.DatabaseHandler;
import com.newsparkapps.norwayfmradio.util.Shoutcast;
import com.newsparkapps.norwayfmradio.util.ShoutcastHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment {
    private static final String PREF_NAME = "fm_sync_pref";
    private static final String KEY_LAST_SYNC = "last_sync_date";
    private RecyclerView radioRecyclerView;
    private CustomAdapter adapterMusic;
    private ShimmerFrameLayout shimmerFrameLayout;
    private LinearLayout subPlayer;
    private ImageButton trigger;
    ImageView promo;
    private static final long HOLD_TIME = 5000; // 6 seconds
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable holdRunnable;
    private TextView subPlayerName;
    private NetworkImageView subPlayerImage;
    private RadioManager radioManager;
    private ImageLoader imageLoader;
    private Station pendingStation;
    private final ArrayList<Shoutcast> fmList = new ArrayList<>();
    private boolean isDataLoaded = false;
    private DatabaseHandler db;
    private boolean doubleBackToExitPressedOnce = false;
    private static final int PERMISSION_REQUEST_CODE = 112;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        requireActivity().addMenuProvider(
                new MenuProvider() {
                    @Override
                    public void onCreateMenu(
                            @NonNull Menu menu,
                            @NonNull MenuInflater menuInflater
                    ) {
                        menu.clear();
                        menuInflater.inflate(R.menu.menu_main, menu);
                    }

                    @Override
                    public boolean onMenuItemSelected(
                            @NonNull MenuItem item
                    ) {
                        Home activity =(Home) requireActivity();
                        int id = item.getItemId();
                        if (id == R.id.favourites) {
                            activity.loadFragment(new FavouritesFragment(), true);
                        } else if (id == R.id.rateus) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setData(Uri.parse(APP_URL));
                            startActivity(intent);
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(),
                Lifecycle.State.RESUMED
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dashboard, container, false);

        shimmerFrameLayout = view.findViewById(R.id.shimmerFrameLayout);
        radioRecyclerView = view.findViewById(R.id.recycler_view);
        subPlayer = view.findViewById(R.id.sub_player);
        trigger = view.findViewById(R.id.playTrigger);
        promo = view.findViewById(R.id.promo);
        subPlayerName = view.findViewById(R.id.subplayername);
        subPlayerImage = view.findViewById(R.id.subplayerimage);

        db = new DatabaseHandler(requireContext());


        promo.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    holdRunnable = () -> {
                        // 🔥 Action after 6 seconds hold

                        Intent a = new Intent(requireActivity(),StreamValidatorActivity.class);
                        startActivity(a);
                    };

                    handler.postDelayed(holdRunnable, HOLD_TIME);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    // ❌ Cancel if released before 6 seconds
                    handler.removeCallbacks(holdRunnable);
                    return true;
            }

            return false;
        });
        
        setupRecyclerView();
        setupClickListeners();


        radioManager = RadioManager.with(requireContext());
        imageLoader = MyApp.getInstance().getImageLoader();


        if (shouldFetchFromServer()) {
            loadRadioStations();
        } else {
            List<Shoutcast> list = db.getAllFmList();
            if (list != null && list.size() > 1) {
                Collections.reverse(list);
                list.remove(0);
            }
            radioRecyclerView.setItemViewCacheSize(20);
            radioRecyclerView.setDrawingCacheEnabled(true);
            radioRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

            adapterMusic = new CustomAdapter( requireActivity(), list, "Dashboard");
            radioRecyclerView.setAdapter(adapterMusic);
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);

        }

        requestNotificationPermission();
        Utils.setFMAnalytics("Home", requireActivity());
        handleBackPress();
        return view;
    }

    private void setupClickListeners() {

        trigger.setOnClickListener(v -> togglePlayback());

        subPlayer.setOnClickListener(v ->
        {
            Home activity = (Home) requireActivity();
            activity.loadFragment(new PlayerFragment(), true);
        });


    }

    private void togglePlayback() {
        radioManager.toggle();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private void setupRecyclerView() {
        Home activity =(Home) requireActivity();
        int itemWidth = Utils.isTablet(activity) ? 180 : 120;
        int columns = Utils.calculateNoOfColumns(itemWidth,requireContext());

        radioRecyclerView.setLayoutManager(new GridLayoutManager(activity, columns));
        radioRecyclerView.setItemAnimator(new DefaultItemAnimator());
        radioRecyclerView.addItemDecoration(new GridSpacingItemDecoration(columns, 8, true));
        radioRecyclerView.setHasFixedSize(true);
    }

    @Override
    protected String getToolbarTitle() {
        return "Norway FM Radio";
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context == null) return;
        try {

            if (!isDataLoaded) {
               // loadRadioStations();
                isDataLoaded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
    public void onStart() {
        super.onStart();
        RadioManager.with(requireActivity()).startAndBind();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        RadioManager.with(requireActivity()).unbind();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void handleBackPress() {
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (doubleBackToExitPressedOnce) {
                                    Utils.startActivity(
                                            (Activity) requireContext(),
                                            Exit.class
                                    );
                                    return;
                                }

                                doubleBackToExitPressedOnce = true;
                                toast(getString(R.string.backagain));

                                new Handler(Looper.getMainLooper())
                                        .postDelayed(
                                                () -> doubleBackToExitPressedOnce = false,
                                                3000
                                        );
                            }
                        });
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

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

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Shoutcast>>(){}.getType();
            List<Shoutcast> serverList =
                    gson.fromJson(response, listType);
            db.clearFmList();
            // Save to DB
            for (Shoutcast item : serverList) {
                db.addShoutcastToList(item);
            }

            saveTodayAsSyncDate();

            adapterMusic = new CustomAdapter( requireActivity(), fmList, "Dashboard");
            radioRecyclerView.setAdapter(adapterMusic);
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        } catch (JSONException e) {
            loadBackup();
        }
    }

    private void loadBackup() {
        adapterMusic = new CustomAdapter(
                requireActivity(),
                ShoutcastHelper.retrieveShoutcasts(requireActivity()),
                "Dashboard"
        );
        radioRecyclerView.setAdapter(adapterMusic);
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
    }

    @Subscribe
    public void onEvent(String status) {
        if (PlaybackStatus.ERROR.equals(status)) {
            Toast.makeText(requireActivity(), "Station not available", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean shouldFetchFromServer() {

        SharedPreferences prefs =
                requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String lastSyncDate = prefs.getString(KEY_LAST_SYNC, "");

        String todayDate = new SimpleDateFormat("yyyyMMdd",
                Locale.getDefault()).format(new Date());

        return !todayDate.equals(lastSyncDate);
    }

    private void saveTodayAsSyncDate() {

        SharedPreferences prefs =
                requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String todayDate = new SimpleDateFormat("yyyyMMdd",
                Locale.getDefault()).format(new Date());

        prefs.edit().putString(KEY_LAST_SYNC, todayDate).apply();
    }
}
