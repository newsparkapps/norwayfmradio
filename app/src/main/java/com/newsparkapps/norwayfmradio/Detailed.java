package com.newsparkapps.norwayfmradio;


import static com.newsparkapps.norwayfmradio.FmConstants.FM_JSON_URL;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdView;
import com.newsparkapps.norwayfmradio.util.Shoutcast;
import com.newsparkapps.norwayfmradio.util.ShoutcastHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Detailed extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 121;
    static RecyclerView radiorecyclerView;
    RecyclerView.LayoutManager radiorecyclerViewManager;
    CustomAdapter adapterMusic; 
    private ArrayList<Shoutcast> fmlist;
    LinearLayout subPlayer;
    RadioManager radioManager;
    LinearLayout splash;
    ImageButton trigger;
    LinearLayout favoriteslayout;
    DatabaseHandler db;
    TextView subplayername,favoritesall;
    ShimmerFrameLayout shimmerFrameLayout;
    NetworkImageView subplayerimage;
    public static String TAG = "Detailed";
    ImageLoader imageLoader = MyApplication.getInstance().getImageLoader();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed);

        FmConstants.MyClass = "Detailed";
        splash = findViewById(R.id.splash);
        splash.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT > 32) {
            if (!shouldShowRequestPermissionRationale("112")) {
                getNotificationPermission();
            }
        }
        shimmerFrameLayout = findViewById(R.id.shimmerFrameLayout);
        shimmerFrameLayout.startShimmerAnimation();

        FrameLayout adContainer = findViewById(R.id.ad_view_container);
        AdView adView = AdmobUtils.createAdView(this);
        adContainer.addView(adView);
        AdmobUtils.loadBannerAd(this, adView);

        radioManager = RadioManager.with(getApplicationContext());

        if (imageLoader == null)
            imageLoader = MyApplication.getInstance().getImageLoader();
        
        favoriteslayout = findViewById(R.id.favoriteslayout);
        favoritesall = findViewById(R.id.favoritesall);
        subplayername =  findViewById(R.id.subplayername);
        subplayerimage =  findViewById(R.id.subplayerimage);
        subPlayer =  findViewById(R.id.sub_player);
        radiorecyclerView =  findViewById(R.id.recycler_view);
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

        favoritesall.setOnClickListener(view -> {
            Intent a1 = new Intent(getApplicationContext(), Detailed.class);
            a1.putExtra("language","favorites");
            startActivity(a1);
            finish();
        });

        subPlayer =  findViewById(R.id.sub_player);
        trigger =  findViewById(R.id.playTrigger);

        Utils deviceUtil = new Utils();
        int itemWidth = 120;
        if (deviceUtil.isTablet(this)) {
            Log.d("Device Type", "This is a tablet");
            itemWidth = 180;
        } else {
            Log.d("Device Type", "This is a mobile");
        }
        int numberOfColumns = calculateNoOfColumns(itemWidth);

        radiorecyclerViewManager = new GridLayoutManager(getApplicationContext(), numberOfColumns);

        radiorecyclerView.setLayoutManager(radiorecyclerViewManager);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());
        radiorecyclerView.addItemDecoration(new GridSpacingItemDecoration(3, 5, true));

        fmlist = new ArrayList<>();
        getFm();

        subPlayer.setOnClickListener(view -> {
            Intent a12 = new Intent(getApplicationContext(),MyPlayers.class);
            startActivity(a12);
        });

        trigger.setOnClickListener(view -> {
            if(TextUtils.isEmpty(FmConstants.fmurl)) return;
            radioManager.playOrPause(FmConstants.fmname,FmConstants.fmimage,FmConstants.fmurl);
        });
        new loadRadioFragments().execute();
        setDatavalues();
    }


    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    public void setDatavalues() {
        if (FmConstants.isPlaying) {
            trigger.setImageResource(R.drawable.ic_pause);
        } else {
            trigger.setImageResource(R.drawable.ic_play);
        }
        if (FmConstants.fmimage.isEmpty()) {
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
                ? R.drawable.ic_pause
                : R.drawable.ic_play);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent g = new Intent(getApplicationContext(), Exit.class);
            startActivity(g);
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
                Log.i(TAG,"Exception "+e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }

    }

    private void getFm() {
        StringRequest request = new StringRequest(Request.Method.GET, FM_JSON_URL, response -> {
            try {
                Log.i(TAG,"getFm "+response);
                JSONArray array=new JSONArray(response);
                for(int i=1;i<array.length();i++) {
                    JSONObject jsonObject=array.getJSONObject(i);
                    Shoutcast shoutcast = new Shoutcast();
                    try {
                        shoutcast.setName(jsonObject.getString("name"));
                        shoutcast.setImage(jsonObject.getString("image"));
                        shoutcast.setUrl(jsonObject.getString("stream"));
                        fmlist.add(shoutcast);
                    } catch (JSONException e) {
                        Log.i(TAG,"Exception "+e);
                    }
                }
                adapterMusic = new CustomAdapter(Detailed.this,fmlist,"Detailed");
                radiorecyclerView.setAdapter(adapterMusic);
                radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
                splash.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmerAnimation();
            } catch (NullPointerException | JSONException e) {
                Log.i(TAG,"Exception "+e);
            }
        }, error -> {
            try {
                Log.i(TAG,"Backup");
                adapterMusic = new CustomAdapter(getApplicationContext(),
                        ShoutcastHelper.retrieveShoutcasts(Detailed.this, "english"),"Detailed");
                radiorecyclerView.setAdapter(adapterMusic);
                radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
                splash.setVisibility(View.GONE);
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmerAnimation();
            } catch (NullPointerException e) {
                Log.i(TAG,"Exception "+e);
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(request);
    }

    @Override
    public void onDestroy() {
        radioManager.unbind();
        super.onDestroy();
    }

    public void getNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT > 32) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        } catch (Exception e) {
            Log.i(TAG,"Exception "+e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // allow
            } else {
                //deny
            }
        }

    }

    private int calculateNoOfColumns(int itemWidthDp) {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return Math.max(2, (int) (screenWidthDp / itemWidthDp));
    }

}
