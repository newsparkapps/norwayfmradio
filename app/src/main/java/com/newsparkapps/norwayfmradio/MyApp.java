package com.newsparkapps.norwayfmradio;


import static com.newsparkapps.norwayfmradio.FmConstants.OPEN_AD_ID;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.lang.ref.WeakReference;

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private static final String TAG = "MyApp";
    private RequestQueue requestQueue;
    private ImageLoader mImageLoader;
    private static MyApp mInstance;
    private WeakReference<Activity> currentActivityRef;
    private AppOpenAdManager appOpenAdManager;
    public static synchronized MyApp getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        FirebaseApp.initializeApp(this);
        registerActivityLifecycleCallbacks(this);

        MobileAds.initialize(this, initializationStatus -> {});
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        appOpenAdManager = new AppOpenAdManager(this);

        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "FirebaseCrashlytics error", e);
        }
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        return requestQueue;
    }

    public ImageLoader getImageLoader() {
        getRequestQueue();
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(this.requestQueue,
                    new LruBitmapCache());
        }
        return this.mImageLoader;
    }

    /** Foreground event */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {
        Activity activity = currentActivityRef != null ? currentActivityRef.get() : null;

        if (activity != null) {
            appOpenAdManager.showOrLoad(activity);  // Optimized logic
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!appOpenAdManager.isShowingAd()) {
            currentActivityRef = new WeakReference<>(activity);
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivityRef != null && currentActivityRef.get() == activity) {
            currentActivityRef.clear();
        }
    }

    /**
     * Optimized App Open Ad Manager
     * — Loads only when needed
     * — Shows immediately on load
     * — Reloads after dismissal
     */
    private static class AppOpenAdManager {
        private final Application application;
        private AppOpenAd appOpenAd;
        private boolean isShowingAd = false;
        AppOpenAdManager(Application application) {
            this.application = application;
        }
        public boolean isShowingAd() {
            return isShowingAd;
        }

        /** Main optimized method: show if available else load */
        public void showOrLoad(Activity activity) {
            if (isShowingAd) return;
            if (appOpenAd != null) {
                showAdIfAvailable(activity);
                return;
            }
            loadAdAndThenShow(activity);
        }

        /** Load and immediately show once loaded */
        private void loadAdAndThenShow(Activity activity) {
            AdRequest request = new AdRequest.Builder().build();
            AppOpenAd.load(
                    application.getApplicationContext(),
                    OPEN_AD_ID,
                    request,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            Log.d(TAG, "AppOpenAd Loaded");
                            appOpenAd = ad;
                            showAdIfAvailable(activity);
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            Log.e(TAG, "AppOpenAd Load Failed: " + error.getMessage());
                        }
                    }
            );
        }

        /** Show Ad */
        private void showAdIfAvailable(Activity activity) {
            if (isShowingAd || appOpenAd == null) return;
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAd = true;
                    Log.d(TAG, "AppOpenAd Shown");
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AppOpenAd Dismissed");
                    isShowingAd = false;
                    appOpenAd = null;
                    loadAdInBackground();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "Failed to show AppOpenAd: " + adError.getMessage());
                    isShowingAd = false;
                    appOpenAd = null;
                }
            });

            appOpenAd.show(activity);
        }

        /** Load silently for next foreground event */
        private void loadAdInBackground() {
            AdRequest request = new AdRequest.Builder().build();
            AppOpenAd.load(
                    application.getApplicationContext(),
                    OPEN_AD_ID,
                    request,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            Log.d(TAG, "AppOpenAd Preloaded in Background");
                            appOpenAd = ad;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            Log.e(TAG, "Background Load Failed: " + error.getMessage());
                        }
                    }
            );
        }
    }

    public <T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }
}
