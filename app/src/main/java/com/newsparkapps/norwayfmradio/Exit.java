package com.newsparkapps.norwayfmradio;

import static com.newsparkapps.norwayfmradio.FmConstants.APP_URL;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdView;

/**
 * Created by Roney on 9/16/2017.
 */

public class Exit extends Activity {
    Button exit, rateus;
    public static String TAG = "Exit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exit);

        exit = findViewById(R.id.exit);
        rateus = findViewById(R.id.rateusbutton);

        FrameLayout adSquareContainer1 = findViewById(R.id.ad_view_container);
        AdView adView2 = AdmobUtils.createSmallSquareAdView(this);
        adSquareContainer1.addView(adView2);
        AdmobUtils.loadBannerAd(this, adView2);

        exit.setOnClickListener(view -> {
            try {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_HOME);
                startActivity(i);
                finish();
            } catch (SecurityException e) {
                Log.i(TAG,"Exception "+e);
            }
        });

        rateus.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(APP_URL));
            startActivity(intent);
        });
    }

}
