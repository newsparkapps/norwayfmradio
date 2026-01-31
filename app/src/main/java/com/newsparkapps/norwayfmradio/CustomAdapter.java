package com.newsparkapps.norwayfmradio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

    private final Context context;
    private final List<Shoutcast> shoutcasts;
    private final RadioManager radioManager;

    private final ImageLoader imageLoader;

    /* -------------------- CONSTRUCTOR -------------------- */

    public CustomAdapter(@NonNull Context context,
                         @NonNull List<Shoutcast> shoutcasts,
                         @NonNull String from) {

        this.context = context;
        this.shoutcasts = shoutcasts;
        this.radioManager = RadioManager.with(context);

        this.imageLoader = MyApp.getInstance().getImageLoader();
    }

    /* -------------------- VIEW HOLDER -------------------- */

    static class MyViewHolder extends RecyclerView.ViewHolder {

        final TextView name;
        final NetworkImageView icon;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            icon = itemView.findViewById(R.id.imageView);
        }
    }

    /* -------------------- ADAPTER METHODS -------------------- */

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cards_layout, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Shoutcast item = shoutcasts.get(position);
        holder.name.setText(item.getName());
        // 🔥 RESET recycled image (VERY IMPORTANT)
        holder.icon.setImageDrawable(null);

        // 🔥 ALWAYS set default & error images
        holder.icon.setDefaultImageResId(R.drawable.norway_fm_radio_logo);
        holder.icon.setErrorImageResId(R.drawable.norway_fm_radio_logo);

        if (item.getImage() != null && !item.getImage().isEmpty()) {
            holder.icon.setImageUrl(item.getImage(), imageLoader);

        } else {
            holder.icon.setDefaultImageResId(R.drawable.norway_fm_radio_logo);
        }

        holder.icon.setOnClickListener(v -> onRadioClicked(item));
    }

    @Override
    public int getItemCount() {
        return shoutcasts != null ? shoutcasts.size() : 0;
    }

    /* -------------------- CLICK HANDLER -------------------- */
    public void onRadioClicked(@NonNull Shoutcast shoutcast) {
        radioManager.play(
                shoutcast.getName(),
                shoutcast.getImage(),
                shoutcast.getUrl()
        );
        radioManager.toggle();
        Toast.makeText(context, "Loading…", Toast.LENGTH_SHORT).show();
    }
}
