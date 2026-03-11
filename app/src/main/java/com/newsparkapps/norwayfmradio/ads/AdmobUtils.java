package com.newsparkapps.norwayfmradio.ads;

import static com.newsparkapps.norwayfmradio.FmConstants.BANNER_AD_CODE;
import static com.newsparkapps.norwayfmradio.FmConstants.INTERSTITIAL_AD_CODE;
import static com.newsparkapps.norwayfmradio.FmConstants.OPEN_AD_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AdmobUtils {

    private static final String TAG = "AdmobUtils";

    // ==============================
    // 🔹 Interstitial
    // ==============================

    public static InterstitialAd interstitialAd;
    private static int interstitialRetry = 0;
    private static final int MAX_INTERSTITIAL_RETRY = 2;

    // ==============================
    // 🔹 Country Tiers
    // ==============================

    private static final Set<String> TIER1_COUNTRIES = new HashSet<>(Arrays.asList(
            "US","CA","GB","DE","FR","AU","NZ","CH","SE","NO","DK","FI","NL","AT","IE","BE","SG","JP","KR"
    ));

    private static final Set<String> TIER2_COUNTRIES = new HashSet<>(Arrays.asList(
            "IT","ES","PT","PL","CZ","GR","HU","IL","TR","BR","MX","CL","AR","ZA","AE","SA","MY","TH","VN"
    ));

    private static final Set<String> TIER3_COUNTRIES = new HashSet<>(Arrays.asList(
            "IN","PK","BD","NP","LK","ID","PH","NG","KE","EG","ET","TZ","MA","UA","RU","PE","CO"
    ));

    // =====================================================
    // 🔹 Banner – Tier Based + Retry Once With Fallback
    // =====================================================

    public static AdView createAdaptiveBanner(
            Activity activity,
            FrameLayout container,
            String tierAdUnitId,
            String normalAdUnitId
    ) {

        if (activity == null || activity.isFinishing()) {
            return null;
        }

        container.removeAllViews();

        AdView adView = new AdView(activity);
        adView.setAdSize(getAdaptiveSize(activity));
        adView.setAdUnitId(tierAdUnitId);
        Log.d(TAG, "Banner adunit "+tierAdUnitId);

        container.addView(adView);

        final boolean[] hasRetried = {false};

        adView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner loaded");
                hasRetried[0] = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {

                Log.e(TAG, "Banner failed: " + error.getCode());

                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                // Retry only once with NORMAL ad unit
                if (!hasRetried[0]) {
                    hasRetried[0] = true;
                    Log.d(TAG, "Retrying once with fallback banner...");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            container.removeAllViews();
                            AdView fallbackAdView = new AdView(activity);
                            fallbackAdView.setAdSize(getAdaptiveSize(activity));
                            fallbackAdView.setAdUnitId(normalAdUnitId);
                            container.addView(fallbackAdView);
                            fallbackAdView.loadAd(new AdRequest.Builder().build());
                        }
                    }, 1500);

                } else {
                    Log.d(TAG, "Already retried once. No more retries.");
                }
            }

            @Override
            public void onAdImpression() {
                Log.d(TAG, "Banner impression");
            }
        });

        adView.loadAd(new AdRequest.Builder().build());

        return adView;
    }

    private static AdSize getAdaptiveSize(Activity activity) {

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int adWidth = (int) (metrics.widthPixels / metrics.density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                activity,
                adWidth
        );
    }

    // =====================================================
    // 🔹 Interstitial – Limited Retry
    // =====================================================

    public static void loadInterstitialAd(Context context, String adUnit) {

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, adUnit, adRequest,
                new InterstitialAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {

                        Log.d(TAG, "Interstitial loaded");

                        interstitialAd = ad;
                        interstitialRetry = 0;

                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {

                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        interstitialAd = null;
                                        loadInterstitialAd(context, adUnit);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(
                                            @NonNull AdError adError) {
                                        interstitialAd = null;
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {

                        Log.e(TAG, "Interstitial failed: " + adError.getMessage());
                        interstitialAd = null;

                        if (interstitialRetry < MAX_INTERSTITIAL_RETRY) {

                            interstitialRetry++;

                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    loadInterstitialAd(context, INTERSTITIAL_AD_CODE), 2000);

                        } else {
                            interstitialRetry = 0;
                        }
                    }
                });
    }

    public static boolean isInterstitialAdLoaded() {
        return interstitialAd != null;
    }

    // =====================================================
    // 🔹 Country Detection (Safe)
    // =====================================================

    public static String getUserCountry(Context context) {

        String countryCode = null;

        try {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (tm != null) {

                // 1️⃣ SIM Country (most stable)
                countryCode = tm.getSimCountryIso();

                // 2️⃣ Network Country (if SIM empty)
                if (countryCode == null || countryCode.isEmpty()) {
                    countryCode = tm.getNetworkCountryIso();
                }
            }

        } catch (Exception ignored) {
        }

        // 3️⃣ Locale fallback
        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = Locale.getDefault().getCountry();
        }

        // 4️⃣ Final safety fallback
        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = "US";  // safe default
        }

        return countryCode.toUpperCase(Locale.ROOT);
    }

    // =====================================================
    // 🔹 Tier-Based Ad Unit Selection
    // =====================================================

    public static void showInterstitialAd(final Activity activity) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    interstitialAd = null;
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    interstitialAd = null;
                }
            });

            interstitialAd.show(activity);
        }
    }

    public static String getBannerAdUnitId(String countryCode) {
        return BANNER_AD_CODE;
    }

    public static String getInterstitialAdUnitId(String countryCode) {
        return INTERSTITIAL_AD_CODE;
    }

    public static String getOpenAdUnitId(String countryCode) {
        return OPEN_AD_CODE;
    }

    // =====================================================
    // 🔹 Simple Ad Toggle (SharedPref)
    // =====================================================

    public static String getAdOnStatus(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("FM_RADIO_ONLINE", Context.MODE_PRIVATE);
        return prefs.getString("adStatus", "zero");
    }

    public static void setAdOnStatus(Context context, String value) {
        SharedPreferences prefs =
                context.getSharedPreferences("FM_RADIO_ONLINE", Context.MODE_PRIVATE);
        prefs.edit().putString("adStatus", value).apply();
    }
}