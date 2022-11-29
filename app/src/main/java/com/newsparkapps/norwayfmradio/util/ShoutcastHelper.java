package com.newsparkapps.norwayfmradio.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.newsparkapps.norwayfmradio.R;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class ShoutcastHelper {

    public static List<Shoutcast> retrieveShoutcasts(Context context, String language) {
        Reader reader;

        if (language != null) {

            if (language.equals("norway")) {
                reader = new InputStreamReader(context.getResources().openRawResource(R.raw.norway));
            } else {
                reader = new InputStreamReader(context.getResources().openRawResource(R.raw.norway));
            }
        } else {
            reader = new InputStreamReader(context.getResources().openRawResource(R.raw.norway));
        }
        return (new Gson()).fromJson(reader, new TypeToken<List<Shoutcast>>() {
        }.getType());
    }

}
