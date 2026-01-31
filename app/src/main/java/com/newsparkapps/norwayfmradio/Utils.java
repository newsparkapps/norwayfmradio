package com.newsparkapps.norwayfmradio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Utils {
    private static final String TAG = "Utils";
    private static FirebaseAnalytics mFirebaseAnalytics;

    public static void setFMAnalytics(String message, Context context) {

        String specialchar = "_";
        String converted = message.replace(" ", specialchar);
        int converted_length = converted.length();
        if (converted_length > 15) {
            converted = converted.substring(0, 15);
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("BollywoodFM_", message);
        mFirebaseAnalytics.logEvent("BollywoodFM_" + converted, params);
    }

    public static void setFMButtonAnalytics(String message, Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("BollywoodFM_viewall_", message);
        mFirebaseAnalytics.logEvent("BollywoodFM_viewall_" + message, params);
    }

    public static void setErrorAnalytics(String message, Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("BollywoodFM_error_", message);
        mFirebaseAnalytics.logEvent("BollywoodFM_error_" + message, params);
    }



    public static void setErrorLog(String message,String tag) {
        Log.i(tag,message);
    }

    public static void setFMLog(String message,String tag) {
        Log.i(tag,message);
    }

    public int convertPixelsToDp(int px, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.densityDpi / 160f));
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    public static void setScreenAnalytics(String message, Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("BollywoodFM_screen_", message);
        mFirebaseAnalytics.logEvent("BollywoodFM_screen_" + message, params);
    }

    public static void setPlayerAnalytics(String message, Context context) {

        String specialchar = "_";
        String converted = message.replace(" ", specialchar);
        int converted_length = converted.length();
        if (converted_length > 15) {
            converted = converted.substring(0, 15);
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("BollywoodFM_P", message);
        mFirebaseAnalytics.logEvent("BollywoodFM_P" + converted, params);
    }

    public static void startActivity(Activity sourceActivity, Class<?> destinationActivity) {
        Log.i("Utils", "startActivity " + " " + sourceActivity + " " + destinationActivity);
        try {
            Intent intent = new Intent(sourceActivity, destinationActivity);
            sourceActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.d("Utils", "Utils startActivity ActivityNotFoundException"+e);

        }
    }

    public static void setFavoritesAnalytics(String message, Context context) {

        String specialchar = "_";
        String converted = message.replace(" ", specialchar);
        int converted_length = converted.length();
        if (converted_length > 15) {
            converted = converted.substring(0, 15);
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("Bollywood_Fav_", message);
        mFirebaseAnalytics.logEvent("Bollywood_Fav_" + converted, params);
    }


    public static void enableEdgeToEdge(Activity activity, View rootView) {
        try {
            Window window = activity.getWindow();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
            } else {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                );
            }
            window.setStatusBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController insetsController = window.getInsetsController();
                if (insetsController != null) {
                    insetsController.setSystemBarsAppearance(
                            0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            } else {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                );
            }

            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, systemBars.bottom);
                return insets;
            });
        } catch (Exception e) {
            Log.e(TAG, "enableEdgeToEdge Exception "+e);
        }
    }
}
