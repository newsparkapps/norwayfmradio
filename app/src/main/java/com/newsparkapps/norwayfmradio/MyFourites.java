package com.newsparkapps.norwayfmradio;

public class MyFourites {

    int id;

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

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    String name;
    String url;
    String img;

    public MyFourites()
    {

    }

    public MyFourites(int id, String name, String url, String img)
    {
        this.id=id;
        this.name=name;
        this.url=url;
        this.img=img;
    }

    public MyFourites(String name, String url, String img)
    {
        this.name=name;
        this.url=url;
        this.img=img;
    }

}
