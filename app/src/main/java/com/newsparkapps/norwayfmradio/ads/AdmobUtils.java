package com.newsparkapps.norwayfmradio.ads;

import static android.content.Context.MODE_PRIVATE;
import static com.newsparkapps.norwayfmradio.FmConstants.BANNER_AD_CODE;
import static com.newsparkapps.norwayfmradio.FmConstants.INTERSTITIAL_CODE;
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
    private static int retryCount = 0;
    private static final int MAX_RETRY = 3;
    private static final String TAG = "AdmobUtils";
    public static InterstitialAd interstitialAd;
    static SharedPreferences sharedPreferences;
    private static final Set<String> TIER1_COUNTRIES = new HashSet<>(Arrays.asList(
            "US","CA","GB","DE","FR","AU","NZ","CH","SE","NO","DK","FI","NL","AT","IE","BE","SG","JP","KR"
    ));
    private static final Set<String> TIER2_COUNTRIES = new HashSet<>(Arrays.asList(
            "IT","ES","PT","PL","CZ","GR","HU","IL","TR","BR","MX","CL","AR","ZA","AE","SA","MY","TH","VN"
    ));
    private static final Set<String> TIER3_COUNTRIES = new HashSet<>(Arrays.asList(
            "IN","PK","BD","NP","LK","ID","PH","NG","KE","EG","ET","TZ","MA","UA","RU","PE","CO"
    ));

    public static AdView createAdaptiveBanner(
            Activity activity,
            FrameLayout container,
            String adUnitId
    ) {
        return createAdaptiveBanner(activity, container, adUnitId, false);
    }

    private static AdView createAdaptiveBanner(
            Activity activity,
            FrameLayout container,
            String adUnitId,
            boolean hasRetried
    ) {
        Log.d(TAG, "createAdaptiveBanner adUnitId=" + adUnitId +
                " hasRetried=" + hasRetried);

        container.removeAllViews();

        AdView adView = new AdView(activity);
        adView.setAdUnitId(adUnitId);
        adView.setAdSize(getAdaptiveSize(activity));
        container.addView(adView);

        adView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {

                Log.e(TAG, "Banner failed: " + error.getCode());

                // ✅ Only retry once
                if (hasRetried) {
                    Log.d(TAG, "Retry already attempted. Stopping.");
                    return;
                }

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!activity.isFinishing()) {
                        Log.d(TAG, "Retrying banner load...");

                        createAdaptiveBanner(
                                activity,
                                container,
                                BANNER_AD_CODE,
                                true   // mark retry used
                        );
                    }
                }, 2000);
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
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int adWidth = (int) (metrics.widthPixels / metrics.density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                activity,
                adWidth
        );
    }

    private static AdSize getAdaptiveAdSize(
            Activity activity,
            FrameLayout adContainer,
            String type
    ) {
        if ("banner".equalsIgnoreCase(type)) {

            int adWidthPixels = adContainer.getWidth();

            if (adWidthPixels == 0) {
                DisplayMetrics metrics = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                adWidthPixels = metrics.widthPixels;
            }

            float density = activity.getResources().getDisplayMetrics().density;
            int adWidth = (int) (adWidthPixels / density);

            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity,
                    adWidth
            );
        }

        // Square / MREC
        return AdSize.MEDIUM_RECTANGLE;
    }

    public static void loadInterstitialAd(Context context, String adUnit) {
        Log.d("AdmobUtils", "loadInterstitialAd "+adUnit);
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, adUnit, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.d("AdmobUtils", "loadInterstitialAd onAdLoaded");
                        interstitialAd = ad;
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        Log.e("AdmobUtils", "loadInterstitialAd failed to load: " + adError.getMessage());
                        interstitialAd = null;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            loadInterstitialAd(context, INTERSTITIAL_CODE);
                        }, 2000);

                    }
                });
    }

    public static boolean isInterstitialAdLoaded() {
        return interstitialAd != null;
    }

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

    public static String getUserCountry(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String country = tm.getNetworkCountryIso();
            if (country != null && !country.isEmpty()) {
                return country.toUpperCase(Locale.ROOT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Locale.getDefault().getCountry();
    }

    public static String getBannerAdUnitId(String countryCode) {
        if (countryCode == null) return BANNER_AD_CODE; // fallback
        countryCode = countryCode.toUpperCase(Locale.ROOT);

        if (TIER1_COUNTRIES.contains(countryCode)) {
            return BANNER_AD_CODE;
        } else if (TIER2_COUNTRIES.contains(countryCode)) {
            return BANNER_AD_CODE;
        } else if (TIER3_COUNTRIES.contains(countryCode)) {
            return BANNER_AD_CODE;
        } else {
            return BANNER_AD_CODE;
        }
    }

    public static String getOpenAdUnitId(String countryCode) {
        if (countryCode == null) return OPEN_AD_CODE; // fallback
        countryCode = countryCode.toUpperCase(Locale.ROOT);

        if (TIER1_COUNTRIES.contains(countryCode)) {
            return OPEN_AD_CODE;
        } else if (TIER2_COUNTRIES.contains(countryCode)) {
            return OPEN_AD_CODE;
        } else if (TIER3_COUNTRIES.contains(countryCode)) {
            return OPEN_AD_CODE;
        } else {
            return OPEN_AD_CODE;
        }
    }

    public static String getInterstitialAdUnitId(String countryCode) {
        if (countryCode == null) return INTERSTITIAL_CODE; // fallback
        countryCode = countryCode.toUpperCase(Locale.ROOT);

        if (TIER1_COUNTRIES.contains(countryCode)) {
            return INTERSTITIAL_CODE;
        } else if (TIER2_COUNTRIES.contains(countryCode)) {
            return INTERSTITIAL_CODE;
        } else if (TIER3_COUNTRIES.contains(countryCode)) {
            return INTERSTITIAL_CODE;
        } else {
            return INTERSTITIAL_CODE;
        }
    }


    public static String getAdOnStatus(Context context) {
        sharedPreferences = context.getSharedPreferences("FM_RADIO_ONLINE", MODE_PRIVATE);
        String adstatus = "zero";
        adstatus = sharedPreferences.getString("adStatus", "zero");
        return adstatus;
    }

    public static void setAdOnStatus(Context context,String value) {
        SharedPreferences.Editor editor;
        sharedPreferences = context.getSharedPreferences("FM_RADIO_ONLINE", MODE_PRIVATE);
        sharedPreferences = context.getSharedPreferences("FM_RADIO_ONLINE", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString("adStatus", value);
        editor.apply();
    }
}


