package com.newsparkapps.norwayfmradio.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.newsparkapps.norwayfmradio.Circle_image;
import com.newsparkapps.norwayfmradio.FmConstants;
import com.newsparkapps.norwayfmradio.MyApp;
import com.newsparkapps.norwayfmradio.PlaybackStatus;
import com.newsparkapps.norwayfmradio.R;
import com.newsparkapps.norwayfmradio.RadioManager;
import com.newsparkapps.norwayfmradio.Station;
import com.newsparkapps.norwayfmradio.StationChangedEvent;
import com.newsparkapps.norwayfmradio.Utils;
import com.newsparkapps.norwayfmradio.activities.Home;
import com.newsparkapps.norwayfmradio.ads.AdmobUtils;
import com.newsparkapps.norwayfmradio.db.DatabaseHandler;
import com.newsparkapps.norwayfmradio.models.MyFourites;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class PlayerFragment extends BaseFragment {
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

    boolean favStatus = false;

    List<Shoutcast> shoutcasts = new ArrayList<Shoutcast>();
    private boolean isFavorite;
    @Override
    protected String getToolbarTitle() {
        return "Norway FM Radio";
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View rootView = inflater.inflate(R.layout.myplayers, container, false);
        radioManager = RadioManager.with(requireContext());
        trigger = rootView.findViewById(R.id.playTrigger);
        favorites = rootView.findViewById(R.id.favorites);
        bgImage = rootView.findViewById(R.id.bgImage);
        stationIcon = rootView.findViewById(R.id.stationimage);
        stationName = rootView.findViewById(R.id.stationame);
        volumeSeekBar = rootView.findViewById(R.id.seekBar);

        setupAudioControls();
        setupClickListeners();

        if ("one".equals(AdmobUtils.getAdOnStatus(requireContext()))) {
            AdmobUtils.loadInterstitialAd(requireContext(), AdmobUtils.getInterstitialAdUnitId(AdmobUtils.getUserCountry(requireContext())));
            AdmobUtils.setAdOnStatus(requireContext(), "zero");
        } else {
            AdmobUtils.setAdOnStatus(requireContext(), "one");
        }

        db = new DatabaseHandler(requireContext());
        imageLoader = MyApp.getInstance().getImageLoader();
        return rootView;
    }

    public boolean isFavoriteNameMatching(String searchTerm) {
        Log.i("isFavoriteNameMatching","isFavoriteNameMatching searchTerm"+searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return false;
        }

        List<Shoutcast> myFavorites = db.getAllFourites();

        for (Shoutcast item : myFavorites) {

            if (item != null &&
                    item.getName() != null &&
                    item.getName().toLowerCase()
                            .contains(searchTerm.toLowerCase())) {

                return true; // ✅ Found match
            }
        }

        return false;// ❌ No match found
    }

    private void setupAudioControls() {
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {

                                if (!isAdded()) return;

                                Home activity = (Home) requireActivity();

                                if(AdmobUtils.isInterstitialAdLoaded()) {
                                    AdmobUtils.showInterstitialAd(activity);
                                    activity
                                            .getSupportFragmentManager()
                                            .popBackStack();
                                } else {
                                    activity
                                            .getSupportFragmentManager()
                                            .popBackStack();
                                }
                            }
                        });
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

    private void togglePlayback() {
        radioManager.toggle();
        updatePlayButton();
    }

    private void setupClickListeners() {
        trigger.setOnClickListener(v -> togglePlayback());
        favorites.setOnClickListener(v -> toggleFavorite());
    }

    private void updatePlayButton() {
        trigger.setImageResource(
                FmConstants.isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
        );
    }

    /* -------------------- FAVORITES -------------------- */

    private void toggleFavorite() {
        try {
            if (favStatus) {
                db.deleteMessage(
                        new MyFourites(currentStation.name, currentStation.url, currentStation.image)
                );
                favorites.setImageResource(R.drawable.ic_favorites);
                Toast.makeText(requireActivity(), "Removed from favorite list", Toast.LENGTH_SHORT).show();
            } else {
                Utils.setFMAnalytics("Fav_"+currentStation.name, requireActivity());
                db.addShoutcast(
                        new MyFourites(currentStation.name, currentStation.url, currentStation.image)
                );
                favorites.setImageResource(R.drawable.ic_favorites_active);
                Toast.makeText(requireActivity(), "Added to favorite list", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onEvent(String status) {
        if (PlaybackStatus.ERROR.equals(status)) {
            Toast.makeText(requireActivity(), "Station not available", Toast.LENGTH_SHORT).show();
        }
        updateUI();
    }

    private void updateUI() {
        updatePlayButton();
        if (currentStation != null) {
            stationName.setText(currentStation.name);
            stationIcon.setImageBitmap(stationImageBitmap);
            bgImage.setImageResource(R.drawable.norwayfmradio);
            isFavorite = db.getFavorite(currentStation.name);
            Utils.setFMAnalytics("Player_"+currentStation.name, requireActivity());
        }
        favorites.setImageResource(
                isFavorite ? R.drawable.ic_favorites_active : R.drawable.ic_favorites
        );
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onStationChanged(StationChangedEvent event) {
        Station station = event.station;
        currentStation = event.station;
        stationName.setText(station.name);
        loadStationImage(station.image);
        stationIcon.setImageBitmap(stationImageBitmap);
        bgImage.setImageResource(R.drawable.norwayfmradio);
        trigger.setImageResource(
                event.isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
        );
        favStatus = isFavoriteNameMatching(currentStation.name);
        Log.i("favStatus","favStatus "+favStatus);
        if (favStatus) {
            favorites.setImageResource(R.drawable.ic_favorites_active);
        } else {
            favorites.setImageResource(R.drawable.ic_favorites);
        }

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
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        radioManager.startAndBind();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        radioManager.unbind();
        super.onStop();
    }

    @Override
    protected boolean showBackButton() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (db != null) db.close();
    }
}
