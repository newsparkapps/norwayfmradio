package com.newsparkapps.norwayfmradio.models;

public class MyFourites {
    int id;
    String name;
    String url;
    String img;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getImg() {
        return img;
    }
    public MyFourites(String name, String url, String img)
    {
        this.name=name;
        this.url=url;
        this.img=img;
    }
}
