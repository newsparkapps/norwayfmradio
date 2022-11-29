package com.newsparkapps.norwayfmradio;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

import dyanamitechetan.vusikview.VusikView;
import me.drakeet.support.toast.ToastCompat;


public class MyPlayers extends AppCompatActivity {
    private FrameLayout adContainerView;
    private ImageLoader imageLoader = ImageLoader.getInstance();
    RadioManager radioManager;
    AdView adView;
    boolean addedornot;
    ImageButton trigger;
    ImageView favorites,qurekagames;
    DatabaseHandler db;
    private SeekBar mediaVlmSeekBar = null;
    AudioManager audiomanager;
    private VusikView vusikView;
    private NativeAd nativeAd;
    Circle_image stationIcon;
    private DisplayImageOptions options;
    TextView stationName;
    InterstitialAd mInterstitialAd;
    FirebaseRemoteConfig firebaseRemoteConfig;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myplayers);

        radioManager = RadioManager.with(this);
        stationName = findViewById(R.id.stationame);
        trigger = findViewById(R.id.playTrigger);
        stationIcon = findViewById(R.id.stationimage);
        favorites = findViewById(R.id.favorites);
        db = new DatabaseHandler(this);

        vusikView = findViewById(R.id.vusikView);

        adContainerView = findViewById(R.id.ad_view_container);
        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.banneradid));
        adContainerView.addView(adView);
        loadBanner();

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        firebaseRemoteConfig.setConfigSettingsAsync(
                new FirebaseRemoteConfigSettings.Builder().build());

        firebaseRemoteConfig.fetch(10).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Once the config is successfully fetched it must be activated before newly fetched values are returned.
                    firebaseRemoteConfig.activate();
                    checkStatus();
                } else {
                }
            }
        });




        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        initControls();

        try {
            mediaVlmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onStopTrackingTouch(SeekBar arg0) {
                }
                public void onStartTrackingTouch(SeekBar arg0) {
                }
                //When progress level of seekbar1 is changed
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            int[] myImageList = new int[]{R.drawable.music1, R.drawable.music2, R.drawable.music1};
            vusikView.setImages(myImageList).start();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        addedornot = db.getFavorite(FmConstants.fmname);
        if (addedornot) {
            favorites.setImageResource(R.drawable.ic_favorites_active);
        } else {
            favorites.setImageResource(R.drawable.ic_favorites);
        }

        favorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (addedornot) {
                                db.deleteMessage(new MyFourites(FmConstants.fmname, FmConstants.fmurl, FmConstants.fmimage));
                                favorites.setImageResource(R.drawable.ic_favorites);
                                if (android.os.Build.VERSION.SDK_INT == 25) {
                                    ToastCompat.makeText(getApplicationContext(), "Removed from favorite list", Toast.LENGTH_SHORT)
                                            .setBadTokenListener(toast -> {
                                                Log.e("failed toast", "Removed from favorite list");
                                            }).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Removed from favorite list", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                db.addShoutcast(new MyFourites(FmConstants.fmname, FmConstants.fmurl, FmConstants.fmimage));
                                favorites.setImageResource(R.drawable.ic_favorites_active);
                                if (android.os.Build.VERSION.SDK_INT == 25) {
                                    ToastCompat.makeText(getApplicationContext(), "Added to favorite list", Toast.LENGTH_SHORT)
                                            .setBadTokenListener(toast -> {
                                                Log.e("failed toast", "Added to favorite list");
                                            }).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Added to favorite list", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }, 500);


                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(FmConstants.fmurl)) return;
                radioManager.playOrPause(FmConstants.fmname,
                        FmConstants.fmimage, FmConstants.fmurl);
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
    public void onDestroy() {
        //radioManager.unbind();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        radioManager.bind();
    }

    @Subscribe
    public void onEvent(String status) {
        switch (status) {
            case PlaybackStatus.LOADING:
                // loading
                break;

            case PlaybackStatus.ERROR:
                Toast.makeText(getApplicationContext(), "Station not available", Toast.LENGTH_SHORT).show();
                break;
        }

        trigger.setImageResource(status.equals(PlaybackStatus.PLAYING)
                ? R.drawable.ic_pause_black
                : R.drawable.ic_play_arrow_black);
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    private void loadBanner() {
        AdRequest adRequest =
                new AdRequest.Builder().build();
        AdSize adSize = getAdSize();
        adView.setAdSize(adSize);
        adView.loadAd(adRequest);
    }

    private void initControls() {
        audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaVlmSeekBar = findViewById(R.id.seekBar);
        mediaVlmSeekBar.setMax(audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        mediaVlmSeekBar.setProgress(audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        try {
            adView.setMediaView(mediaView);
            mediaView.setImageScaleType(ImageView.ScaleType.FIT_XY);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.GONE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.GONE);
        } else {
            adView.getPriceView().setVisibility(View.GONE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.GONE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.GONE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.GONE);
        }
        adView.setNativeAd(nativeAd);
        VideoController vc = nativeAd.getMediaContent().getVideoController();
        if (vc.hasVideoContent()) {
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    super.onVideoEnd();
                }
            });
        } else {
        }
    }

    private void callNativeAds() {
        AdLoader.Builder builder = new AdLoader.Builder(this, getResources().getString(R.string.nativeadid));
        builder.forNativeAd(
                new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        boolean isDestroyed = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = isDestroyed();
                        }
                        if (isDestroyed || isFinishing() || isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }
                        if (MyPlayers.this.nativeAd != null) {
                            MyPlayers.this.nativeAd.destroy();
                        }
                        MyPlayers.this.nativeAd = nativeAd;
                        FrameLayout frameLayout = findViewById(R.id.fl_adplaceholder);
                        NativeAdView adView =
                                (NativeAdView) getLayoutInflater().inflate(R.layout.ad_unified_small, null);
                        populateNativeAdView(nativeAd, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }
                });

        VideoOptions videoOptions =
                new VideoOptions.Builder().build();
        com.google.android.gms.ads.nativead.NativeAdOptions adOptions =
                new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        builder.withNativeAdOptions(adOptions);
        AdLoader adLoader =
                builder
                        .withAdListener(
                                new AdListener() {
                                    @Override
                                    public void onAdFailedToLoad(LoadAdError loadAdError) {

                                    }
                                })
                        .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void checkStatus() {
        String showAds = firebaseRemoteConfig.getString("new6BollywoodShowAds");
        if (showAds.equals("true")) {
            callNativeAds();
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(this,getResources().getString(R.string.interstitialadid), adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.
                    mInterstitialAd = interstitialAd;
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Called when fullscreen content is dismissed.
                            Log.d("TAG", "The ad was dismissed.");
                            Intent a = new Intent(getApplicationContext(), Detailed.class);
                            startActivity(a);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            // Called when fullscreen content failed to show.
                            Log.d("TAG", "The ad failed to show.");
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                            // Make sure to set your reference to null so you don't
                            // show it a second time.
                            mInterstitialAd = null;
                            Log.d("TAG", "The ad was shown.");
                        }
                    });
                }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error
                    mInterstitialAd = null;
                }
            });
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mInterstitialAd != null) {
                        mInterstitialAd.show(MyPlayers.this);
                    } else {
                        onBackPressed();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
