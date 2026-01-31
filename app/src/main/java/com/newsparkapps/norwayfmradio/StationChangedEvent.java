package com.newsparkapps.norwayfmradio;

public class StationChangedEvent {

    public final Station station;
    public final boolean isPlaying;

    public StationChangedEvent(Station station, boolean isPlaying) {
        this.station = station;
        this.isPlaying = isPlaying;
    }
}
