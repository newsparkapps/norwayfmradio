package com.newsparkapps.norwayfmradio;

import static com.newsparkapps.norwayfmradio.FmConstants.OPEN_AD_CODE;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.newsparkapps.norwayfmradio.ads.AdmobUtils;

import java.lang.ref.WeakReference;
import java.util.Date;

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private static final String TAG = "MyApp";
    private static MyApp instance;
    private AppOpenAdManager appOpenAdManager;
    private RequestQueue requestQueue;
    private ImageLoader mImageLoader;

    boolean openAdLOaded = false;
    private WeakReference<Activity> currentActivityRef;  // WeakReference prevents memory leaks

    public static MyApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        FirebaseApp.initializeApp(this);
        registerActivityLifecycleCallbacks(this);


        new Handler(Looper.getMainLooper()).post(() -> {
            MobileAds.initialize(this, initializationStatus -> {
                Log.d("AdMob", "AdMob SDK initialization complete.");
            });
        });

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
        getRequestQueue().getCache().clear();
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(this.requestQueue,
                    new LruBitmapCache());
        }
        return this.mImageLoader;
    }

    /**
     * Called when the app moves to foreground.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {
        Activity activity = currentActivityRef != null ? currentActivityRef.get() : null;
        if (activity != null) {
            appOpenAdManager.showAdIfAvailable(activity);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!appOpenAdManager.isShowingAd()) {
            currentActivityRef = new WeakReference<>(activity); // Use WeakReference to avoid leaks
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
            currentActivityRef.clear();  // Clear reference when activity is destroyed
        }
    }

    /**
     * App Open Ad Manager (Handles loading and displaying of App Open Ads)
     */
    private static class AppOpenAdManager {
        private final Application application;
        private AppOpenAd appOpenAd;
        private long loadTime = 0;
        private boolean isShowingAd = false;
        private boolean openAdLoadFailed = false;

        public AppOpenAdManager(Application application) {
            this.application = application;
            loadAd(application.getApplicationContext());
        }

        /**
         * Loads an App Open Ad if one is not already available.
         */
        public void loadAd(Context context) {
            if (isAdAvailable()) {
                return;
            }
            String adUbit ="";

            if(!openAdLoadFailed) {
                adUbit =  AdmobUtils.getOpenAdUnitId(AdmobUtils.getUserCountry(context));
            } else {
                adUbit = OPEN_AD_CODE;
            }


            Log.d(TAG, "adUbit  "+adUbit);

            AdRequest adRequest = new AdRequest.Builder().build();
            AppOpenAd.load(context, adUbit, adRequest,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
                            appOpenAd = ad;
                            openAdLoadFailed = false;
                            loadTime = new Date().getTime();
                            Log.d(TAG, "App Open Ad Loaded openAdLOadFailed "+openAdLoadFailed);
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError error) {
                            Log.e(TAG, "Failed to load App Open Ad: " + error.getMessage());
                            openAdLoadFailed = true;
                            loadAd(context);
                        }
                    });
        }

        /**
         * Returns true if an ad is available and valid.
         */
        private boolean isAdAvailable() {
            return appOpenAd != null && (new Date().getTime() - loadTime < 3600000); // Valid for 1 hour
        }

        /**
         * Returns true if an ad is currently being displayed.
         */
        public boolean isShowingAd() {
            return isShowingAd;
        }

        /**
         * Shows the ad if it's available and not already showing.
         */
        public void showAdIfAvailable(Activity activity) {
            if (isShowingAd || !isAdAvailable()) {
                return;
            }

            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    isShowingAd = false;
                    appOpenAd = null;
                    loadAd(application.getApplicationContext());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    isShowingAd = false;
                    Log.e(TAG, "App Open Ad failed to show: " + adError.getMessage());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAd = true;
                    Log.d(TAG, "App Open Ad Displayed");
                }
            });

            appOpenAd.show(activity);
        }
    }

    public <T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }
}

