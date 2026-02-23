package com.newsparkapps.norwayfmradio.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdView;
import com.newsparkapps.norwayfmradio.R;
import com.newsparkapps.norwayfmradio.Utils;
import com.newsparkapps.norwayfmradio.ads.AdmobUtils;
import com.newsparkapps.norwayfmradio.fragments.HomeFragment;

public class Home extends AppCompatActivity {
    private Toolbar toolbar;
    private FrameLayout adContainer;
    private AdView bannerAdView;
    private boolean bannerLoaded = false;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_IS_NIGHT_MODE = "is_night_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        View rootView = findViewById(R.id.root_view);
        Utils.enableEdgeToEdge(this, rootView);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adContainer = findViewById(R.id.ad_view_container);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            loadBannerOnce();
        }
    }

    private void loadBannerOnce() {
        if (bannerLoaded) return;
        bannerAdView = AdmobUtils.createAdaptiveBanner(
                this,
                adContainer,
                AdmobUtils.getBannerAdUnitId(
                        AdmobUtils.getUserCountry(this)
                )
        );
        bannerLoaded = true;
    }

    public void setupToolbar(String title, boolean showBackButton) {

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(showBackButton);
        }

        toolbar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        try {
            FragmentTransaction tx = getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment);
            if (addToBackStack && !(fragment instanceof HomeFragment)) {
                tx.addToBackStack(null);
            }
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Home","onResume");
        if (bannerAdView != null) bannerAdView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("Home","onPause");
        if (bannerAdView != null) bannerAdView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && bannerAdView != null) {
            bannerAdView.destroy();
        }
    }
}
