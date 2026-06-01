package com.newsparkapps.norwayfmradio.activities;

import static com.newsparkapps.norwayfmradio.FmConstants.BANNER_AD_CODE;

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
import com.newsparkapps.norwayfmradio.ads.AdmobUtils;
import com.newsparkapps.norwayfmradio.R;
import com.newsparkapps.norwayfmradio.Utils;
import com.newsparkapps.norwayfmradio.fragments.HomeFragment;

import java.util.concurrent.atomic.AtomicBoolean;

public class Home extends AppCompatActivity {
    private Toolbar toolbar;
    private FrameLayout adContainer;
    private AdView bannerAdView;
    private boolean bannerLoaded = false;

    private ConsentInformation consentInformation;
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);

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
        }
    }

    private void loadBannerOnce() {
        if (bannerLoaded) return;
        String tierAdUnit = AdmobUtils.getBannerAdUnitId(
                AdmobUtils.getUserCountry(this));
        bannerAdView = AdmobUtils.createAdaptiveBanner(
                this,
                adContainer,
                tierAdUnit,
                BANNER_AD_CODE   // normal fallback
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
        if (bannerAdView != null) bannerAdView.resume();
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) bannerAdView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && bannerAdView != null) {
            bannerAdView.destroy();
        }
    }


    private void checkConsentRequest() {

        ConsentRequestParameters params =
                new ConsentRequestParameters.Builder().build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);

        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> {

                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            this,
                            formError -> {

                                if (formError != null) {
                                    Log.w("AdMob", formError.getMessage());
                                    return;
                                }

                                if (consentInformation.canRequestAds()) {
                                    initializeMobileAdsSdk();
                                }
                            });
                },
                requestConsentError ->
                        Log.w("AdMob", requestConsentError.getMessage()));
    }

    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }
        MobileAds.initialize(this, initializationStatus -> {
            loadBannerOnce();
            AdmobUtils.loadInterstitialAd(this, AdmobUtils.getInterstitialAdUnitId(AdmobUtils.getUserCountry(this)));
        });
    }
}
