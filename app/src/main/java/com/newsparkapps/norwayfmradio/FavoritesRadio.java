package com.newsparkapps.norwayfmradio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import java.util.ArrayList;
import java.util.List;

public class FavoritesRadio extends Fragment {
    View v;
    private static RecyclerView radiorecyclerView;
    FirebaseAnalytics mFirebaseAnalytics;
    LinearLayout subPlayer;
    ImageButton trigger;
    TextView favoritesall;
    CustomAdapter adapterMusic;
    private List<Shoutcast> shoutcasts;
    DatabaseHandler db;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.recyclerhorizontal, container, false);
        v.setBackgroundColor(getResources().getColor(R.color.bgcolor));

        shoutcasts = new ArrayList<Shoutcast>();

        subPlayer = v.findViewById(R.id.sub_player);
        trigger = v.findViewById(R.id.playTrigger);
        radiorecyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
        radiorecyclerView.setHasFixedSize(true);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());


        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
        Bundle bundle = new Bundle();
        bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, 1);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "OnlineFMRadio_Myfavorites");
        mFirebaseAnalytics.logEvent("OnlineFMRadio_Myfavorites", bundle);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        radiorecyclerView.setLayoutManager(linearLayoutManager);


        db = new DatabaseHandler(getContext());
        ArrayList<String> mymessageslist = new ArrayList<>();
        // Reading all contacts
        Log.d("Reading: ", "Reading all Message..");

        List<Shoutcast> myFourites = db.getAllFourites();
        for (Shoutcast cn : myFourites)
        {
            Shoutcast items = new Shoutcast(cn.getName(),cn.getUrl(),cn.getImage());
            shoutcasts.add(items);
        }

        adapterMusic = new CustomAdapter(getActivity(),shoutcasts,"Detailed");
        radiorecyclerView.setAdapter(adapterMusic);
        radiorecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
        return v;
    }
}
