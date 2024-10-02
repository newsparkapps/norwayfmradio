package com.newsparkapps.norwayfmradio;

import static android.content.Context.MODE_PRIVATE;
import static com.newsparkapps.norwayfmradio.FmConstants.BANNER_AD_ID;
import static com.newsparkapps.norwayfmradio.FmConstants.INTERSTITIAL_AD_ID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.Display;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdmobUtils {
    public static InterstitialAd interstitialAd;

    static SharedPreferences sharedPreferences;
    public static void loadBannerAd(Context context, AdView adView) {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    public static AdView createAdView(Context context) {
        AdView adView = new AdView(context);
        adView.setAdSize(getAdSize(context));
        adView.setAdUnitId(BANNER_AD_ID);
        return adView;
    }

    public static AdView createSquareAdView(Context context) {
        AdView adView2 = new AdView(context);
        adView2.setAdSize(getSquareAdSize(context));
        adView2.setAdUnitId(BANNER_AD_ID);
        return adView2;
    }

    public static AdView createSmallSquareAdView(Context context) {
        AdView adView3 = new AdView(context);
        adView3.setAdSize(getSmallSquareAdSize(context));
        adView3.setAdUnitId(BANNER_AD_ID);
        return adView3;
    }

    public static AdSize getAdSize(Context context) {
        Display display = ((AppCompatActivity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }

    public static AdSize getSquareAdSize(Context context) {
        AdSize adSize1 = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, 320);
        AdView bannerView1 = new AdView(context);
        bannerView1.setAdUnitId(BANNER_AD_ID);
        bannerView1.setAdSize(adSize1);
        AdRequest adRequest2 = new AdRequest.Builder().build();
        bannerView1.loadAd(adRequest2);
        return adSize1;
    }

    public static AdSize getSmallSquareAdSize(Context context) {
        AdSize adSize2 = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, 250);
        AdView bannerView1 = new AdView(context);
        bannerView1.setAdUnitId(BANNER_AD_ID);
        bannerView1.setAdSize(adSize2);
        AdRequest adRequest3 = new AdRequest.Builder().build();
        bannerView1.loadAd(adRequest3);
        return adSize2;
    }

    public static void loadInterstitialAd(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, INTERSTITIAL_AD_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                    }
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        interstitialAd = null;
                    }
                });
    }

    public static boolean isInterstitialAdLoaded() {
        return interstitialAd != null;
    }
    public static void showInterstitialAd(final Context context, final Class<?> targetActivity) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    loadInterstitialAd(context);
                    Intent intent = new Intent(context, targetActivity);
                    context.startActivity(intent);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    interstitialAd = null;
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    interstitialAd = null;
                }
            });

            interstitialAd.show((android.app.Activity) context);
        } else {
            Intent intent = new Intent(context, targetActivity);
            context.startActivity(intent);
            loadInterstitialAd(context);
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


