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
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.newsparkapps.norwayfmradio.R;
import com.newsparkapps.norwayfmradio.Utils;
import com.newsparkapps.norwayfmradio.ads.AdmobUtils;
import com.newsparkapps.norwayfmradio.fragments.HomeFragment;

import java.util.concurrent.atomic.AtomicBoolean;

public class Home extends AppCompatActivity {
    private Toolbar toolbar;
    private FrameLayout adContainer;
    private AdView bannerAdView;
    private boolean bannerLoaded = false;

    private ConsentInformation consentInformation;
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
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


        checkConsentRequest();
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

    private void checkConsentRequest() {
        try {
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

            consentInformation = UserMessagingPlatform.getConsentInformation(this);
            consentInformation.requestConsentInfoUpdate(this, params,
                    () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, loadAndShowError -> {
                        if (loadAndShowError != null) {
                            Log.w("AS_Consent", String.format("Load Error %s: %s",
                                    loadAndShowError.getErrorCode(),
                                    loadAndShowError.getMessage()));
                            return;
                        }
                        // Check if ads can be requested
                        if (consentInformation != null && consentInformation.canRequestAds()) {
                            initializeMobileAdsSdk();
                        }
                    }),
                    requestConsentError -> Log.w("AS_Consent", String.format("Request Error %s: %s",
                            requestConsentError.getErrorCode(),
                            requestConsentError.getMessage()))
            );

        } catch (NullPointerException e) {
            Log.e("TAG","ConsentInformation null",e);
        } catch (OutOfMemoryError e) {
            Log.e("TAG","ConsentInformation OOM",e);
        }
    }

    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }
        MobileAds.initialize(this);
    }
}
