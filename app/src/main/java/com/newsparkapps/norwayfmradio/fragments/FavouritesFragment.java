package com.newsparkapps.norwayfmradio.fragments;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.newsparkapps.norwayfmradio.GridSpacingItemDecoration;
import com.newsparkapps.norwayfmradio.MyApp;
import com.newsparkapps.norwayfmradio.PlaybackStatus;
import com.newsparkapps.norwayfmradio.R;
import com.newsparkapps.norwayfmradio.RadioManager;
import com.newsparkapps.norwayfmradio.Station;
import com.newsparkapps.norwayfmradio.StationChangedEvent;
import com.newsparkapps.norwayfmradio.Utils;
import com.newsparkapps.norwayfmradio.activities.Home;
import com.newsparkapps.norwayfmradio.adapters.CustomAdapter;
import com.newsparkapps.norwayfmradio.db.DatabaseHandler;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class FavouritesFragment extends BaseFragment {

    LinearLayout subPlayer;
    ImageButton trigger;
    private TextView subPlayerName;
    private Station pendingStation;
    private ImageLoader imageLoader;
    private NetworkImageView subPlayerImage;
    CustomAdapter adapterMusic;
    DatabaseHandler db;
    private RadioManager radioManager;

    TextView status;
    RecyclerView radiorecyclerView;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View rootView = inflater.inflate(R.layout.favoritesfm, container, false);
 
        List<Shoutcast> shoutcasts = new ArrayList<Shoutcast>();
        subPlayer = rootView.findViewById(R.id.sub_player);
        trigger = rootView.findViewById(R.id.playTrigger);
        status = rootView.findViewById(R.id.status);
        subPlayerName = rootView.findViewById(R.id.subplayername);
        subPlayerImage = rootView.findViewById(R.id.subplayerimage);
        radiorecyclerView =  rootView.findViewById(R.id.recycler_view);
        radiorecyclerView.setHasFixedSize(true);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());

        radioManager = RadioManager.with(requireActivity());
        imageLoader = MyApp.getInstance().getImageLoader();

        Utils.setFMAnalytics("Favourites", requireContext());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        radiorecyclerView.setLayoutManager(linearLayoutManager);

        Home activity =(Home) requireActivity();
        int itemWidth = Utils.isTablet(activity) ? 180 : 120;
        int columns = Utils.calculateNoOfColumns(itemWidth,requireContext());

        radiorecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), columns));
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());
        radiorecyclerView.addItemDecoration(new GridSpacingItemDecoration(columns, 8, true));
        radiorecyclerView.setHasFixedSize(true);
 

        trigger.setOnClickListener(v -> togglePlayback());

        db = new DatabaseHandler(requireContext());
        List<Shoutcast> myFourites = db.getAllFourites();
        for (Shoutcast cn : myFourites)
        {
            Shoutcast items = new Shoutcast(cn.getName(),cn.getUrl(),cn.getImage());
            shoutcasts.add(items);
        }

        if (myFourites.isEmpty()) {
            status.setVisibility(VISIBLE);
        } else {
            status.setVisibility(GONE);
        }

        adapterMusic = new CustomAdapter(requireContext(), shoutcasts,"Detailed");
        radiorecyclerView.setAdapter(adapterMusic);
        radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
        updateSubPlayer();

        subPlayer.setOnClickListener(v ->
        {
            activity.loadFragment(new PlayerFragment(), true);
        });

        handleBackPress();

        return rootView;
    }


    private void updateSubPlayer() {
        if (pendingStation != null) {
            subPlayerName.setText(pendingStation.name);
            subPlayerImage.setImageUrl(pendingStation.image, imageLoader);
        } else {
            subPlayerImage.setDefaultImageResId(R.drawable.norway_fm_radio_logo);
        }
        subPlayer.setVisibility(VISIBLE);
    }
    private void togglePlayback() {
        radioManager.toggle();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSubPlayer();
    }

    private void handleBackPress() {
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                requireActivity()
                                        .getSupportFragmentManager()
                                        .popBackStack();
                            }
                        }
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (db != null) {
            db.close();
        }
    }
    @Override
    protected String getToolbarTitle() {
        return "My Favourites";
    }

    @Override
    protected boolean showBackButton() {
        return true;
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

    @Subscribe
    public void onEvent(String status) {
        if (PlaybackStatus.ERROR.equals(status)) {
            Toast.makeText(requireContext(), "Station not available", Toast.LENGTH_SHORT).show();
        }
        updateSubPlayer();
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
        subPlayer.setVisibility(VISIBLE);
    }

}
