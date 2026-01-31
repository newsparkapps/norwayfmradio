package com.newsparkapps.norwayfmradio;


import static com.newsparkapps.norwayfmradio.FmConstants.BANNER_AD_ID;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class AdBannerManager {

    private static AdBannerManager instance;
    private AdView adView;
    private boolean isLoaded = false;

    private AdBannerManager() {}

    public static synchronized AdBannerManager getInstance() {
        if (instance == null) {
            instance = new AdBannerManager();
        }
        return instance;
    }

    public void load(Activity activity, FrameLayout container, String adUnitId) {
        if (adView == null) {
            adView = new AdView(activity);
            adView.setAdUnitId(adUnitId);
            adView.setAdSize(getAdaptiveSize(activity));

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    isLoaded = true;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        load(activity, container, BANNER_AD_ID);
                    }, 2500);

                }
            });

            adView.loadAd(new AdRequest.Builder().build());
        }

        attach(container);
    }

    private void attach(FrameLayout container) {
        if (adView.getParent() != null) {
            ((ViewGroup) adView.getParent()).removeView(adView);
        }
        container.removeAllViews();
        container.addView(adView);
    }

    public void pause() {
        if (adView != null) adView.pause();
    }

    public void resume() {
        if (adView != null) adView.resume();
    }

    public void destroy() {
        if (adView != null) {
            adView.destroy();
            adView = null;
            isLoaded = false;
        }
    }

    private AdSize getAdaptiveSize(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int adWidth = (int) (metrics.widthPixels / metrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);

        /*Display display = ((AppCompatActivity) activity).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);*/
    }
}
