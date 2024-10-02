package com.newsparkapps.norwayfmradio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Utils {
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
        params.putString("NorwayFM_", message);
        mFirebaseAnalytics.logEvent("NorwayFM_" + converted, params);
    }

    public static void setFMButtonAnalytics(String message, Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("NorwayFM_viewall_", message);
        mFirebaseAnalytics.logEvent("NorwayFM_viewall_" + message, params);
    }

    public static void setErrorAnalytics(String message, Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("NorwayFM_error_", message);
        mFirebaseAnalytics.logEvent("NorwayFM_error_" + message, params);
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

    public boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    public static void setScreenAnalytics(String message, Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        // Log a custom event
        Bundle params = new Bundle();
        params.putString("NorwayFM_screen_", message);
        mFirebaseAnalytics.logEvent("NorwayFM_screen_" + message, params);
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
        params.putString("NorwayFM_P", message);
        mFirebaseAnalytics.logEvent("NorwayFM_P" + converted, params);
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
}
