package com.newsparkapps.norwayfmradio;

import static com.newsparkapps.norwayfmradio.FmConstants.BANNER_AD_ID;

import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected void setupBanner(FrameLayout adContainer) {
        AdBannerManager.getInstance().load(
                this,
                adContainer,
                BANNER_AD_ID
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        AdBannerManager.getInstance().resume();
    }

    @Override
    protected void onPause() {
        AdBannerManager.getInstance().pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ❌ DO NOT destroy here
    }
}