package com.newsparkapps.norwayfmradio;



import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {
    Context mContext;
    RadioManager radioManager;
    String presentactivity;
    private List<Shoutcast> shoutcasts;
    ImageLoader imageLoader = MyApplication.getInstance().getImageLoader();

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView site;
        NetworkImageView imageViewIcon;

        public MyViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.site = (TextView) itemView.findViewById(R.id.site);
            this.imageViewIcon = (NetworkImageView) itemView.findViewById(R.id.imageView);
        }
    }

    public CustomAdapter(Context context, List<Shoutcast> shoutcasts, String activity) {
        this.shoutcasts = shoutcasts;
        this.mContext = context;
        this.presentactivity = activity;
        radioManager = RadioManager.with(context);

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent,
                                           int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cards_layout, parent, false);
        MyViewHolder myViewHolder = new MyViewHolder(view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int listPosition) {

        final TextView name = holder.name;
        TextView site = holder.site;
        NetworkImageView imageView = holder.imageViewIcon;
        Typeface custom_font = Typeface.createFromAsset(mContext.getAssets(), "fonts/Muli-Regular.ttf");

        if (imageLoader == null)
            imageLoader = MyApplication.getInstance().getImageLoader();

        name.setText(shoutcasts.get(listPosition).getName());
        site.setText(shoutcasts.get(listPosition).getUrl());
        name.setTypeface(custom_font);

        imageView.setDefaultImageResId(R.drawable.norwayradio_small);

        if (shoutcasts.get(listPosition).getImage().equals("")) {
            //imageView.setImageResource(R.drawable.ticketlogo_not);
        } else {
            imageView.setImageUrl(shoutcasts.get(listPosition).getImage(), imageLoader);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                radioManager.playOrPause(shoutcasts.get(listPosition).getName(), shoutcasts.get(listPosition).getImage(), shoutcasts.get(listPosition).getUrl());
                try {
                    ((Detailed) mContext).setDatavalues();
                    Toast.makeText(mContext, "Loading..", Toast.LENGTH_SHORT).show();
                } catch (NullPointerException | ClassCastException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return shoutcasts.size();
    }


}