/*
package com.newsparkapps.norwayfmradio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.newsparkapps.norwayfmradio.util.ShoutcastHelper;
import com.synnapps.carouselview.CarouselView;
import com.synnapps.carouselview.ImageListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class Dash extends AppCompatActivity {

    private final String TAG = Dash.class.getSimpleName();
    private static RecyclerView radiorecyclerView;
    private RecyclerView.LayoutManager radiorecyclerViewManager;
    CustomAdapter adapterMusic;
    CarouselView carouselView;
    LinearLayout subPlayer;
    RadioManager radioManager;
    ImageButton trigger;
    LinearLayout favoriteslayout;
    DatabaseHandler db;
    private FrameLayout adContainerView;
    AdView adView;
    TextView subplayername,favoritesall;
    InterstitialAd interstitialAd;
    NetworkImageView subplayerimage;
    int[] sampleImages = {R.drawable.aa};
    ImageLoader imageLoader = MyApplication.getInstance().getImageLoader();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed);

        FmConstants.MyClass = "Detailed";

        MobileAds.initialize(this,"ca-app-pub-4257458430524860~3643850862");

        carouselView = (CarouselView) findViewById(R.id.carouselView);
        carouselView.setPageCount(sampleImages.length);
        Intent a =getIntent();
        String language = a.getStringExtra("language");

        */
/*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(language.toUpperCase() + " FM RADIO");*//*


        interstitialAd = new InterstitialAd(Dash.this);
        interstitialAd.setAdUnitId(getResources().getString(R.string.interstitialadid));
        AdRequest adRequests1 = new AdRequest.Builder().build();
        interstitialAd.loadAd(adRequests1);
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                Intent a = new Intent(getApplicationContext(), Exit.class);
                startActivity(a); }
            @Override
            public void onAdLeftApplication() {
                Intent a = new Intent(getApplicationContext(), Exit.class);
                startActivity(a);
            }
            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }
        });

        radioManager = RadioManager.with(getApplicationContext());

        if (imageLoader == null)
            imageLoader = MyApplication.getInstance().getImageLoader();

        adContainerView = findViewById(R.id.ad_view_container);
        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.banneradid));
        adContainerView.addView(adView);
        loadBanner();


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




        db = new DatabaseHandler(getApplicationContext());
        int count = db.getMessagesCount();
        if (count == 0) {
            favoriteslayout.setVisibility(View.GONE);
        } else {
            favoriteslayout.setVisibility(View.VISIBLE);
        }


        favoritesall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent a = new Intent(getApplicationContext(), Dash.class);
                a.putExtra("language","favorites");
                startActivity(a);
                finish();
            }
        });

        subPlayer = (LinearLayout) findViewById(R.id.sub_player);
        trigger = (ImageButton) findViewById(R.id.playTrigger);

        radiorecyclerViewManager = new GridLayoutManager(getApplicationContext(), 3);
        radiorecyclerView.setLayoutManager(radiorecyclerViewManager);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());
        radiorecyclerView.addItemDecoration(new GridSpacingItemDecoration(3, 5, true));


        adapterMusic = new CustomAdapter(Dash.this,
                ShoutcastHelper.retrieveShoutcasts(Dash.this, "bollywood"),"Detailed");
        radiorecyclerView.setAdapter(adapterMusic);

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
                if(TextUtils.isEmpty(FmConstants.fmurl)) return;
                radioManager.playOrPause(FmConstants.fmname,FmConstants.fmimage,FmConstants.fmurl);
            }
        });


        new loadRadioFragments().execute();
        setDatavalues();
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
                new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build();
        AdSize adSize = getAdSize();
        adView.setAdSize(adSize);
        adView.loadAd(adRequest);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
           if (interstitialAd.isLoaded()) {
               interstitialAd.show();
           } else {
               Intent g = new Intent(getApplicationContext(), Exit.class);
               startActivity(g);
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
}
*/
