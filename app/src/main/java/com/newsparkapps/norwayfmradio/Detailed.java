package com.newsparkapps.norwayfmradio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.newsparkapps.norwayfmradio.util.Shoutcast;
import com.newsparkapps.norwayfmradio.util.ShoutcastHelper;
import com.synnapps.carouselview.CarouselView;
import com.synnapps.carouselview.ImageListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Detailed extends AppCompatActivity {

    private final String TAG = Detailed.class.getSimpleName();
    private static RecyclerView radiorecyclerView;
    private RecyclerView.LayoutManager radiorecyclerViewManager;
    CustomAdapter adapterMusic;
    CarouselView carouselView;
    LinearLayout subPlayer;
    RadioManager radioManager;
    public String FM_JSON_URL;
    ImageButton trigger;
    LinearLayout favoriteslayout;
    DatabaseHandler db;
    LinearLayout splash;
    boolean paused = false;
    private FrameLayout adContainerView;
    AdView adView;
    TextView subplayername,favoritesall;
    ProgressBar progressBar;
    InterstitialAd mInterstitialAd,exitmInterstitialAd;
    private ArrayList<Shoutcast> fmlist;
    NetworkImageView subplayerimage;
    FirebaseRemoteConfig firebaseRemoteConfig;
    int[] sampleImages = {R.drawable.aa};
    ImageLoader imageLoader = MyApplication.getInstance().getImageLoader();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed);
        setContentView(R.layout.detailed);

        FmConstants.MyClass = "Detailed";
        fmlist = new ArrayList<>();

        getFMLive();


        splash = findViewById(R.id.splash);
        splash.setVisibility(View.VISIBLE);


        carouselView = (CarouselView) findViewById(R.id.carouselView);
        progressBar = findViewById(R.id.progressBar);
        carouselView.setPageCount(sampleImages.length);
        Intent a =getIntent();
        String language = a.getStringExtra("language");

        radioManager = RadioManager.with(getApplicationContext());

        if (imageLoader == null)
            imageLoader = MyApplication.getInstance().getImageLoader();

        adContainerView = findViewById(R.id.ad_view_container);
        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.banneradid));
        adContainerView.addView(adView);
        loadBanner();

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        firebaseRemoteConfig.setConfigSettingsAsync(
                new FirebaseRemoteConfigSettings.Builder().build());

        firebaseRemoteConfig.fetch(10).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Once the config is successfully fetched it must be activated before newly fetched values are returned.
                    firebaseRemoteConfig.activate();
                    checkStatus();
                } else {
                }
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this,getResources().getString(R.string.interstitialadid), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when fullscreen content is dismissed.
                        Log.d("TAG", "The ad was dismissed.");
                        Intent a = new Intent(getApplicationContext(), Exit.class);
                        startActivity(a);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when fullscreen content failed to show.
                        Log.d("TAG", "The ad failed to show.");
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                        // Make sure to set your reference to null so you don't
                        // show it a second time.
                        mInterstitialAd = null;
                        Log.d("TAG", "The ad was shown.");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                mInterstitialAd = null;
            }
        });



        ImageListener imageListener = new ImageListener() {
            @Override
            public void setImageForPosition(int position, ImageView imageView) {
                imageView.setImageResource(sampleImages[position]);
            }
        };
        carouselView.setImageListener(imageListener);
        favoriteslayout =(LinearLayout) findViewById(R.id.favoriteslayout);
        favoritesall =(TextView) findViewById(R.id.favoritesall);
        subplayername = (TextView) findViewById(R.id.subplayername);
        subplayerimage = (NetworkImageView) findViewById(R.id.subplayerimage);
        subPlayer = (LinearLayout) findViewById(R.id.sub_player);
        radiorecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        radiorecyclerView.setHasFixedSize(true);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());
        radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);


        favoritesall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent a = new Intent(getApplicationContext(), Detailed.class);
                a.putExtra("language","favorites");
                startActivity(a);
            }
        });

        subPlayer = (LinearLayout) findViewById(R.id.sub_player);
        trigger = (ImageButton) findViewById(R.id.playTrigger);

        radiorecyclerViewManager = new GridLayoutManager(getApplicationContext(), 3);
        radiorecyclerView.setLayoutManager(radiorecyclerViewManager);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());
        radiorecyclerView.addItemDecoration(new GridSpacingItemDecoration(3, 5, true));


        /*adapterMusic = new CustomAdapter(Detailed.this,
                ShoutcastHelper.retrieveShoutcasts(Detailed.this, "bollywood"),"Detailed");
        radiorecyclerView.setAdapter(adapterMusic);*/

        subPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent a = new Intent(getApplicationContext(),MyPlayers.class);
                startActivity(a);
            }
        });

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if(TextUtils.isEmpty(FmConstants.fmurl)) return;
                radioManager.playOrPause(FmConstants.fmname,FmConstants.fmimage,FmConstants.fmurl);*/
                if (FmConstants.isPlaying) {
                    radioManager.pausePlaying();
                    FmConstants.isPlaying =false;
                    setDatavalues();
                } else {
                    radioManager.continuePlaying();
                    FmConstants.isPlaying =true;
                    setDatavalues();
                }
            }
        });
        setDatavalues();
    }

    private void checkStatus() {
        String showAds = firebaseRemoteConfig.getString("norwayShowAds");
        if (showAds.equals("true")) {
            loadExitInterstitials();
        }
    }

    private void loadExitInterstitials() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this,getResources().getString(R.string.interstitialadid), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                exitmInterstitialAd = interstitialAd;
                exitmInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when fullscreen content is dismissed.
                        paused = false;
                        loadExitInterstitials();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when fullscreen content failed to show.
                        Log.d("TAG", "The ad failed to show.");
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                        // Make sure to set your reference to null so you don't
                        // show it a second time.
                        exitmInterstitialAd = null;
                        Log.d("TAG", "The ad was shown.");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                mInterstitialAd = null;
            }
        });
    }


    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    public void setDatavalues() {

        if (FmConstants.isPlaying) {
            trigger.setImageResource(R.drawable.ic_pause_black);
        } else {
            trigger.setImageResource(R.drawable.ic_play_arrow_black);
        }
        if (FmConstants.fmimage.equals("")) {
            subplayerimage.setDefaultImageResId(R.drawable.norwayradio_small);
        } else {
            subplayerimage.setImageUrl(FmConstants.fmimage, imageLoader);
        }
        subplayername.setText(FmConstants.fmname);
        subPlayer.setVisibility(View.VISIBLE);
    }



    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


    @Override
    public void onResume() {
        super.onResume();
        radioManager.bind();
        db = new DatabaseHandler(getApplicationContext());
        int count = db.getMessagesCount();
        if (count == 0) {
            favoriteslayout.setVisibility(View.GONE);
        } else {
            new loadRadioFragments().execute();
            favoriteslayout.setVisibility(View.VISIBLE);
        }
        /*if (paused) {
            if(exitmInterstitialAd != null) {
                exitmInterstitialAd.show(Detailed.this);
            }
        }*/
    }

    @Subscribe
    public void onEvent(String status){
        switch (status){
            case PlaybackStatus.LOADING:
                // loading
                break;

            case PlaybackStatus.ERROR:
                Toast.makeText(getApplicationContext(), "Station not available", Toast.LENGTH_SHORT).show();
                break;
        }

        trigger.setImageResource(status.equals(PlaybackStatus.PLAYING)
                ? R.drawable.ic_pause_black
                : R.drawable.ic_play_arrow_black);
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    private void loadBanner() {
        AdRequest adRequest =
                new AdRequest.Builder().build();
        AdSize adSize = getAdSize();
        adView.setAdSize(adSize);
        adView.loadAd(adRequest);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(Detailed.this);
            } else {
                Intent a = new Intent(getApplicationContext(), Exit.class);
                startActivity(a);
            }
        }
        return false;
    }

    public class loadRadioFragments extends AsyncTask<String, Void, String> {

        @SuppressLint("WrongThread")
        @Override
        protected String doInBackground(String... params) {
            try {
                FavoritesRadio favoritesRadio=new FavoritesRadio();
                FragmentManager manager=getSupportFragmentManager();
                FragmentTransaction transaction=manager.beginTransaction();
                transaction.add(R.id.favorites, favoritesRadio, "favoritesRadio");
                transaction.commit();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }

    }



    private void getFMLive() {
        FM_JSON_URL = "https://ronstech.co.in/fmradio/norway.json";
        //FM_JSON_URL = "https://script.googleusercontent.com/macros/echo?user_content_key=zgS2MbTNp7suAMaB80NMLRMwhCm6sd4FihYHBcPQaDTGEx1OI5SM1pcLzWtUkMB532CLNJeIMLCo_GPS8ynBVm463UAt1NdTOJmA1Yb3SEsKFZqtv3DaNYcMrmhZHmUMWojr9NvTBuBLhyHCd5hHa1GhPSVukpSQTydEwAEXFXgt_wltjJcH3XHUaaPC1fv5o9XyvOto09QuWI89K6KjOu0SP2F-BdwUMgPXEdWGFtWtiKZAu0OpzarN348uWFs4mhyOA0g3-2l8xNz2_L-YbtVK2l-QLqb-UmXYeZtFRJfXBoJA_D-PCQ&lib=MnrE7b2I2PjfH799VodkCPiQjIVyBAxva";
        StringRequest request = new StringRequest(Request.Method.GET, FM_JSON_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray array=new JSONArray(response);
                    for(int i=0;i<array.length();i++) {
                        JSONObject jsonObject=array.getJSONObject(i);
                        Shoutcast shoutcast = new Shoutcast();
                        try {
                            shoutcast.setName(jsonObject.getString("name"));
                            shoutcast.setImage(jsonObject.getString("image"));
                            shoutcast.setUrl(jsonObject.getString("stream"));
                            // Finally, add the object to your arraylist
                            fmlist.add(shoutcast);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapterMusic = new CustomAdapter(Detailed.this,fmlist,"Detailed");
                    radiorecyclerView.setAdapter(adapterMusic);
                    radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
                    progressBar.setVisibility(View.GONE);
                    splash.setVisibility(View.GONE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    adapterMusic = new CustomAdapter(getApplicationContext(),
                            ShoutcastHelper.retrieveShoutcasts(Detailed.this, "norway"),"Detailed");
                    radiorecyclerView.setAdapter(adapterMusic);
                    radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
                    progressBar.setVisibility(View.GONE);
                    splash.setVisibility(View.GONE);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request);
    }


    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onDestroy() {
        radioManager.unbind();
        super.onDestroy();
    }
}
