package com.newsparkapps.norwayfmradio.util;

import com.google.gson.annotations.SerializedName;

public class Shoutcast {

    @SerializedName("name")
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @SerializedName("id")
    private int id;

    @SerializedName("image")
    private String image;

    @SerializedName("stream")
    private String url;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Shoutcast(String name, String url, String img) {
        this.name = name;
        this.url = url;
        this.image = img;
    }


    public Shoutcast() {

    }

    public Shoutcast(int id, String name, String url, String img) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.image = img;
    }
}

