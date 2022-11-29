package com.newsparkapps.norwayfmradio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.newsparkapps.norwayfmradio.util.ShoutcastHelper;


public class MalayalamRadio extends Fragment {
    View v;
    private static RecyclerView radiorecyclerView;

    CustomAdapter adapterMusic;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.recyclerhorizontal, container, false);
        v.setBackgroundColor(getResources().getColor(R.color.bgcolor));

        radiorecyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
        radiorecyclerView.setHasFixedSize(true);
        radiorecyclerView.setItemAnimator(new DefaultItemAnimator());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        radiorecyclerView.setLayoutManager(linearLayoutManager);

        adapterMusic = new CustomAdapter(getActivity(),
                ShoutcastHelper.retrieveShoutcasts(getContext(), "malayalam"),"Detailed");
        radiorecyclerView.setAdapter(adapterMusic);


        return v;
    }
}
